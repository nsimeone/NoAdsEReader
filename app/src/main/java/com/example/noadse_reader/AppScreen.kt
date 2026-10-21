package com.example.noadse_reader

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        syncLibrary(context)
        extractMissingCovers(context)
    }

    Column(modifier = modifier) {
        PrimaryTabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Library") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Import") })
        }
        when (tab) {
            0 -> LibraryScreen(modifier = Modifier.weight(1f))
            else -> ImportScreen(modifier = Modifier.weight(1f))
        }
    }
}