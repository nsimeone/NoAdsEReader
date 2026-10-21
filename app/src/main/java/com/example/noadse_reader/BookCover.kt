package com.example.noadse_reader

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BookCover(coverPath: String?, title: String, modifier: Modifier = Modifier) {
    val cover by produceState<ImageBitmap?>(null, coverPath) {
        value = if (coverPath.isNullOrEmpty()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(coverPath)?.asImageBitmap()
            }
        }
    }

    val image = cover
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(title.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium)
        }
    }
}