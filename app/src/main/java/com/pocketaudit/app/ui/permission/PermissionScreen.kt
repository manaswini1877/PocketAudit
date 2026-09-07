package com.pocketaudit.app.ui.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketaudit.app.ui.theme.*

@Composable
fun PermissionScreen(
    onPermissionGranted: () -> Unit,
    onSkipToDemo: () -> Unit
) {
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(PermissionHelper.isNotificationListenerGranted(context)) }
    var isBatteryOptIgnored by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context)) }
    val isOemDevice = remember { PermissionHelper.isOEMAggressiveDevice() }
    val oemName = remember { PermissionHelper.getOEMName() }

    // Re-check permission whenever user comes back to screen
    DisposableEffect(Unit) {
        onDispose { }
    }

    LaunchedEffect(isGranted) {
        if (isGranted) {
            onPermissionGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Shield Icon Header
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(ElectricBlue.copy(alpha = 0.15f))
                .border(1.dp, ElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Shield",
                tint = ElectricBlue,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Notification Access Needed",
            style = Typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "PocketAudit monitors incoming banking & UPI notifications on-device in real time to protect you against fake collect requests, phishing, and hidden traps.",
            style = Typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // On-Device Privacy Badge
        Surface(
            shape = RoundedCornerShape(50.dp),
            color = CardSurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = RiskLowGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "100% On-Device • Zero Cloud / Internet Access",
                    style = Typography.labelMedium,
                    color = RiskLowGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Primary Notification Permission Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = if (isGranted) RiskLowGreen else ElectricBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "1. Enable Notification Access",
                            style = Typography.titleLarge,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isGranted) "Permission Granted" else "Action Required",
                            style = Typography.bodyMedium,
                            color = if (isGranted) RiskLowGreen else RiskMediumYellow
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        PermissionHelper.openNotificationListenerSettings(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGranted) CardBorderDark else ElectricBlue
                    )
                ) {
                    Text(
                        text = if (isGranted) "Re-check Permission Status" else "Grant Notification Access",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // OEM Battery Optimization Tip Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isOemDevice) RiskMediumBg.copy(alpha = 0.5f) else CardSurfaceDark
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isOemDevice) RiskMediumYellow.copy(alpha = 0.4f) else CardBorderDark
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatterySaver,
                        contentDescription = null,
                        tint = RiskMediumYellow,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "2. OEM Battery & Autostart Tip",
                            style = Typography.titleLarge,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Detected: $oemName",
                            style = Typography.labelMedium,
                            color = RiskMediumYellow
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "On devices like Vivo/iQOO 15, Xiaomi (MIUI), and Oppo, aggressive battery optimization can kill background notification listeners. Disable battery optimization & allow Autostart for PocketAudit so alerts never stop appearing.",
                    style = Typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        PermissionHelper.openBatteryOptimizationSettings(context)
                        isBatteryOptIgnored = PermissionHelper.isIgnoringBatteryOptimizations(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RiskMediumYellow)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = RiskMediumYellow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBatteryOptIgnored) "Battery Optimization Disabled ✓" else "Open Battery / Autostart Settings",
                        color = RiskMediumYellow,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Continue / Demo Mode Buttons
        if (isGranted) {
            Button(
                onClick = onPermissionGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RiskLowGreen)
            ) {
                Text(
                    text = "Go to Security Dashboard",
                    style = Typography.titleLarge,
                    color = TextPrimary
                )
            }
        } else {
            OutlinedButton(
                onClick = onSkipToDemo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TechPurple)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = TechPurple
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Launch Live Hackathon Demo Mode",
                    style = Typography.titleLarge,
                    color = TechPurple
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
