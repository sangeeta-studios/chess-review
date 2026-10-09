package com.sangeetastudios.chessreview.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassifierTest {
    private fun eval(cp: Int, best: String? = "e2e4") = PositionEval(cp, null, best)

    @Test
    fun winPercentIsSymmetric() {
        assertEquals(50.0, Classifier.winPercent(0), 1e-9)
        assertEquals(100.0, Classifier.winPercent(300) + Classifier.winPercent(-300), 1e-9)
        assertTrue(Classifier.winPercent(10_000) > 97)
    }

    @Test
    fun accuracyBounds() {
        assertEquals(100.0, Classifier.accuracy(0.0), 0.01)
        assertTrue(Classifier.accuracy(30.0) < 30)
        assertEquals(0.0, Classifier.accuracy(100.0), 0.0)
    }

    @Test
    fun bestMoveMatchesEngine() {
        val (cls, _) = Classifier.classify(eval(30), eval(25), "e2e4", true, false)
        assertEquals(MoveClass.BEST, cls)
    }

    @Test
    fun thresholdsFromWhiteAndBlack() {
        assertEquals(MoveClass.EXCELLENT, Classifier.classify(eval(30), eval(20), "d2d4", true, false).first)
        assertEquals(MoveClass.BLUNDER, Classifier.classify(eval(0), eval(-400), "d2d4", true, false).first)
        // Black to move: White's eval going up is bad for Black.
        assertEquals(MoveClass.MISTAKE, Classifier.classify(eval(0), eval(150), "e7e5", false, false).first)
        assertEquals(MoveClass.INACCURACY, Classifier.classify(eval(0), eval(70), "e7e5", false, false).first)
    }

    @Test
    fun missingMateIsAtLeastInaccuracy() {
        val before = PositionEval(null, 2, "d1h5")
        val after = eval(900)
        assertEquals(MoveClass.INACCURACY, Classifier.classify(before, after, "a2a3", true, false).first)
    }

    @Test
    fun mateDisplay() {
        assertEquals("M3", PositionEval(null, 3, null).display)
        assertEquals("-M1", PositionEval(null, -1, null).display)
        assertEquals("+1.3", PositionEval(130, null, null).display)
        assertEquals("#", PositionEval(PositionEval.MATE_CP, null, null).display)
    }

    @Test
    fun oneBlunderPullsGameAccuracyDown() {
        val clean = Classifier.gameAccuracy(List(20) { 100.0 to 1.0 })!!
        val withBlunder = Classifier.gameAccuracy(List(19) { 100.0 to 1.0 } + (5.0 to 12.0))!!
        assertEquals(100.0, clean, 0.01)
        assertTrue("was $withBlunder", withBlunder < 80)
    }
}
