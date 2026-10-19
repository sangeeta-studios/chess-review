package com.sangeetastudios.chessreview.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sangeetastudios.chessreview.analysis.MoveClass
import com.sangeetastudios.chessreview.analysis.MoveReview
import com.sangeetastudios.chessreview.pgn.ChessUtil
import com.sangeetastudios.chessreview.pgn.Game
import com.sangeetastudios.chessreview.ui.theme.color
import java.util.Locale

@Composable
fun ClassBadge(cls: MoveClass, size: Int = 22) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(cls.color()),
        contentAlignment = Alignment.Center,
    ) {
        // Sized in dp so the symbol always fits the circle regardless of the user's font scale.
        val fontSize = with(LocalDensity.current) { (size * if (cls.symbol.length > 1) 0.42f else 0.5f).dp.toSp() }
        Text(
            cls.symbol,
            color = Color.White,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(
                fontSize = fontSize,
                lineHeight = fontSize,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            ),
        )
    }
}

/** Explains the move that led to the current position. */
@Composable
fun FeedbackCard(state: ReviewUiState, modifier: Modifier = Modifier) {
    val game = state.game ?: return
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(12.dp)) {
            if (state.analyzing) {
                Text(
                    "Analyzing… ${(state.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
            if (state.ply == 0) {
                Text("Starting position", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Use ▶ or swipe the board to step through the game.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            val ply = game.plies[state.ply - 1]
            val review = state.review?.moves?.firstOrNull { it.plyIndex == ply.index }
            val eval = state.evals.getOrNull(state.ply)
            val moveLabel = "${ply.moveNumber}${if (ply.isWhite) "." else "..."} ${ply.san}"
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (review != null) {
                    ClassBadge(review.classification, 28)
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (review != null) "$moveLabel ${verdict(review.classification)}"
                        else moveLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = review?.classification?.color() ?: MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (review == null) {
                        Text("Waiting for engine…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (eval != null) {
                    Text(
                        eval.display,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (eval.whiteCp >= 0) Color(0xFFF2F2F2) else Color(0xFF403D39))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        color = if (eval.whiteCp >= 0) Color(0xFF262421) else Color(0xFFF2F2F2),
                    )
                }
            }
            if (review != null && review.classification != MoveClass.BEST && review.classification != MoveClass.FORCED &&
                review.bestMoveSan != null
            ) {
                Spacer(Modifier.height(6.dp))
                val before = state.evals.getOrNull(ply.index)
                val line = before?.let { ChessUtil.pvToSan(ply.fenBefore, it.pv) }.orEmpty()
                Text(
                    "Best was ${review.bestMoveSan}" + (before?.let { " (${it.display})" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MoveClass.BEST.color(),
                    fontWeight = FontWeight.SemiBold,
                )
                if (line.size > 1) {
                    Text(
                        "Line: " + formatLine(ply.index, line),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun verdict(cls: MoveClass) = when (cls) {
    MoveClass.BEST -> "is best"
    MoveClass.EXCELLENT -> "is excellent"
    MoveClass.GOOD -> "is good"
    MoveClass.INACCURACY -> "is an inaccuracy"
    MoveClass.MISTAKE -> "is a mistake"
    MoveClass.BLUNDER -> "is a blunder"
    MoveClass.FORCED -> "is forced"
}

private fun formatLine(startPly: Int, sans: List<String>): String = buildString {
    sans.forEachIndexed { i, san ->
        val p = startPly + i
        if (p % 2 == 0) append("${p / 2 + 1}. ") else if (i == 0) append("${p / 2 + 1}... ")
        append(san).append(' ')
    }
}.trim()

@Composable
fun NavigationControls(
    onFirst: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onLast: () -> Unit,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = onFlip) { Icon(Icons.Filled.SwapVert, "Flip board") }
        FilledTonalIconButton(onClick = onFirst) { Icon(Icons.Filled.FirstPage, "First move") }
        FilledTonalIconButton(onClick = onPrev, modifier = Modifier.size(56.dp, 48.dp)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous move", Modifier.size(32.dp))
        }
        FilledTonalIconButton(onClick = onNext, modifier = Modifier.size(56.dp, 48.dp)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next move", Modifier.size(32.dp))
        }
        FilledTonalIconButton(onClick = onLast) { Icon(Icons.Filled.LastPage, "Last move") }
    }
}

/** Two-column move list with classification markers. */
@Composable
fun MoveList(state: ReviewUiState, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val game = state.game ?: return
    val rows = (game.plies.size + 1) / 2
    val listState = rememberLazyListState()
    val byPly = state.review?.moves?.associateBy { it.plyIndex }.orEmpty()
    LaunchedEffect(state.ply) {
        if (state.ply > 0) {
            val row = (state.ply - 1) / 2
            val visible = listState.layoutInfo.visibleItemsInfo
            if (visible.none { it.index == row } || visible.lastOrNull()?.index == row) {
                val target = (row - 2).coerceAtLeast(0)
                // Animate small steps; jump straight there for long moves (e.g. "last move").
                if (kotlin.math.abs(listState.firstVisibleItemIndex - target) > 6) listState.scrollToItem(target)
                else listState.animateScrollToItem(target)
            }
        }
    }
    LazyColumn(modifier, state = listState) {
        itemsIndexed(List(rows) { it }) { row, _ ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (row % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${row + 1}.",
                    modifier = Modifier.width(36.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                for (side in 0..1) {
                    val idx = row * 2 + side
                    val ply = game.plies.getOrNull(idx)
                    Box(Modifier.weight(1f)) {
                        if (ply != null) MoveChip(ply.san, byPly[idx], state.ply == idx + 1) { onSelect(idx + 1) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveChip(san: String, review: MoveReview?, selected: Boolean, onClick: () -> Unit) {
    val cls = review?.classification
    val showMark = cls in setOf(MoveClass.INACCURACY, MoveClass.MISTAKE, MoveClass.BLUNDER, MoveClass.BEST)
    Row(
        Modifier
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .heightIn(min = 40.dp)
            .padding(horizontal = 8.dp)
            .semantics { contentDescription = san + (cls?.let { ", ${it.label}" } ?: "") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cls != null && showMark) {
            ClassBadge(cls, 18)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            san,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Players, accuracy, estimated game rating and per-class counts. */
@Composable
fun SummaryCard(state: ReviewUiState, modifier: Modifier = Modifier) {
    val game = state.game ?: return
    val review = state.review
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayerColumn(game.white, game.whiteElo, review?.whiteAccuracy, review?.whiteGameElo, true, Modifier.weight(1f))
                Text(
                    game.result.replace("1/2-1/2", "½-½"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                PlayerColumn(game.black, game.blackElo, review?.blackAccuracy, review?.blackGameElo, false, Modifier.weight(1f))
            }
            game.headers["Termination"]?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
            if (review != null) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                MoveClass.entries.forEach { cls ->
                    val w = review.whiteCounts[cls] ?: 0
                    val b = review.blackCounts[cls] ?: 0
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("$w", Modifier.weight(1f), textAlign = TextAlign.Center, color = cls.color(), fontWeight = FontWeight.Bold)
                        Row(Modifier.weight(2f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            ClassBadge(cls, 18)
                            Spacer(Modifier.width(6.dp))
                            Text(cls.label, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("$b", Modifier.weight(1f), textAlign = TextAlign.Center, color = cls.color(), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerColumn(name: String, elo: String?, accuracy: Double?, gameElo: Int?, white: Boolean, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (white) Color(0xFFF2F2F2) else Color(0xFF262421)),
        )
        Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        elo?.let { Text("($it)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text(
            accuracy?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text("Accuracy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(gameElo?.toString() ?: "—", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Game rating", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
