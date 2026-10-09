package com.sangeetastudios.chessreview.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.sangeetastudios.chessreview.data.GameRepository
import com.sangeetastudios.chessreview.data.SavedGame
import com.sangeetastudios.chessreview.data.Settings
import com.sangeetastudios.chessreview.pgn.PgnParseException
import com.sangeetastudios.chessreview.pgn.PgnParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Home : Screen
    data class Review(val gameId: String) : Screen
    data object About : Screen
    data object Licenses : Screen
    /** Full text of a bundled license file in assets/licenses. */
    data class LicenseText(val title: String, val asset: String) : Screen
}

class AppViewModel(app: Application, private val handle: SavedStateHandle) : AndroidViewModel(app) {
    val repository = GameRepository(app)
    val settings = Settings(app)

    private val _screen = MutableStateFlow(restoreScreen())
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _recent = MutableStateFlow<List<SavedGame>>(emptyList())
    val recent: StateFlow<List<SavedGame>> = _recent.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    val username = MutableStateFlow(settings.username)
    val depth = MutableStateFlow(settings.depth)

    init {
        refreshRecent()
    }

    fun refreshRecent() {
        viewModelScope.launch { _recent.value = repository.recent() }
    }

    fun navigate(screen: Screen) {
        _screen.value = screen
        handle["screen"] = when (screen) {
            Screen.Home -> "home"
            Screen.About -> "about"
            Screen.Licenses -> "licenses"
            is Screen.LicenseText -> "license:${screen.asset}|${screen.title}"
            is Screen.Review -> "review:${screen.gameId}"
        }
        if (screen == Screen.Home) refreshRecent()
    }

    /** Returns true if the back press was consumed. */
    fun back(): Boolean {
        navigate(
            when (_screen.value) {
                Screen.Home -> return false
                is Screen.LicenseText -> Screen.Licenses
                Screen.Licenses -> Screen.About
                else -> Screen.Home
            },
        )
        return true
    }

    fun importPgn(text: String) {
        if (text.isBlank()) {
            showMessage("Paste or share a PGN first.")
            return
        }
        if (PgnParser.isLinkOnly(text)) {
            showMessage("That's a game link, not a PGN. In Chess.com open the game, tap Share → PGN, then choose Chess Review.")
            return
        }
        viewModelScope.launch {
            _importing.value = true
            try {
                val pgn = text.trim()
                val game = withContext(Dispatchers.Default) { PgnParser.parse(pgn) }
                val id = repository.idFor(pgn)
                if (repository.load(id) == null) {
                    repository.save(
                        SavedGame(
                            id = id, pgn = pgn, createdAt = System.currentTimeMillis(),
                            white = game.white, black = game.black, result = game.result, date = game.date,
                        ),
                    )
                }
                navigate(Screen.Review(id))
            } catch (e: PgnParseException) {
                showMessage("Couldn't read this PGN: ${e.message}")
            } catch (e: Exception) {
                showMessage("Couldn't import this game.")
            } finally {
                _importing.value = false
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repository.delete(id)
            refreshRecent()
        }
    }

    /** Shows [text] in the Home screen's snackbar (navigating there if needed). */
    fun showMessage(text: String) {
        if (_screen.value != Screen.Home) navigate(Screen.Home)
        _message.value = text
    }
    fun messageShown() { _message.value = null }

    fun updateSettings(name: String, newDepth: Int) {
        settings.username = name
        settings.depth = newDepth
        username.value = settings.username
        depth.value = newDepth
    }

    private fun restoreScreen(): Screen {
        val s = handle.get<String>("screen") ?: return Screen.Home
        return when {
            s == "about" -> Screen.About
            s == "licenses" -> Screen.Licenses
            s.startsWith("license:") -> s.removePrefix("license:").split('|', limit = 2)
                .let { Screen.LicenseText(title = it.getOrElse(1) { "" }, asset = it[0]) }
            s.startsWith("review:") -> Screen.Review(s.removePrefix("review:"))
            else -> Screen.Home
        }
    }
}
