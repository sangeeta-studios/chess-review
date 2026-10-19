package com.sangeetastudios.chessreview.analysis

import kotlin.math.exp
import kotlin.math.ln

enum class MoveClass(val label: String, val symbol: String) {
    BEST("Best", "★"),
    EXCELLENT("Excellent", "!"),
    GOOD("Good", "✓"),
    INACCURACY("Inaccuracy", "?!"),
    MISTAKE("Mistake", "?"),
    BLUNDER("Blunder", "??"),
    FORCED("Forced", "□"),
}

/** Engine evaluation of a position, normalised to White's point of view. */
data class PositionEval(
    val cp: Int?,
    val mate: Int?,
    val bestMove: String?,
    val pv: List<String> = emptyList(),
) {
    /**
     * Centipawns from White's view; mates are mapped to ±(10000 - distance). A finished
     * checkmate is stored as cp = ±10000 with no mate distance.
     */
    val whiteCp: Int
        get() = when {
            mate != null && mate > 0 -> MATE_CP - mate
            mate != null -> -MATE_CP - mate // mate <= 0: White is getting mated
            else -> cp ?: 0
        }

    /** Short display string such as "+1.3", "-0.4", "M3", "-M2". */
    val display: String
        get() = when {
            mate == null && kotlin.math.abs(cp ?: 0) >= MATE_CP -> "#"
            mate != null -> if (mate > 0) "M$mate" else "-M${-mate}"
            else -> {
                val v = (cp ?: 0) / 100.0
                (if (v > 0) "+" else "") + String.format(java.util.Locale.US, "%.1f", v)
            }
        }

    companion object {
        const val MATE_CP = 10_000
    }
}

data class MoveReview(
    val plyIndex: Int,
    val classification: MoveClass,
    val winLoss: Double,
    val accuracy: Double,
    /** Centipawns the mover gave up (evals clamped to ±1000 so mates don't dominate). */
    val cpLoss: Int,
    /** Engine's preferred move (UCI) in the position before this ply. */
    val bestMove: String?,
    val bestMoveSan: String?,
)

object Classifier {
    private const val MATE_THRESHOLD = PositionEval.MATE_CP - 500
    private const val CP_CLAMP = 1000
    private const val MAX_ELO = 3100.0

    /** Lichess win-percentage model: 0..100 for White given a White-POV centipawn score. */
    fun winPercent(whiteCp: Int): Double {
        val cp = whiteCp.coerceIn(-1000, 1000)
        return 50 + 50 * (2 / (1 + exp(-0.00368208 * cp)) - 1)
    }

    /** Lichess per-move accuracy from the mover's win-percentage drop. */
    fun accuracy(winLoss: Double): Double =
        (103.1668 * exp(-0.04354 * winLoss) - 3.1669).coerceIn(0.0, 100.0)

    /** Mean of the volatility-weighted mean and the harmonic mean of per-move accuracies. */
    fun gameAccuracy(moves: List<Pair<Double, Double>>): Double? {
        if (moves.isEmpty()) return null
        val weighted = moves.sumOf { it.first * it.second } / moves.sumOf { it.second }
        val harmonic = moves.size / moves.sumOf { 1.0 / it.first.coerceAtLeast(1.0) }
        return (weighted + harmonic) / 2
    }

    /** Centipawn loss of a move, from White-POV evals clamped to ±[CP_CLAMP]. */
    fun cpLoss(before: PositionEval, after: PositionEval, moverIsWhite: Boolean): Int {
        val sign = if (moverIsWhite) 1 else -1
        val b = (sign * before.whiteCp).coerceIn(-CP_CLAMP, CP_CLAMP)
        val a = (sign * after.whiteCp).coerceIn(-CP_CLAMP, CP_CLAMP)
        return (b - a).coerceAtLeast(0)
    }

    /** Rough Elo for an average centipawn loss (same curve as Chesskit's game-rating estimate). */
    fun eloFromAcpl(acpl: Double): Double = MAX_ELO * exp(-0.01 * acpl)

    private fun acplFromElo(elo: Double): Double = -100 * ln(elo.coerceIn(1.0, MAX_ELO) / MAX_ELO)

    /**
     * Estimated performance rating for one side. With the player's [rating] known, the estimate
     * moves away from it by how much better or worse they played than that rating usually does;
     * otherwise it comes straight from the ACPL curve.
     */
    fun estimatedElo(acpl: Double, rating: Int?): Int {
        val elo = if (rating == null || rating <= 0) eloFromAcpl(acpl)
        else rating * exp(-0.005 * (acpl - acplFromElo(rating.toDouble())))
        return elo.coerceIn(100.0, MAX_ELO).roundToNearest(10)
    }

    private fun Double.roundToNearest(step: Int): Int = (Math.round(this / step) * step).toInt()

    fun classify(
        before: PositionEval,
        after: PositionEval,
        playedUci: String,
        moverIsWhite: Boolean,
        onlyMove: Boolean,
    ): Pair<MoveClass, Double> {
        val sign = if (moverIsWhite) 1 else -1
        val winBefore = winPercent(sign * before.whiteCp)
        val winAfter = winPercent(sign * after.whiteCp)
        val loss = (winBefore - winAfter).coerceAtLeast(0.0)

        if (onlyMove) return MoveClass.FORCED to loss
        if (playedUci == before.bestMove) return MoveClass.BEST to loss

        var cls = when {
            loss < 2 -> MoveClass.EXCELLENT
            loss < 5 -> MoveClass.GOOD
            loss < 10 -> MoveClass.INACCURACY
            loss < 20 -> MoveClass.MISTAKE
            else -> MoveClass.BLUNDER
        }
        // Throwing away a forced mate is never better than an inaccuracy (mistake if the
        // advantage left is no longer decisive).
        val hadMate = sign * before.whiteCp > MATE_THRESHOLD
        val stillMate = sign * after.whiteCp > MATE_THRESHOLD
        if (hadMate && !stillMate) {
            val floor = if (sign * after.whiteCp >= 500) MoveClass.INACCURACY else MoveClass.MISTAKE
            if (cls.ordinal < floor.ordinal) cls = floor
        }
        return cls to loss
    }
}
