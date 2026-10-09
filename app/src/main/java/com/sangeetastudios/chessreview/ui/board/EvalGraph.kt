package com.sangeetastudios.chessreview.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.sangeetastudios.chessreview.analysis.Classifier
import com.sangeetastudios.chessreview.analysis.MoveClass
import com.sangeetastudios.chessreview.analysis.MoveReview
import com.sangeetastudios.chessreview.analysis.PositionEval
import com.sangeetastudios.chessreview.ui.theme.color
import kotlin.math.roundToInt

/** Win-probability area chart over the game; tap or drag to jump to a position. */
@Composable
fun EvalGraph(
    evals: List<PositionEval>,
    totalPositions: Int,
    current: Int,
    moves: List<MoveReview>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val select by rememberUpdatedState(onSelect)
    val total by rememberUpdatedState(totalPositions)
    Canvas(
        modifier
            .semantics { contentDescription = "Evaluation graph" }
            .pointerInput(Unit) {
                fun pick(x: Float) {
                    if (total < 2) return
                    select((x / size.width * (total - 1)).roundToInt().coerceIn(0, total - 1))
                }
                detectTapGestures { pick(it.x) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    if (total >= 2) select((change.position.x / size.width * (total - 1)).roundToInt().coerceIn(0, total - 1))
                }
            },
    ) {
        drawRect(Color(0xFF403D39))
        if (totalPositions < 2) return@Canvas
        val step = size.width / (totalPositions - 1)
        fun y(e: PositionEval) = size.height * (1f - (Classifier.winPercent(e.whiteCp) / 100f).toFloat())

        if (evals.isNotEmpty()) {
            val path = Path().apply {
                moveTo(0f, size.height)
                evals.forEachIndexed { i, e -> lineTo(i * step, y(e)) }
                lineTo((evals.size - 1) * step, size.height)
                close()
            }
            drawPath(path, Color(0xFFF2F2F2))
        }
        drawLine(Color(0x66888888), Offset(0f, size.height / 2), Offset(size.width, size.height / 2))
        drawLine(Color(0xFF81B64C), Offset(current * step, 0f), Offset(current * step, size.height), strokeWidth = 2f)

        moves.forEach { m ->
            if (m.classification in setOf(MoveClass.INACCURACY, MoveClass.MISTAKE, MoveClass.BLUNDER)) {
                val e = evals.getOrNull(m.plyIndex + 1) ?: return@forEach
                drawCircle(m.classification.color(), radius = 4.5f * density, center = Offset((m.plyIndex + 1) * step, y(e)))
            }
        }
    }
}
