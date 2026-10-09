package com.sangeetastudios.chessreview.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sangeetastudios.chessreview.data.SavedGame
import com.sangeetastudios.chessreview.ui.AppViewModel
import com.sangeetastudios.chessreview.ui.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AppViewModel, snackbar: SnackbarHostState) {
    val recent by vm.recent.collectAsStateWithLifecycle()
    val importing by vm.importing.collectAsStateWithLifecycle()
    val username by vm.username.collectAsStateWithLifecycle()
    val depth by vm.depth.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var pgn by rememberSaveable { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull()
            }
            if (text == null) vm.showMessage("Couldn't open that file.") else { pgn = text; vm.importPgn(text) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chess Review", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSettings = true }) { Icon(Icons.Filled.Settings, "Settings") }
                    IconButton(onClick = { vm.navigate(Screen.About) }) { Icon(Icons.Filled.Info, "About") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val wide = maxWidth >= 720.dp
            val importCard: @Composable (Modifier) -> Unit = { m ->
                ImportCard(
                    pgn = pgn,
                    onPgnChange = { pgn = it },
                    importing = importing,
                    onPaste = {
                        val text = clipboard.getText()?.text.orEmpty()
                        if (text.isBlank()) vm.showMessage("Clipboard is empty.") else { pgn = text; vm.importPgn(text) }
                    },
                    onOpenFile = { openFile.launch(arrayOf("application/x-chess-pgn", "application/vnd.chess-pgn", "text/*", "application/octet-stream")) },
                    onReview = { vm.importPgn(pgn) },
                    modifier = m,
                )
            }
            if (wide) {
                Row(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(1f)) { importCard(Modifier.fillMaxWidth()) }
                    LazyColumn(Modifier.weight(1f)) { recentItems(recent, vm) }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    item { importCard(Modifier.fillMaxWidth().padding(vertical = 8.dp)) }
                    recentItems(recent, vm)
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(username, depth, onDismiss = { showSettings = false }) { name, newDepth ->
            vm.updateSettings(name, newDepth)
            showSettings = false
        }
    }
}

private fun LazyListScope.recentItems(recent: List<SavedGame>, vm: AppViewModel) {
    item {
        Text(
            "Recent games",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
        )
    }
    if (recent.isEmpty()) {
        item {
            Text(
                "Games you review will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    items(recent, key = { it.id }) { g -> RecentGameRow(g, { vm.navigate(Screen.Review(g.id)) }, { vm.delete(g.id) }) }
    item { Spacer(Modifier.height(24.dp)) }
}

@Composable
private fun ImportCard(
    pgn: String,
    onPgnChange: (String) -> Unit,
    importing: Boolean,
    onPaste: () -> Unit,
    onOpenFile: () -> Unit,
    onReview: () -> Unit,
    modifier: Modifier,
) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(16.dp)) {
            Text("Review a game", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "From your chess app, open a finished game and tap Share → PGN → Chess Review. Or paste the PGN below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = pgn,
                onValueChange = onPgnChange,
                label = { Text("PGN") },
                placeholder = { Text("[Event \"…\"]\n1. e4 e5 2. Nf3 …") },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 220.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPaste, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.ContentPaste, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Paste", maxLines = 1)
                }
                OutlinedButton(onClick = onOpenFile, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.FolderOpen, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open file", maxLines = 1)
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onReview,
                enabled = !importing && pgn.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                if (importing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Review game", maxLines = 1)
            }
        }
    }
}

@Composable
private fun RecentGameRow(game: SavedGame, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${game.white} vs ${game.black}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(game.result, game.date?.replace('.', '-')).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete ${game.white} vs ${game.black}") }
        }
    }
}

@Composable
fun MessageEffect(vm: AppViewModel, snackbar: SnackbarHostState) {
    val message by vm.message.collectAsStateWithLifecycle()
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.messageShown()
        }
    }
}

