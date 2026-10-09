package com.sangeetastudios.chessreview.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.sangeetastudios.chessreview.R
import com.sangeetastudios.chessreview.analysis.MoveClass
import com.sangeetastudios.chessreview.ui.theme.BoardColors
import com.sangeetastudios.chessreview.ui.theme.color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class BoardArrow(val from: Int, val to: Int, val color: Color)

@Composable
fun rememberPiecePainters(): Map<Char, Painter> {
    val ids = mapOf(
        'K' to R.drawable.piece_wk, 'Q' to R.drawable.piece_wq, 'R' to R.drawable.piece_wr,
        'B' to R.drawable.piece_wb, 'N' to R.drawable.piece_wn, 'P' to R.drawable.piece_wp,
        'k' to R.drawable.piece_bk, 'q' to R.drawable.piece_bq, 'r' to R.drawable.piece_br,
        'b' to R.drawable.piece_bb, 'n' to R.drawable.piece_bn, 'p' to R.drawable.piece_bp,
    )
    return ids.mapValues { (_, id) -> rememberVectorPainter(ImageVector.vectorResource(id)) }
}

/**
 * Square-board renderer. Squares are indexed a1 = 0 … h8 = 63. Swiping horizontally calls
 * [onSwipe] with -1 (previous) or +1 (next).
 */
@Composable
fun ChessBoard(
    pieces: CharArray,
    flipped: Boolean,
    modifier: Modifier = Modifier,
    lastMove: Pair<Int, Int>? = null,
    checkSquare: Int? = null,
    badge: Pair<Int, MoveClass>? = null,
    arrows: List<BoardArrow> = emptyList(),
    description: String = "",
    onSwipe: (Int) -> Unit = {},
) {
    val painters = rememberPiecePainters()
    val measurer = rememberTextMeasurer()
    val swipe = remember { floatArrayOf(0f) }
    val currentOnSwipe by rememberUpdatedState(onSwipe)

    Canvas(
        modifier
            .aspectRatio(1f)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { swipe[0] = 0f },
                    onDragEnd = {
                        val threshold = size.width / 8f
                        if (swipe[0] > threshold) currentOnSwipe(-1) else if (swipe[0] < -threshold) currentOnSwipe(1)
                    },
                ) { _, dx -> swipe[0] += dx }
            },
    ) {
        val sq = size.width / 8f

        fun topLeft(index: Int): Offset {
            val file = index % 8
            val rank = index / 8
            val col = if (flipped) 7 - file else file
            val row = if (flipped) rank else 7 - rank
            return Offset(col * sq, row * sq)
        }

        fun center(index: Int) = topLeft(index) + Offset(sq / 2, sq / 2)

        // Squares + last-move highlight.
        for (i in 0 until 64) {
            val light = (i % 8 + i / 8) % 2 == 1
            val highlighted = lastMove != null && (i == lastMove.first || i == lastMove.second)
            val color = when {
                highlighted && light -> BoardColors.lastMoveLight
                highlighted -> BoardColors.lastMoveDark
                light -> BoardColors.light
                else -> BoardColors.dark
            }
            drawRect(color, topLeft(i), Size(sq, sq))
        }

        // Classification tint on the destination square.
        badge?.let { (square, cls) -> drawRect(cls.color().copy(alpha = 0.45f), topLeft(square), Size(sq, sq)) }

        checkSquare?.let { s ->
            drawCircle(BoardColors.check, radius = sq * 0.5f, center = center(s))
        }

        // Coordinates on the edge squares.
        val coordStyle = TextStyle(fontSize = (sq * 0.2f).toSp(), fontWeight = FontWeight.Bold)
        for (k in 0 until 8) {
            val fileIdx = if (flipped) 7 - k else k
            val fileSquare = if (flipped) 56 + fileIdx else fileIdx
            val rankIdx = if (flipped) k else 7 - k
            val rankSquare = rankIdx * 8 + (if (flipped) 7 else 0)
            val fileLight = (fileSquare % 8 + fileSquare / 8) % 2 == 1
            val rankLight = (rankSquare % 8 + rankSquare / 8) % 2 == 1
            val fileText = measurer.measure("${'a' + fileIdx}", coordStyle.copy(color = if (fileLight) BoardColors.dark else BoardColors.light))
            drawText(fileText, topLeft = Offset(k * sq + sq - fileText.size.width - sq * 0.05f, 8 * sq - fileText.size.height))
            val rankText = measurer.measure("${rankIdx + 1}", coordStyle.copy(color = if (rankLight) BoardColors.dark else BoardColors.light))
            drawText(rankText, topLeft = Offset(sq * 0.05f, k * sq))
        }

        // Pieces.
        for (i in 0 until 64) {
            val p = pieces[i]
            if (p == ' ') continue
            val painter = painters[p] ?: continue
            translate(topLeft(i).x, topLeft(i).y) {
                with(painter) { draw(Size(sq, sq)) }
            }
        }

        arrows.forEach { drawArrow(center(it.from), center(it.to), it.color, sq) }

        // Badge in the top-right corner of the destination square.
        badge?.let { (square, cls) ->
            val r = sq * 0.22f
            val c = topLeft(square) + Offset(sq - r * 0.7f, r * 0.7f)
            drawCircle(Color.White, r * 1.12f, c)
            drawCircle(cls.color(), r, c)
            val text = measurer.measure(
                cls.symbol,
                TextStyle(color = Color.White, fontSize = (r * if (cls.symbol.length > 1) 0.95f else 1.15f).toSp(), fontWeight = FontWeight.Black),
            )
            drawText(text, topLeft = c - Offset(text.size.width / 2f, text.size.height / 2f))
        }
    }
}

private fun DrawScope.drawArrow(from: Offset, to: Offset, color: Color, sq: Float) {
    val angle = atan2(to.y - from.y, to.x - from.x)
    val head = sq * 0.42f
    val width = sq * 0.17f
    val shaftEnd = Offset(to.x - cos(angle) * head * 0.8f, to.y - sin(angle) * head * 0.8f)
    drawLine(color, from, shaftEnd, strokeWidth = width, cap = StrokeCap.Butt)
    val path = Path().apply {
        moveTo(to.x, to.y)
        val back = Offset(to.x - cos(angle) * head, to.y - sin(angle) * head)
        val px = -sin(angle) * head * 0.55f
        val py = cos(angle) * head * 0.55f
        lineTo(back.x + px, back.y + py)
        lineTo(back.x - px, back.y - py)
        close()
    }
    drawPath(path, color)
}
