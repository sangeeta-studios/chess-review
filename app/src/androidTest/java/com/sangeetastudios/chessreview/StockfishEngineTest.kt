package com.sangeetastudios.chessreview

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sangeetastudios.chessreview.engine.StockfishEngine
import com.sangeetastudios.chessreview.pgn.PgnParser
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StockfishEngineTest {
    private val engine = StockfishEngine(
        StockfishEngine.binaryFile(InstrumentationRegistry.getInstrumentation().targetContext),
    )

    @After
    fun tearDown() = engine.close()

    @Test
    fun returnsBestMoveForStartPosition() = runBlocking {
        engine.start()
        val result = engine.analyse(PgnParser.START_FEN, depth = 10)
        assertNotNull(result.bestMove)
        assertNotNull(result.scoreCp)
    }

    @Test
    fun findsMateInOneFromSampleGame() = runBlocking {
        engine.start()
        // Position before 34. Qb8# in the sample Chess.com coach game.
        val fen = "k7/2Q4p/p7/4B1p1/8/8/PP2NPPP/6K1 w - - 3 34"
        val result = engine.analyse(fen, depth = 8)
        assertEquals("c7b8", result.bestMove)
        assertEquals(1, result.mateIn)
    }
}
