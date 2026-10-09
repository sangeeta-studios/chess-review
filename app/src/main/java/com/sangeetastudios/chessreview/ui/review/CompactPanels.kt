package com.sangeetastudios.chessreview.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sangeetastudios.chessreview.analysis.MoveClass
import com.sangeetastudios.chessreview.ui.board.EvalGraph

/** Single-line, horizontally scrolling move list that keeps the current move in view. */
@Composable
fun MoveStrip(state: ReviewUiState, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val game = state.game ?: return
    val listState = rememberLazyListState()
    val byPly = state.review?.moves?.associateBy { it.plyIndex }.orEmpty()
    LaunchedEffect(state.ply) {
        val target = (state.ply - 1).coerceAtLeast(0)
        // Keep a couple of earlier moves visible to the left of the current one.
        listState.animateScrollToItem((target - 2).coerceAtLeast(0))
    }
    LazyRow(
        modifier.semantics { contentDescription = "Moves" },
        state = listState,
        contentPadding = PaddingValues(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(game.plies, key = { i, _ -> i }) { i, ply ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ply.isWhite) {
                    Text(
                        "${ply.moveNumber}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp, end = 2.dp),
                    )
                }
                val cls = byPly[i]?.classification
                val selected = state.ply == i + 1
                Row(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { onSelect(i + 1) }
                        .heightIn(min = 40.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (cls != null && cls in MARKED) {
                        ClassBadge(cls, 20)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        ply.san,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private val MARKED = setOf(MoveClass.BEST, MoveClass.INACCURACY, MoveClass.MISTAKE, MoveClass.BLUNDER)

@Composable
fun AnalysisProgress(state: ReviewUiState, modifier: Modifier = Modifier) {
    if (!state.analyzing) return
    Column(modifier) {
        Text(
            "Analyzing… ${(state.progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
    }
}

/** Eval graph + accuracy/counts; shown at the start of a review and in the sheet. */
@Composable
fun GameSummaryPanel(state: ReviewUiState, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        AnalysisProgress(state, Modifier.fillMaxWidth().padding(bottom = 8.dp))
        EvalGraph(
            evals = state.evals,
            totalPositions = state.totalPositions,
            current = state.ply,
            moves = state.review?.moves.orEmpty(),
            onSelect = onSelect,
            modifier = Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.height(8.dp))
        SummaryCard(state, Modifier.fillMaxWidth())
    }
}

/** Fixed bottom bar for phones: navigation plus a button for the full moves/summary sheet. */
@Composable
fun ReviewBottomBar(
    onFirst: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onLast: () -> Unit,
    onFlip: () -> Unit,
    onMore: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onFlip) { Icon(Icons.Filled.SwapVert, "Flip board") }
            IconButton(onClick = onFirst) { Icon(Icons.Filled.FirstPage, "First move") }
            IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous move") }
            IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next move") }
            IconButton(onClick = onLast) { Icon(Icons.Filled.LastPage, "Last move") }
            IconButton(onClick = onMore) { Icon(Icons.AutoMirrored.Filled.ListAlt, "Moves and summary") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovesSummarySheet(state: ReviewUiState, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxHeight(0.85f)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Moves") })
                Tab(tab == 1, { tab = 1 }, text = { Text("Summary") })
            }
            when (tab) {
                // Picking a move closes the sheet so the board is visible again.
                0 -> MoveList(state, { onSelect(it); onDismiss() }, Modifier.fillMaxWidth().weight(1f))
                else -> GameSummaryPanel(
                    state,
                    onSelect,
                    Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
                )
            }
        }
    }
}
