package com.sangeetastudios.chessreview.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sangeetastudios.chessreview.analysis.MoveClass
import com.sangeetastudios.chessreview.pgn.ChessUtil
import com.sangeetastudios.chessreview.ui.board.BoardArrow
import com.sangeetastudios.chessreview.ui.board.ChessBoard
import com.sangeetastudios.chessreview.ui.board.EvalBar
import com.sangeetastudios.chessreview.ui.board.EvalGraph
import com.sangeetastudios.chessreview.ui.theme.BoardColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(gameId: String, viewModel: ReviewViewModel, onBack: () -> Unit) {
    LaunchedEffect(gameId) { viewModel.open(gameId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.game?.let { "${it.white} vs ${it.black}" } ?: "Game Review",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .focusRequester(focus)
                .focusable()
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (e.key) {
                        Key.DirectionRight, Key.DirectionDown -> { viewModel.step(1); true }
                        Key.DirectionLeft, Key.DirectionUp -> { viewModel.step(-1); true }
                        Key.MoveHome -> { viewModel.first(); true }
                        Key.MoveEnd -> { viewModel.last(); true }
                        Key.F -> { viewModel.flip(); true }
                        else -> false
                    }
                },
        ) {
            when {
                state.error != null -> ErrorState(state.error!!, onBack)
                state.game == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> AdaptiveReview(state, viewModel)
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun AdaptiveReview(state: ReviewUiState, vm: ReviewViewModel) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val twoPane = maxWidth > maxHeight || maxWidth >= 840.dp
        if (twoPane) {
            val boardColumnWidth = minOf(maxHeight - 72.dp + EVAL_BAR_TOTAL, maxWidth * 0.6f)
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
                BoardColumn(state, vm, Modifier.width(boardColumnWidth))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f).fillMaxSize()) {
                    FeedbackCard(state, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    NavigationControls(vm::first, { vm.step(-1) }, { vm.step(1) }, vm::last, vm::flip, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    InfoTabs(state, vm, Modifier.weight(1f))
                }
            }
        } else {
            // Phone layout: full-width board, one-line move strip, feedback filling the rest,
            // and a fixed bottom bar. The full move list and summary live in a bottom sheet.
            var showSheet by rememberSaveable { mutableStateOf(false) }
            val reserved = maxOf(260.dp, maxHeight * 0.3f)
            val boardColumnWidth = minOf(maxWidth, maxHeight - reserved + EVAL_BAR_TOTAL).coerceAtLeast(240.dp)
            Column(Modifier.fillMaxSize()) {
                BoardColumn(state, vm, Modifier.width(boardColumnWidth).align(Alignment.CenterHorizontally).padding(horizontal = 4.dp))
                MoveStrip(state, vm::goTo, Modifier.fillMaxWidth().height(44.dp))
                HorizontalDivider()
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    if (state.ply == 0) {
                        GameSummaryPanel(state, vm::goTo, Modifier.fillMaxWidth())
                    } else {
                        FeedbackCard(state, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        EvalGraph(
                            evals = state.evals,
                            totalPositions = state.totalPositions,
                            current = state.ply,
                            moves = state.review?.moves.orEmpty(),
                            onSelect = vm::goTo,
                            modifier = Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
                ReviewBottomBar(vm::first, { vm.step(-1) }, { vm.step(1) }, vm::last, vm::flip) { showSheet = true }
            }
            if (showSheet) MovesSummarySheet(state, vm::goTo) { showSheet = false }
        }
    }
}

private val EVAL_BAR_TOTAL: Dp = 22.dp

@Composable
private fun BoardColumn(state: ReviewUiState, vm: ReviewViewModel, modifier: Modifier) {
    val game = state.game ?: return
    val fen = game.fenAt(state.ply)
    val ply = game.plies.getOrNull(state.ply - 1)
    val review = ply?.let { p -> state.review?.moves?.firstOrNull { it.plyIndex == p.index } }
    val arrows = buildList {
        if (review != null && review.classification !in setOf(MoveClass.BEST, MoveClass.FORCED, MoveClass.EXCELLENT)) {
            review.bestMove?.let { add(BoardArrow(ChessUtil.squareIndex(it.substring(0, 2)), ChessUtil.squareIndex(it.substring(2, 4)), BoardColors.bestArrow)) }
        }
    }
    val topIsWhite = state.flipped
    val gameElo = { white: Boolean -> if (white) state.review?.whiteGameElo else state.review?.blackGameElo }
    Column(modifier) {
        PlayerStrip(if (topIsWhite) game.white else game.black, if (topIsWhite) game.whiteElo else game.blackElo, gameElo(topIsWhite), topIsWhite)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val boardSize = maxWidth - EVAL_BAR_TOTAL
            Row {
                EvalBar(state.evals.getOrNull(state.ply), state.flipped, Modifier.width(16.dp).height(boardSize))
                Spacer(Modifier.width(6.dp))
                ChessBoard(
                    pieces = ChessUtil.piecesFromFen(fen),
                    flipped = state.flipped,
                    modifier = Modifier.size(boardSize),
                    lastMove = ply?.let { ChessUtil.squareIndex(it.from) to ChessUtil.squareIndex(it.to) },
                    checkSquare = ChessUtil.checkedKingSquare(fen),
                    badge = if (ply != null && review != null) ChessUtil.squareIndex(ply.to) to review.classification else null,
                    arrows = arrows,
                    description = ply?.let { "Board after ${it.moveNumber}${if (it.isWhite) "." else "..."} ${it.san}" } ?: "Starting position",
                    onSwipe = { vm.step(it) },
                )
            }
        }
        PlayerStrip(if (topIsWhite) game.black else game.white, if (topIsWhite) game.blackElo else game.whiteElo, gameElo(!topIsWhite), !topIsWhite)
    }
}

@Composable
private fun PlayerStrip(name: String, elo: String?, gameElo: Int?, white: Boolean) {
    Row(Modifier.fillMaxWidth().padding(start = EVAL_BAR_TOTAL, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (white) Color(0xFFF2F2F2) else Color(0xFF262421)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        elo?.let {
            Spacer(Modifier.width(6.dp))
            Text("($it)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Estimated rating for this game, so it is visible without opening the summary.
        gameElo?.let {
            Spacer(Modifier.width(8.dp))
            // Icon + number keeps the name readable; the Summary tab spells out "Game rating".
            Row(
                Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .semantics(mergeDescendants = true) { contentDescription = "Game rating $it" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    "$it",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun InfoTabs(state: ReviewUiState, vm: ReviewViewModel, modifier: Modifier) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier) {
        PrimaryTabRow(selectedTabIndex = tab) {
            Tab(tab == 0, { tab = 0 }, text = { Text("Moves") })
            Tab(tab == 1, { tab = 1 }, text = { Text("Summary") })
        }
        when (tab) {
            0 -> MoveList(state, vm::goTo, Modifier.fillMaxSize())
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                EvalGraph(
                    evals = state.evals,
                    totalPositions = state.totalPositions,
                    current = state.ply,
                    moves = state.review?.moves.orEmpty(),
                    onSelect = vm::goTo,
                    modifier = Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.height(12.dp))
                SummaryCard(state, Modifier.fillMaxWidth())
            }
        }
    }
}
