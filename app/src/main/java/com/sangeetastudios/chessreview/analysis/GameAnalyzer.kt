package com.sangeetastudios.chessreview.analysis

import com.sangeetastudios.chessreview.engine.EngineResult
import com.sangeetastudios.chessreview.engine.StockfishEngine
import com.sangeetastudios.chessreview.pgn.ChessUtil
import com.sangeetastudios.chessreview.pgn.Game
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Evaluations of every position of a game: index 0 = start, index i = after ply i. */
data class AnalysisProgress(val evals: List<PositionEval>, val total: Int) {
    val done: Boolean get() = evals.size == total
    val fraction: Float get() = if (total == 0) 1f else evals.size / total.toFloat()
}

class GameAnalyzer(private val engine: StockfishEngine) {

    /** Emits progressively longer eval lists; [resumeFrom] lets a cached partial run continue. */
    fun analyze(game: Game, depth: Int, resumeFrom: List<PositionEval> = emptyList()): Flow<AnalysisProgress> = flow {
        val total = game.plies.size + 1
        val evals = resumeFrom.take(total).toMutableList()
        emit(AnalysisProgress(evals.toList(), total))
        engine.start()
        engine.newGame()
        while (evals.size < total) {
            val fen = game.fenAt(evals.size)
            evals += evaluate(fen, depth)
            emit(AnalysisProgress(evals.toList(), total))
        }
    }

    private suspend fun evaluate(fen: String, depth: Int): PositionEval {
        val board = ChessUtil.boardFromFen(fen)
        val whiteToMove = ChessUtil.whiteToMove(fen)
        if (board.isMated) {
            return PositionEval(if (whiteToMove) -PositionEval.MATE_CP else PositionEval.MATE_CP, null, null)
        }
        if (board.isStaleMate || board.isInsufficientMaterial) return PositionEval(0, null, null)
        return engine.analyse(fen, depth).toWhitePov(whiteToMove)
    }

    private fun EngineResult.toWhitePov(whiteToMove: Boolean): PositionEval {
        val s = if (whiteToMove) 1 else -1
        return PositionEval(
            cp = scoreCp?.let { it * s },
            mate = mateIn?.takeIf { it != 0 }?.let { it * s },
            bestMove = bestMove,
            pv = pv,
        )
    }
}

/** Per-move feedback and per-side summary derived from a full set of evaluations. */
data class GameReview(
    val moves: List<MoveReview>,
    val whiteAccuracy: Double?,
    val blackAccuracy: Double?,
    val whiteCounts: Map<MoveClass, Int>,
    val blackCounts: Map<MoveClass, Int>,
) {
    companion object {
        fun build(game: Game, evals: List<PositionEval>): GameReview {
            val moves = game.plies.mapNotNull { ply ->
                val before = evals.getOrNull(ply.index) ?: return@mapNotNull null
                val after = evals.getOrNull(ply.index + 1) ?: return@mapNotNull null
                val onlyMove = ChessUtil.boardFromFen(ply.fenBefore).legalMoves().size == 1
                val (cls, loss) = Classifier.classify(before, after, ply.uci, ply.isWhite, onlyMove)
                val bestSan = before.bestMove?.let { ChessUtil.sanForUci(ply.fenBefore, it) }
                MoveReview(ply.index, cls, loss, Classifier.accuracy(loss), before.bestMove, bestSan)
            }
            fun side(white: Boolean) = moves.filter { (it.plyIndex % 2 == 0) == white }
            val weights = volatilityWeights(evals.map { Classifier.winPercent(it.whiteCp) })
            fun acc(list: List<MoveReview>) = Classifier.gameAccuracy(list.map { it.accuracy to (weights.getOrNull(it.plyIndex) ?: 1.0) })
            fun counts(list: List<MoveReview>) = list.groupingBy { it.classification }.eachCount()
            return GameReview(
                moves = moves,
                whiteAccuracy = acc(side(true)),
                blackAccuracy = acc(side(false)),
                whiteCounts = counts(side(true)),
                blackCounts = counts(side(false)),
            )
        }

        /**
         * Lichess-style weights: the standard deviation of win% in a sliding window around each
         * move, so moves in sharp positions count more than shuffling in a decided game.
         */
        private fun volatilityWeights(win: List<Double>): List<Double> {
            if (win.size < 2) return emptyList()
            val window = ((win.size - 1) / 10).coerceIn(2, 8)
            return (0 until win.size - 1).map { i ->
                val from = (i - window / 2).coerceIn(0, (win.size - window).coerceAtLeast(0))
                val slice = win.subList(from, (from + window).coerceAtMost(win.size))
                val mean = slice.average()
                kotlin.math.sqrt(slice.sumOf { (it - mean) * (it - mean) } / slice.size).coerceIn(0.5, 12.0)
            }
        }
    }
}
