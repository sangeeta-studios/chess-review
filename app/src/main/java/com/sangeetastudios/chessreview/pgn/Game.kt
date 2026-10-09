package com.sangeetastudios.chessreview.pgn

/** A single half-move of a parsed game. FENs describe the position before/after it. */
data class Ply(
    val index: Int,
    val san: String,
    val uci: String,
    val fenBefore: String,
    val fenAfter: String,
) {
    val isWhite: Boolean get() = index % 2 == 0
    val moveNumber: Int get() = index / 2 + 1
    val from: String get() = uci.substring(0, 2)
    val to: String get() = uci.substring(2, 4)
}

data class Game(
    val headers: Map<String, String>,
    val startFen: String,
    val plies: List<Ply>,
) {
    val white: String get() = headers["White"] ?: "White"
    val black: String get() = headers["Black"] ?: "Black"
    val whiteElo: String? get() = headers["WhiteElo"]?.takeIf { it.isNotBlank() && it != "?" }
    val blackElo: String? get() = headers["BlackElo"]?.takeIf { it.isNotBlank() && it != "?" }
    val result: String get() = headers["Result"] ?: "*"
    val date: String? get() = headers["Date"]?.takeIf { !it.contains("?") }

    /** FEN of the position shown after [plyCount] half-moves (0 = start). */
    fun fenAt(plyCount: Int): String = if (plyCount == 0) startFen else plies[plyCount - 1].fenAfter
}
