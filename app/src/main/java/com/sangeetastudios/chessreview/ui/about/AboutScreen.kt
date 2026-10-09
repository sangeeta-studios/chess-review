package com.sangeetastudios.chessreview.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sangeetastudios.chessreview.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, onLicenses: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp)) {
                Text("Chess Review", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Version ${BuildConfig.VERSION_NAME} · Sangeeta Studios", style = MaterialTheme.typography.bodyMedium)
                Section("Privacy", PRIVACY)
                Section("Not affiliated", "Chess Review is an independent app and is not affiliated with or endorsed by Chess.com or any other chess platform.")
                Section("Free software", LICENSE_SUMMARY)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onLicenses) { Text("Source code & licenses") }
            }
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Spacer(Modifier.height(20.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium)
}

private const val PRIVACY =
    "All analysis runs on your device. The app has no internet permission, collects no personal data, " +
        "and stores the games you import only on this device."

private const val LICENSE_SUMMARY =
    "Chess Review is free software: you can redistribute it and/or modify it under the terms of the " +
        "GNU General Public License, version 3 or later. It comes with ABSOLUTELY NO WARRANTY. " +
        "It includes the Stockfish chess engine (GPLv3), chesslib (Apache 2.0) and the cburnett chess " +
        "pieces (GPLv2+)."
