package com.sangeetastudios.chessreview.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sangeetastudios.chessreview.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class Component(
    val name: String,
    val notice: String,
    val license: String,
    /** Bundled license text in assets/licenses, with its button label. */
    val texts: List<Pair<String, String>>,
    val sourceUrl: String?,
)

private fun components() = listOf(
    Component(
        name = "Chess Review",
        notice = "Copyright © 2026 Sangeeta Studios",
        license = "GNU General Public License v3.0 or later",
        texts = listOf("GNU GPL v3" to "GPL-3.0.txt"),
        sourceUrl = BuildConfig.SOURCE_URL.ifBlank { null },
    ),
    Component(
        name = "Stockfish 17.1",
        notice = "Copyright © 2004-2025 The Stockfish developers",
        license = "GNU General Public License v3.0",
        texts = listOf("GNU GPL v3" to "GPL-3.0.txt", "Authors" to "Stockfish-AUTHORS.txt"),
        sourceUrl = BuildConfig.STOCKFISH_SOURCE_URL,
    ),
    Component(
        name = "chesslib 1.3.4",
        notice = "Copyright © 2017 Ben-Hur Carlos Vieira Langoni Junior",
        license = "Apache License 2.0",
        texts = listOf("Apache 2.0" to "Apache-2.0.txt"),
        sourceUrl = "https://github.com/bhlangonijr/chesslib",
    ),
    Component(
        name = "cburnett chess pieces",
        notice = "Copyright © Colin M.L. Burnett",
        license = "GNU General Public License v2.0 or later (used under v3)",
        texts = listOf("GNU GPL v3" to "GPL-3.0.txt"),
        sourceUrl = "https://github.com/lichess-org/lila/tree/master/public/piece/cburnett",
    ),
    Component(
        name = "AndroidX, Jetpack Compose, Kotlin",
        notice = "Copyright © The Android Open Source Project, JetBrains s.r.o.",
        license = "Apache License 2.0",
        texts = listOf("Apache 2.0" to "Apache-2.0.txt"),
        sourceUrl = "https://source.android.com",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit, onOpenText: (title: String, asset: String) -> Unit) {
    val uriHandler = LocalUriHandler.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Source code & licenses") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(components()) { c ->
                Card(
                    Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(c.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(c.notice, style = MaterialTheme.typography.bodyMedium)
                        Text(c.license, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            c.texts.forEach { (label, asset) ->
                                TextButton(onClick = { onOpenText("${c.name} — $label", asset) }) { Text(label) }
                            }
                            c.sourceUrl?.let { url -> TextButton(onClick = { uriHandler.openUri(url) }) { Text("Source code") } }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseTextScreen(title: String, asset: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var paragraphs by remember(asset) { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(asset) {
        paragraphs = withContext(Dispatchers.IO) {
            runCatching { context.assets.open("licenses/$asset").bufferedReader().use { it.readText() } }
                .getOrDefault("License text unavailable.")
                .split(Regex("\n\\s*\n"))
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        val list = paragraphs
        if (list == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            SelectionContainer {
                LazyColumn(
                    Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(list) { p ->
                        Text(p.trimEnd(), fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}
