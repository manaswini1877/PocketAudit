package com.pocketaudit.app.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.vector.ImageVector
import com.pocketaudit.app.detection.RiskSource

data class RiskSourcePresentation(
    val label: String,
    val icon: ImageVector
)

fun riskSourcePresentation(source: RiskSource): RiskSourcePresentation = when (source) {
    RiskSource.NOTIFICATION -> RiskSourcePresentation("Notification", Icons.Default.Notifications)
    RiskSource.SIMULATOR -> RiskSourcePresentation("Demo simulator", Icons.Default.PlayArrow)
    RiskSource.SHARED_TEXT -> RiskSourcePresentation("Shared text", Icons.Default.Share)
    RiskSource.SHARED_IMAGE -> RiskSourcePresentation("Shared image", Icons.Default.Image)
    RiskSource.CLIPBOARD -> RiskSourcePresentation("Clipboard", Icons.Default.ContentPaste)
    RiskSource.MANUAL -> RiskSourcePresentation("Manual check", Icons.Default.EditNote)
    RiskSource.QR_SCAN -> RiskSourcePresentation("QR scan", Icons.Default.QrCodeScanner)
}
