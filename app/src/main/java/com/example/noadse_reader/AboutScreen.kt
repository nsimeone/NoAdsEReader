package com.example.noadse_reader

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.IOException

private fun readAsset(context: Context, name: String): String =
    try {
        context.assets.open(name).bufferedReader().use { it.readText() }
    } catch (e: IOException) {
        "License text missing: $name"
    }

@Suppress("DEPRECATION")
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appName = remember {
        context.applicationInfo.loadLabel(context.packageManager).toString()
    }
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
    }
    val readiumLicense = remember { readAsset(context, "LICENSE-readium.txt") }
    val apacheLicense = remember { readAsset(context, "LICENSE-apache-2.0.txt") }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(appName, style = MaterialTheme.typography.headlineSmall)
        Text("Version $version", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Privacy", style = MaterialTheme.typography.titleMedium)
        Text(
            "This app has no internet access and collects no data. Books you add are " +
                    "copied into the app's private storage on this phone, and nothing leaves it.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Open-source software", style = MaterialTheme.typography.titleMedium)
        Text(
            "This app is built with open-source libraries. Their licenses are " +
                    "reproduced below, as those licenses require.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text("Readium Kotlin Toolkit", style = MaterialTheme.typography.titleSmall)
        Text(
            "Not affiliated with or endorsed by the Readium Foundation.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(readiumLicense, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Android Jetpack, Compose, Room, Material and Kotlin libraries",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(apacheLicense, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "This app is provided as is, without warranty of any kind.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}