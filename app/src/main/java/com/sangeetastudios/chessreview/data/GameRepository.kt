package com.sangeetastudios.chessreview.data

import android.content.Context
import com.sangeetastudios.chessreview.analysis.PositionEval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class SavedGame(
    val id: String,
    val pgn: String,
    val createdAt: Long,
    val white: String,
    val black: String,
    val result: String,
    val date: String?,
    val depth: Int = 0,
    val evals: List<PositionEval> = emptyList(),
)

/** Stores imported games and their (possibly partial) engine evaluations as JSON files. */
class GameRepository(context: Context) {
    private val dir = File(context.filesDir, "games").apply { mkdirs() }

    fun idFor(pgn: String): String =
        MessageDigest.getInstance("SHA-1").digest(pgn.trim().toByteArray())
            .joinToString("") { "%02x".format(it) }.take(16)

    suspend fun save(game: SavedGame) = withContext(Dispatchers.IO) {
        val tmp = File(dir, "${game.id}.json.tmp")
        tmp.writeText(game.toJson().toString())
        tmp.renameTo(File(dir, "${game.id}.json"))
    }

    suspend fun load(id: String): SavedGame? = withContext(Dispatchers.IO) {
        File(dir, "$id.json").takeIf { it.exists() }?.let { runCatching { fromJson(JSONObject(it.readText())) }.getOrNull() }
    }

    suspend fun recent(): List<SavedGame> = withContext(Dispatchers.IO) {
        dir.listFiles { f -> f.name.endsWith(".json") }.orEmpty()
            .mapNotNull { runCatching { fromJson(JSONObject(it.readText()), withEvals = false) }.getOrNull() }
            .sortedByDescending { it.createdAt }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) { File(dir, "$id.json").delete() }

    private fun SavedGame.toJson() = JSONObject().apply {
        put("id", id); put("pgn", pgn); put("createdAt", createdAt)
        put("white", white); put("black", black); put("result", result); put("date", date)
        put("depth", depth)
        put("evals", JSONArray().apply {
            evals.forEach { e ->
                put(JSONObject().apply {
                    e.cp?.let { put("cp", it) }
                    e.mate?.let { put("mate", it) }
                    e.bestMove?.let { put("best", it) }
                    put("pv", e.pv.joinToString(" "))
                })
            }
        })
    }

    private fun fromJson(o: JSONObject, withEvals: Boolean = true): SavedGame {
        val evals = if (!withEvals) emptyList() else {
            val arr = o.optJSONArray("evals") ?: JSONArray()
            (0 until arr.length()).map { i ->
                val e = arr.getJSONObject(i)
                PositionEval(
                    cp = if (e.has("cp")) e.getInt("cp") else null,
                    mate = if (e.has("mate")) e.getInt("mate") else null,
                    bestMove = e.optString("best").takeIf { it.isNotEmpty() },
                    pv = e.optString("pv").split(' ').filter { it.isNotEmpty() },
                )
            }
        }
        return SavedGame(
            id = o.getString("id"),
            pgn = o.getString("pgn"),
            createdAt = o.getLong("createdAt"),
            white = o.optString("white"),
            black = o.optString("black"),
            result = o.optString("result"),
            date = o.optString("date").takeIf { it.isNotEmpty() && it != "null" },
            depth = o.optInt("depth"),
            evals = evals,
        )
    }
}
