package com.sangeetastudios.chessreview.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.IOException

/** Engine output for one position. Scores are from the side to move's point of view. */
data class EngineResult(
    val bestMove: String?,
    val scoreCp: Int?,
    val mateIn: Int?,
    val pv: List<String>,
    val depth: Int,
)

/**
 * Minimal UCI client for the bundled Stockfish binary. The binary is packaged as
 * `libstockfish.so` so Android extracts it to nativeLibraryDir, where exec is permitted.
 */
class StockfishEngine(private val binary: File) : Closeable {
    private var process: Process? = null
    private lateinit var writer: BufferedWriter
    private lateinit var reader: BufferedReader
    private val mutex = Mutex()

    suspend fun start(threads: Int = defaultThreads(), hashMb: Int = 64) = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (process != null) return@withContext
            if (!binary.canExecute()) throw IOException("Engine binary not found: $binary")
            val p = ProcessBuilder(binary.absolutePath).redirectErrorStream(true).start()
            process = p
            writer = p.outputStream.bufferedWriter()
            reader = p.inputStream.bufferedReader()
            send("uci")
            readUntil { it == "uciok" }
            send("setoption name Threads value $threads")
            send("setoption name Hash value $hashMb")
            waitReady()
        }
    }

    suspend fun newGame() = mutex.withLock {
        withContext(Dispatchers.IO) {
            send("ucinewgame")
            waitReady()
        }
    }

    /** Searches [fen] to [depth] (bounded by [maxMillis]) and returns the final principal line. */
    suspend fun analyse(fen: String, depth: Int, maxMillis: Int = 4000): EngineResult = mutex.withLock {
        withContext(Dispatchers.IO) {
            send("position fen $fen")
            send("go depth $depth movetime $maxMillis")
            var cp: Int? = null
            var mate: Int? = null
            var pv: List<String> = emptyList()
            var reachedDepth = 0
            var stopped = false
            while (true) {
                val line = reader.readLine() ?: throw IOException("Engine exited unexpectedly")
                if (!stopped && !currentCoroutineContext().isActive) {
                    send("stop")
                    stopped = true
                }
                if (line.startsWith("bestmove")) {
                    val best = line.split(' ').getOrNull(1)?.takeIf { it != "(none)" }
                    return@withContext EngineResult(best, cp, mate, pv, reachedDepth)
                }
                if (line.startsWith("info") && " score " in line && " bound" !in line) {
                    val parts = line.split(' ')
                    val multipv = parts.valueAfter("multipv")?.toIntOrNull() ?: 1
                    if (multipv != 1) continue
                    reachedDepth = parts.valueAfter("depth")?.toIntOrNull() ?: reachedDepth
                    val scoreIdx = parts.indexOf("score")
                    when (parts.getOrNull(scoreIdx + 1)) {
                        "cp" -> { cp = parts[scoreIdx + 2].toInt(); mate = null }
                        "mate" -> { mate = parts[scoreIdx + 2].toInt(); cp = null }
                    }
                    val pvIdx = parts.indexOf("pv")
                    if (pvIdx >= 0) pv = parts.subList(pvIdx + 1, parts.size)
                }
            }
            @Suppress("UNREACHABLE_CODE")
            error("unreachable")
        }
    }

    private fun List<String>.valueAfter(key: String): String? =
        indexOf(key).takeIf { it >= 0 }?.let { getOrNull(it + 1) }

    private fun send(cmd: String) {
        writer.write(cmd)
        writer.newLine()
        writer.flush()
    }

    private fun waitReady() {
        send("isready")
        readUntil { it == "readyok" }
    }

    private fun readUntil(predicate: (String) -> Boolean) {
        while (true) {
            val line = reader.readLine() ?: throw IOException("Engine exited unexpectedly")
            if (predicate(line.trim())) return
        }
    }

    override fun close() {
        val p = process ?: return
        process = null
        runCatching { send("quit") }
        runCatching { if (!p.waitFor(500, java.util.concurrent.TimeUnit.MILLISECONDS)) p.destroy() }
    }

    companion object {
        fun binaryFile(context: Context) =
            File(context.applicationInfo.nativeLibraryDir, "libstockfish.so")

        fun defaultThreads() = (Runtime.getRuntime().availableProcessors() - 1).coerceIn(1, 4)
    }
}
