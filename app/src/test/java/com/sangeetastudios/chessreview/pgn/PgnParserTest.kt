package com.sangeetastudios.chessreview.pgn

import com.github.bhlangonijr.chesslib.Board
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PgnParserTest {

    @Test
    fun parsesChessComCoachGame() {
        val game = PgnParser.parse(SAMPLE)
        assertEquals("Coach-Anna", game.white)
        assertEquals("prateeksingla", game.black)
        assertEquals("1-0", game.result)
        assertEquals("198", game.blackElo)
        assertEquals(67, game.plies.size)
        assertEquals("e2e4", game.plies[0].uci)
        assertEquals("O-O", game.plies[12].san)
        assertEquals("e1g1", game.plies[12].uci)
        assertEquals("Qb8#", game.plies.last().san)
        val board = Board().apply { loadFromFen(game.plies.last().fenAfter) }
        assertTrue(board.isMated)
    }

    @Test
    fun stripsClocksCommentsVariationsAndNags() {
        val pgn = """
            [Event "Live"]
            [Result "*"]

            1. e4 {[%clk 0:09:58.1]} 1... e5 {[%clk 0:09:57]} 2. Nf3 $1 (2. f4 exf4) Nc6!? ; line comment
            3. Bb5 a6 *
        """.trimIndent()
        val game = PgnParser.parse(pgn)
        assertEquals(listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6"), game.plies.map { it.san })
    }

    @Test
    fun handlesPromotionAndCustomFen() {
        val pgn = """
            [SetUp "1"]
            [FEN "8/P7/8/8/8/8/8/k6K w - - 0 1"]

            1. a8=Q+ Kb2 *
        """.trimIndent()
        val game = PgnParser.parse(pgn)
        assertEquals("a7a8q", game.plies[0].uci)
        assertEquals(2, game.plies.size)
    }

    @Test(expected = PgnParseException::class)
    fun rejectsIllegalMove() {
        PgnParser.parse("1. e4 e5 2. Ke3 *")
    }

    @Test
    fun detectsLinkOnlyShare() {
        assertTrue(PgnParser.isLinkOnly("https://www.chess.com/game/live/123456"))
    }

    @Test
    fun encodedSanRoundTripsSampleGame() {
        val game = PgnParser.parse(SAMPLE)
        for (ply in game.plies) {
            assertEquals(ply.san, ChessUtil.sanForUci(ply.fenBefore, ply.uci))
        }
    }

    @Test
    fun disambiguatesSan() {
        // Knights on b1 and f3 can both reach d2.
        val fen = "4k3/8/8/8/8/5N2/8/1N2K3 w - - 0 1"
        assertEquals("Nbd2", ChessUtil.sanForUci(fen, "b1d2"))
        assertEquals("Nfd2", ChessUtil.sanForUci(fen, "f3d2"))
    }

    @Test
    fun sanForUci() {
        assertEquals("Nf3", ChessUtil.sanForUci(PgnParser.START_FEN, "g1f3"))
    }

    companion object {
        val SAMPLE = """
            [Event "Play vs Coach"]
            [Site "Chess.com"]
            [Date "2026.10.02"]
            [Round "?"]
            [White "Coach-Anna"]
            [Black "prateeksingla"]
            [Result "1-0"]
            [TimeControl "-"]
            [WhiteElo "1600"]
            [BlackElo "198"]
            [Termination "Coach Anna won by checkmate"]
            [ECO "C68"]
            [EndDate "2026.10.02"]
            [Link "https://www.chess.com/game/coach/259131488"]

            1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Bxc6 bxc6 5. d4 exd4 6. Qxd4 d6 7. O-O c5 8.
            Qd5 Rb8 9. Ng5 Be6 10. Nxe6 fxe6 11. Qxe6+ Ne7 12. e5 dxe5 13. Qxe5 Kd7 14. Rd1+
            Kc8 15. Rxd8+ Kxd8 16. Qe2 Rb6 17. Qc4 Rc6 18. Be3 Nf5 19. Nc3 Ne7 20. Rd1+ Rd6
            21. Bxc5 Rd7 22. Rxd7+ Kxd7 23. Ne2 g5 24. Bd4 Rg8 25. Be5 c6 26. Qd3+ Nd5 27.
            c4 Bc5 28. cxd5 cxd5 29. Qxd5+ Kc8 30. Qxg8+ Kd7 31. Qd5+ Kc8 32. Qxc5+ Kb7 33.
            Qc7+ Ka8 34. Qb8# 1-0
        """.trimIndent()
    }
}
