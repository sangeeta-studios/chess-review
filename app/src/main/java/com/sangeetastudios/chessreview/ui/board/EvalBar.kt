package com.sangeetastudios.chessreview.ui.board

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sangeetastudios.chessreview.analysis.Classifier
import com.sangeetastudios.chessreview.analysis.PositionEval

/** Vertical evaluation bar; White's share grows from the bottom (top when [flipped]). */
@Composable
fun EvalBar(eval: PositionEval?, flipped: Boolean, modifier: Modifier = Modifier) {
    val target = eval?.let { (Classifier.winPercent(it.whiteCp) / 100).toFloat() } ?: 0.5f
    val white by animateFloatAsState(target, tween(450), label = "eval")
    val whiteBetter = (eval?.whiteCp ?: 0) >= 0
    val label = eval?.display?.removePrefix("+")?.removePrefix("-") ?: ""
    Box(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF403D39))
            .semantics { contentDescription = "Evaluation ${eval?.display ?: "pending"}" },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(white)
                .align(if (flipped) Alignment.TopCenter else Alignment.BottomCenter)
                .background(Color(0xFFF2F2F2)),
        )
        // Label sits on the winning side's end of the bar.
        val atBottom = whiteBetter != flipped
        Text(
            label,
            color = if (whiteBetter) Color(0xFF403D39) else Color(0xFFF2F2F2),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier
                .align(if (atBottom) Alignment.BottomCenter else Alignment.TopCenter)
                .padding(vertical = 3.dp),
        )
    }
}
