package com.sangeetastudios.chessreview

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.sangeetastudios.chessreview.ui.AppViewModel
import com.sangeetastudios.chessreview.ui.Screen
import com.sangeetastudios.chessreview.ui.about.AboutScreen
import com.sangeetastudios.chessreview.ui.about.LicenseTextScreen
import com.sangeetastudios.chessreview.ui.about.LicensesScreen
import com.sangeetastudios.chessreview.ui.home.HomeScreen
import com.sangeetastudios.chessreview.ui.home.MessageEffect
import com.sangeetastudios.chessreview.ui.review.ReviewScreen
import com.sangeetastudios.chessreview.ui.review.ReviewViewModel
import com.sangeetastudios.chessreview.ui.theme.ChessReviewTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()
    private val reviewViewModel: ReviewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            ChessReviewTheme {
                Surface { AppRoot(appViewModel, reviewViewModel) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /** Accepts PGN text shared via ACTION_SEND or a .pgn file via ACTION_SEND/ACTION_VIEW. */
    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            Intent.ACTION_VIEW -> intent.data
            else -> return
        }
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        lifecycleScope.launch {
            val pgn = text?.takeIf { it.isNotBlank() } ?: uri?.let { readUri(it) }
            if (pgn.isNullOrBlank()) {
                appViewModel.showMessage("Nothing to review in what was shared.")
            } else {
                appViewModel.importPgn(pgn)
            }
        }
    }

    private suspend fun readUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openInputStream(uri)?.use { input ->
                // PGN files are small; cap the read to avoid loading something huge by mistake.
                input.readNBytesCompat(2 * 1024 * 1024).decodeToString()
            }
        }.getOrNull()
    }

    private fun java.io.InputStream.readNBytesCompat(max: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (out.size() < max) {
            val n = read(buf, 0, minOf(buf.size, max - out.size()))
            if (n < 0) break
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
}

@Composable
private fun AppRoot(vm: AppViewModel, reviewVm: ReviewViewModel) {
    val screen by vm.screen.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MessageEffect(vm, snackbar)
    BackHandler(enabled = screen != Screen.Home) { vm.back() }
    when (val s = screen) {
        Screen.Home -> HomeScreen(vm, snackbar)
        Screen.About -> AboutScreen(onBack = { vm.back() }, onLicenses = { vm.navigate(Screen.Licenses) })
        Screen.Licenses -> LicensesScreen(onBack = { vm.back() }, onOpenText = { title, asset -> vm.navigate(Screen.LicenseText(title, asset)) })
        is Screen.LicenseText -> LicenseTextScreen(s.title, s.asset, onBack = { vm.back() })
        is Screen.Review -> ReviewScreen(s.gameId, reviewVm, onBack = { vm.back() })
    }
}
