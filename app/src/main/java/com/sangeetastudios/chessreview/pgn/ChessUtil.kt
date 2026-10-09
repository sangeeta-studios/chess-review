package com.sangeetastudios.chessreview.pgn

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.move.Move

/**
 * Board helpers. SAN is encoded/decoded here against [Board.legalMoves] rather than with
 * chesslib's MoveList, whose SAN helpers share a cached board between calls.
 */
object ChessUtil {
    private val sanRegex = Regex("""^([NBRQK])?([a-h])?([1-8])?x?([a-h][1-8])(?:=?([NBRQ]))?$""")

    /** Returns 64 piece chars ('K','q',…, or ' ') indexed by square, a1 = 0 … h8 = 63. */
    fun piecesFromFen(fen: String): CharArray {
        val squares = CharArray(64) { ' ' }
        val placement = fen.substringBefore(' ')
        var rank = 7
        var file = 0
        for (c in placement) {
            when {
                c == '/' -> { rank--; file = 0 }
                c.isDigit() -> file += c.digitToInt()
                else -> { if (rank in 0..7 && file in 0..7) squares[rank * 8 + file] = c; file++ }
            }
        }
        return squares
    }

    fun whiteToMove(fen: String): Boolean = fen.split(' ').getOrNull(1) != "b"

    /** Square index (a1 = 0) from algebraic name such as "e4". */
    fun squareIndex(name: String): Int = (name[1] - '1') * 8 + (name[0] - 'a')

    fun squareName(index: Int): String = "${'a' + index % 8}${'1' + index / 8}"

    fun boardFromFen(fen: String): Board = Board().apply { loadFromFen(fen) }

    /** Square of the king of the side to move if it is in check, else null. */
    fun checkedKingSquare(fen: String): Int? {
        if (!boardFromFen(fen).isKingAttacked) return null
        val king = if (whiteToMove(fen)) 'K' else 'k'
        return piecesFromFen(fen).indexOf(king).takeIf { it >= 0 }
    }

    fun toUci(move: Move): String {
        val promo = move.promotion.takeIf { it != Piece.NONE }
            ?.pieceType?.sanSymbol?.lowercase().orEmpty()
        return (move.from.value() + move.to.value()).lowercase() + promo
    }

    private fun pieceLetter(type: PieceType): String = when (type) {
        PieceType.KNIGHT -> "N"
        PieceType.BISHOP -> "B"
        PieceType.ROOK -> "R"
        PieceType.QUEEN -> "Q"
        PieceType.KING -> "K"
        else -> ""
    }

    /** Finds the legal move in [board] described by [san]; null if none/ambiguous. */
    fun decodeSan(board: Board, san: String): Move? {
        val clean = san.trim().trimEnd('+', '#', '!', '?').replace("0", "O")
        val legal = board.legalMoves()
        if (clean == "O-O" || clean == "O-O-O") {
            val toFile = if (clean == "O-O") "G" else "C"
            return legal.firstOrNull {
                board.getPiece(it.from).pieceType == PieceType.KING &&
                    it.from.value()[0] == 'E' && it.to.value()[0].toString() == toFile &&
                    it.from.value()[1] == it.to.value()[1]
            }
        }
        val m = sanRegex.matchEntire(clean) ?: return null
        val (pieceStr, fileStr, rankStr, dest, promoStr) = m.destructured
        val type = when (pieceStr) {
            "N" -> PieceType.KNIGHT; "B" -> PieceType.BISHOP; "R" -> PieceType.ROOK
            "Q" -> PieceType.QUEEN; "K" -> PieceType.KING; else -> PieceType.PAWN
        }
        val candidates = legal.filter { mv ->
            val from = mv.from.value().lowercase()
            board.getPiece(mv.from).pieceType == type &&
                mv.to.value().lowercase() == dest &&
                (fileStr.isEmpty() || from[0].toString() == fileStr) &&
                (rankStr.isEmpty() || from[1].toString() == rankStr) &&
                (promoStr.isEmpty() && mv.promotion == Piece.NONE ||
                    promoStr.isNotEmpty() && mv.promotion != Piece.NONE &&
                    pieceLetter(mv.promotion.pieceType) == promoStr)
        }
        return candidates.singleOrNull()
    }

    /** Encodes a legal [move] in [board] as SAN, including check/mate suffix. */
    fun encodeSan(board: Board, move: Move): String {
        val piece = board.getPiece(move.from)
        val type = piece.pieceType
        val from = move.from.value().lowercase()
        val to = move.to.value().lowercase()
        val sb = StringBuilder()
        if (type == PieceType.KING && kotlin.math.abs(from[0] - to[0]) == 2) {
            sb.append(if (to[0] == 'g') "O-O" else "O-O-O")
        } else {
            val isCapture = board.getPiece(move.to) != Piece.NONE ||
                (type == PieceType.PAWN && from[0] != to[0])
            if (type == PieceType.PAWN) {
                if (isCapture) sb.append(from[0]).append('x')
                sb.append(to)
                if (move.promotion != Piece.NONE) sb.append('=').append(pieceLetter(move.promotion.pieceType))
            } else {
                sb.append(pieceLetter(type))
                val rivals = board.legalMoves().filter {
                    it.to == move.to && it.from != move.from && board.getPiece(it.from) == piece
                }
                if (rivals.isNotEmpty()) {
                    val sameFile = rivals.any { it.from.value()[0].lowercaseChar() == from[0] }
                    val sameRank = rivals.any { it.from.value()[1] == from[1] }
                    when {
                        !sameFile -> sb.append(from[0])
                        !sameRank -> sb.append(from[1])
                        else -> sb.append(from)
                    }
                }
                if (isCapture) sb.append('x')
                sb.append(to)
            }
        }
        board.doMove(move)
        if (board.isMated) sb.append('#') else if (board.isKingAttacked) sb.append('+')
        board.undoMove()
        return sb.toString()
    }

    /** Converts a UCI move (e.g. "g1f3") in [fen] to SAN ("Nf3"); falls back to the UCI. */
    fun sanForUci(fen: String, uci: String): String = try {
        val board = boardFromFen(fen)
        board.legalMoves().firstOrNull { toUci(it) == uci }?.let { encodeSan(board, it) } ?: uci
    } catch (e: Exception) {
        uci
    }

    /** Converts an engine principal variation to SAN, stopping at the first unknown move. */
    fun pvToSan(fen: String, pv: List<String>, max: Int = 6): List<String> {
        val out = ArrayList<String>()
        try {
            val board = boardFromFen(fen)
            for (uci in pv.take(max)) {
                val move = board.legalMoves().firstOrNull { toUci(it) == uci } ?: break
                out += encodeSan(board, move)
                board.doMove(move)
            }
        } catch (_: Exception) {
        }
        return out
    }
}
