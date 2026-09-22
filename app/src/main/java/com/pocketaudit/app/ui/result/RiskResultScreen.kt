package com.pocketaudit.app.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Rule
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
import com.pocketaudit.app.detection.DecidedBy
import com.pocketaudit.app.detection.RiskSource
import com.pocketaudit.app.detection.UpiUriParser
import com.pocketaudit.app.ui.common.riskSourcePresentation
import com.pocketaudit.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskResultScreen(
    alert: AlertEntity?,
    onBack: () -> Unit,
    onViewHistoryDetail: ((Long) -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.risk_result_title), style = Typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (alert == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val (riskColor, riskBg) = when (alert.riskLevel) {
            RiskLevel.HIGH -> Pair(RiskHighRed, RiskHighBg)
            RiskLevel.MEDIUM -> Pair(RiskMediumYellow, RiskMediumBg)
            else -> Pair(RiskLowGreen, RiskLowBg)
        }

        val sourceUi = riskSourcePresentation(alert.source)
        val decidedLabel = when (alert.decidedBy) {
            DecidedBy.RULES -> stringResource(R.string.decided_by_rules)
            DecidedBy.ON_DEVICE_AI -> stringResource(R.string.decided_by_ai)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
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
                        text = alert.riskLevel.name,
                        style = Typography.headlineLarge,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = riskColor
                    )
                    Text(
                        text = "${alert.riskScore}%",
                        style = Typography.titleLarge,
                        color = riskColor
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(sourceUi.icon, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(sourceUi.label, style = Typography.labelMedium, color = ElectricBlue)
                    }
                    Text(decidedLabel, style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (alert.source == RiskSource.QR_SCAN) {
                Spacer(Modifier.height(16.dp))
                QrPayeeCard(rawQr = alert.body)
            }

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = ElectricBlue)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.plain_language_explanation), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(alert.explanation, style = Typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Rule, contentDescription = null, tint = TechPurple)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.matched_scam_rules), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (alert.matchedPatterns.isEmpty()) {
                        Text(stringResource(R.string.no_rules_matched), color = TextSecondary)
                    } else {
                        alert.matchedPatterns.forEach { reason ->
                            Text("• $reason", modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.extracted_text_preview), style = Typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = alert.body.ifBlank { alert.title },
                        style = Typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            if (onViewHistoryDetail != null) {
                TextButton(onClick = { onViewHistoryDetail(alert.id) }) {
                    Text(stringResource(R.string.open_in_history))
                }
            }
        }
    }
}


@Composable
private fun QrPayeeCard(rawQr: String) {
    val parsed = UpiUriParser.parse(rawQr)
    val amountText = parsed.am?.takeIf { it.isNotBlank() }?.let { "₹$it" }
        ?: stringResource(R.string.qr_amount_not_set)
    val nameText = parsed.pn?.takeIf { it.isNotBlank() } ?: stringResource(R.string.qr_field_missing)
    val vpaText = parsed.pa?.takeIf { it.isNotBlank() } ?: stringResource(R.string.qr_field_missing)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (parsed.isUpiPay || !parsed.pa.isNullOrBlank()) {
                QrFieldRow(label = stringResource(R.string.qr_payee_name), value = nameText)
                QrFieldRow(label = stringResource(R.string.qr_payee_vpa), value = vpaText)
                QrFieldRow(label = stringResource(R.string.qr_amount), value = amountText)
            } else {
                Text(stringResource(R.string.qr_not_upi), style = Typography.bodyMedium)
            }
            Text(
                text = stringResource(R.string.qr_compare_shop_board),
                style = Typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.qr_cannot_verify_vpa),
                style = Typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QrFieldRow(label: String, value: String) {
    Column {
        Text(label, style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = Typography.titleLarge, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

