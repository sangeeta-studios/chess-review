package com.sangeetastudios.chessreview.pgn


class PgnParseException(message: String) : Exception(message)

object PgnParser {
    const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    private val headerRegex = Regex("""^\s*\[(\w+)\s+"((?:[^"\\]|\\.)*)"\s*]\s*$""")
    private val resultTokens = setOf("1-0", "0-1", "1/2-1/2", "½-½", "*")
    private val urlOnly = Regex("""^\s*https?://\S+\s*$""")

    /** Returns true if the text looks like a bare game link rather than a PGN. */
    fun isLinkOnly(text: String): Boolean = urlOnly.matches(text.trim()) ||
        (text.contains("chess.com/") && !text.contains("1."))

    /** Parses the first game found in [text]. */
    fun parse(text: String): Game {
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n').trim()
        if (normalized.isEmpty()) throw PgnParseException("The PGN is empty.")

        val headers = LinkedHashMap<String, String>()
        val moveText = StringBuilder()
        var inMoves = false
        for (line in normalized.lines()) {
            val m = if (!inMoves) headerRegex.matchEntire(line) else null
            if (m != null) {
                headers[m.groupValues[1]] = m.groupValues[2].replace("\\\"", "\"")
            } else if (line.isNotBlank()) {
                // A second header block means a second game; we only take the first.
                if (inMoves && headerRegex.matches(line)) break
                inMoves = true
                moveText.append(line).append('\n')
            }
        }

        val startFen = headers["FEN"]?.takeIf { it.isNotBlank() } ?: START_FEN
        val sans = tokenize(moveText.toString())
        if (sans.isEmpty()) throw PgnParseException("No moves found in the PGN.")

        val board = try {
            ChessUtil.boardFromFen(startFen)
        } catch (e: Exception) {
            throw PgnParseException("Invalid FEN start position.")
        }
        val plies = ArrayList<Ply>(sans.size)
        for ((i, rawSan) in sans.withIndex()) {
            val san = normalizeSan(rawSan)
            val before = board.fen
            val move = ChessUtil.decodeSan(board, san)
                ?: throw PgnParseException("Illegal or unreadable move ${moveLabel(i)}$san")
            board.doMove(move)
            plies += Ply(i, san, ChessUtil.toUci(move), before, board.fen)
        }
        return Game(headers, startFen, plies)
    }

    private fun moveLabel(i: Int) = "${i / 2 + 1}${if (i % 2 == 0) "." else "..."} "

    /** Strips comments, variations, NAGs, move numbers and results, returning SAN tokens. */
    internal fun tokenize(moveText: String): List<String> {
        val sb = StringBuilder()
        var braceDepth = 0
        var parenDepth = 0
        var lineComment = false
        for (ch in moveText) {
            when {
                lineComment -> if (ch == '\n') lineComment = false
                braceDepth > 0 -> if (ch == '}') braceDepth--
                ch == '{' -> braceDepth++
                ch == ';' && parenDepth == 0 -> lineComment = true
                ch == '(' -> parenDepth++
                ch == ')' -> if (parenDepth > 0) parenDepth--
                parenDepth > 0 -> Unit
                else -> sb.append(ch)
            }
        }
        return sb.toString()
            .replace(Regex("""\$\d+"""), " ")
            .replace(Regex("""\d+\.(\.\.)?"""), " ")
            .split(Regex("""\s+"""))
            .map { it.trim().trimEnd('!', '?') }
            .filter { it.isNotEmpty() && it !in resultTokens && it != "..." }
    }

    private fun normalizeSan(san: String) = san.replace("0-0-0", "O-O-O").replace("0-0", "O-O")
}
