package com.pocketaudit.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

class ImageTextExtractor(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return@withContext null

        context.contentResolver.openInputStream(uri)?.use { stream ->
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(
                    bounds.outWidth,
                    bounds.outHeight,
                    MAX_EDGE_PX
                )
            }
            val bitmap = BitmapFactory.decodeStream(stream, null, decodeOptions) ?: return@withContext null
            try {
                val scaled = downscaleIfNeeded(bitmap, MAX_EDGE_PX)
                try {
                    val image = InputImage.fromBitmap(scaled, 0)
                    val result = recognizer.process(image).await()
                    result.text.trim().takeIf { it.isNotEmpty() }
                } finally {
                    if (scaled !== bitmap) {
                        scaled.recycle()
                    }
                }
            } finally {
                bitmap.recycle()
            }
        }
    }

    private fun downscaleIfNeeded(source: Bitmap, maxEdge: Int): Bitmap {
        val width = source.width
        val height = source.height
        val largest = max(width, height)
        if (largest <= maxEdge) return source
        val scale = maxEdge.toFloat() / largest.toFloat()
        val targetW = (width * scale).roundToInt().coerceAtLeast(1)
        val targetH = (height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var inSampleSize = 1
        val largest = max(width, height)
        while (largest / inSampleSize > maxEdge * 2) {
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    companion object {
        private const val MAX_EDGE_PX = 2048
    }
}
