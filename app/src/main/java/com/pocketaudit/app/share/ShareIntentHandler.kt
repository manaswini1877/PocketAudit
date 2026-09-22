package com.pocketaudit.app.share

import android.content.Intent
import android.net.Uri
import android.os.Build

sealed class SharePayload {
    data class Text(val value: String) : SharePayload()
    data class Image(val uri: Uri) : SharePayload()
}

object ShareIntentHandler {

    fun parse(intent: Intent?): SharePayload? {
        if (intent == null || intent.action != Intent.ACTION_SEND) return null
        val type = intent.type ?: return null

        if (type.startsWith("text/")) {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
            if (shared.isNotBlank()) return SharePayload.Text(shared)
            return null
        }

        if (type.startsWith("image/")) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            if (uri != null) return SharePayload.Image(uri)
        }
        return null
    }
}
