package com.pocketaudit.app.ui.detail

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketaudit.app.R
import com.pocketaudit.app.data.model.AlertEntity
import com.pocketaudit.app.data.model.RiskLevel
import com.pocketaudit.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDetailScreen(
    alert: AlertEntity?,
    onBack: () -> Unit,
    onMarkAsSafe: (Long) -> Unit,
    onDelete: (AlertEntity) -> Unit
) {
    if (alert == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            Text("Alert not found", style = Typography.bodyLarge)
        }
        return
    }

    val (riskColor, riskBg) = when (alert.riskLevel) {
        RiskLevel.HIGH -> Pair(RiskHighRed, RiskHighBg)
        RiskLevel.MEDIUM -> Pair(RiskMediumYellow, RiskMediumBg)
        else -> Pair(RiskLowGreen, RiskLowBg)
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val timestampText = try {
        DateUtils.formatDateTime(
            context,
            alert.timestamp,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR
        )
    } catch (e: Exception) {
        java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(alert.timestamp))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.alert_detail_title), style = Typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { onDelete(alert); onBack() }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_alert), tint = RiskHighRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Risk Header Score Meter
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = riskBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, riskColor.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.risk_assessment),
                        style = Typography.labelMedium,
                        color = riskColor,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${alert.riskScore}%",
                        style = Typography.headlineLarge,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = riskColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = CardSurfaceDark
                    ) {
                        Text(
                            text = alert.riskLevel.name + if (alert.isSafe) " (Marked Safe)" else "",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            style = Typography.titleLarge,
                            fontSize = 14.sp,
                            color = if (alert.isSafe) RiskLowGreen else riskColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Prominent Plain Language Breakdown Card (Item 4)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.plain_language_explanation),
                            style = Typography.titleLarge,
                            fontSize = 17.sp,
                            color = ElectricBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = alert.explanation.ifBlank { "This notification was flagged by PocketAudit on-device analysis due to detected financial scam risk patterns." },
                        style = Typography.bodyLarge,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Specific Rule Triggers Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Rule,
                            contentDescription = null,
                            tint = TechPurple,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.matched_scam_rules),
                            style = Typography.titleLarge,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (alert.matchedPatterns.isEmpty()) {
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("• ", color = TechPurple, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Triggered ${alert.scamType.name.replace('_', ' ')} security filter",
                                style = Typography.bodyMedium,
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        alert.matchedPatterns.forEach { pattern ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", color = TechPurple, fontWeight = FontWeight.Bold)
                                Text(
                                    text = pattern,
                                    style = Typography.bodyMedium,
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Original Intercepted Notification Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.original_payload),
                        style = Typography.labelMedium,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = alert.appName,
                            style = Typography.titleLarge,
                            fontSize = 14.sp,
                            color = ElectricBlue
                        )
                        Text(
                            text = timestampText,
                            style = Typography.labelMedium,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = alert.title,
                        style = Typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = alert.body,
                        style = Typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Package: ${alert.packageName}",
                        style = Typography.labelMedium,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mark as Safe Button
            if (!alert.isSafe) {
                OutlinedButton(
                    onClick = { onMarkAsSafe(alert.id); onBack() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RiskLowGreen)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = RiskLowGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.mark_as_safe), color = RiskLowGreen, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
