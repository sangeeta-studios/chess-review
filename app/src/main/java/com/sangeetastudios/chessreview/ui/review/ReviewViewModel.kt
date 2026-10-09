package com.sangeetastudios.chessreview.ui.review

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.sangeetastudios.chessreview.analysis.GameAnalyzer
import com.sangeetastudios.chessreview.analysis.GameReview
import com.sangeetastudios.chessreview.analysis.PositionEval
import com.sangeetastudios.chessreview.data.GameRepository
import com.sangeetastudios.chessreview.data.SavedGame
import com.sangeetastudios.chessreview.data.Settings
import com.sangeetastudios.chessreview.engine.StockfishEngine
import com.sangeetastudios.chessreview.pgn.Game
import com.sangeetastudios.chessreview.pgn.PgnParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReviewUiState(
    val gameId: String? = null,
    val game: Game? = null,
    val evals: List<PositionEval> = emptyList(),
    val review: GameReview? = null,
    val ply: Int = 0,
    val flipped: Boolean = false,
    val analyzing: Boolean = false,
    val error: String? = null,
) {
    val totalPositions: Int get() = (game?.plies?.size ?: 0) + 1
    val progress: Float get() = if (totalPositions == 0) 0f else evals.size / totalPositions.toFloat()
}

class ReviewViewModel(app: Application, private val handle: SavedStateHandle) : AndroidViewModel(app) {
    private val repository = GameRepository(app)
    private val settings = Settings(app)
    private val engine = StockfishEngine(StockfishEngine.binaryFile(app))
    private val analyzer = GameAnalyzer(engine)
    private var job: Job? = null
    private var saved: SavedGame? = null

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    fun open(id: String) {
        if (_state.value.gameId == id && _state.value.error == null) return
        job?.cancel()
        val restoredPly = handle.get<Int>("ply").takeIf { handle.get<String>("gameId") == id }
        handle["gameId"] = id
        _state.value = ReviewUiState(gameId = id)
        job = viewModelScope.launch {
            val stored = repository.load(id)
            if (stored == null) {
                _state.update { it.copy(error = "This game is no longer available.") }
                return@launch
            }
            saved = stored
            val game = withContext(Dispatchers.Default) { runCatching { PgnParser.parse(stored.pgn) }.getOrNull() }
            if (game == null) {
                _state.update { it.copy(error = "This game could not be read.") }
                return@launch
            }
            val user = settings.username
            val flipped = user.isNotEmpty() && game.black.equals(user, ignoreCase = true) &&
                !game.white.equals(user, ignoreCase = true)
            val depth = settings.depth
            val resume = if (stored.depth == depth) stored.evals else emptyList()
            _state.update {
                it.copy(game = game, flipped = flipped, ply = restoredPly ?: 0, evals = resume)
            }
            if (resume.size < game.plies.size + 1) analyze(game, depth, resume) else publishReview(game, resume)
        }
    }

    private suspend fun analyze(game: Game, depth: Int, resume: List<PositionEval>) {
        _state.update { it.copy(analyzing = true) }
        var lastSaved = resume.size
        try {
            analyzer.analyze(game, depth, resume).collect { p ->
                publishReview(game, p.evals)
                if (p.done || p.evals.size - lastSaved >= 6) {
                    lastSaved = p.evals.size
                    persist(depth, p.evals)
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(error = "Engine error: ${e.message ?: "unknown"}") }
        } finally {
            _state.update { it.copy(analyzing = false) }
        }
    }

    private suspend fun publishReview(game: Game, evals: List<PositionEval>) {
        val review = withContext(Dispatchers.Default) { GameReview.build(game, evals) }
        _state.update { it.copy(evals = evals, review = review) }
    }

    private suspend fun persist(depth: Int, evals: List<PositionEval>) {
        val s = saved ?: return
        val updated = s.copy(depth = depth, evals = evals)
        saved = updated
        repository.save(updated)
    }

    fun goTo(ply: Int) {
        val max = _state.value.game?.plies?.size ?: 0
        val p = ply.coerceIn(0, max)
        handle["ply"] = p
        _state.update { it.copy(ply = p) }
    }

    fun step(delta: Int) = goTo(_state.value.ply + delta)
    fun first() = goTo(0)
    fun last() = goTo(Int.MAX_VALUE)
    fun flip() = _state.update { it.copy(flipped = !it.flipped) }

    /** Re-runs analysis, e.g. after the depth setting changed. */
    fun reanalyze() {
        val id = _state.value.gameId ?: return
        _state.value = ReviewUiState()
        open(id)
    }

    override fun onCleared() {
        job?.cancel()
        engine.close()
    }
}
