package com.pocketaudit.app.ui.dashboard

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketaudit.app.R
import com.pocketaudit.app.data.model.AlertEntity
import com.pocketaudit.app.data.model.RiskLevel
import com.pocketaudit.app.data.model.ScamType
import com.pocketaudit.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    alerts: List<AlertEntity>,
    onAlertClick: (Long) -> Unit,
    onOpenDemoMode: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearAll: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf<RiskLevel?>(null) }

    val filteredAlerts = remember(alerts, selectedFilter) {
        if (selectedFilter == null) alerts else alerts.filter { it.riskLevel == selectedFilter }
    }

    val highRiskCount = remember(alerts) { alerts.count { it.riskLevel == RiskLevel.HIGH } }
    val mediumRiskCount = remember(alerts) { alerts.count { it.riskLevel == RiskLevel.MEDIUM } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = Typography.headlineMedium,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                            tint = TextSecondary
                        )
                    }
                    IconButton(onClick = onClearAll) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear All",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenDemoMode,
                containerColor = TechPurple,
                contentColor = TextPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.demo_simulator_mode)) },
                text = { Text(stringResource(R.string.demo_simulator_mode), fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Status Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (highRiskCount > 0) RiskHighBg else CardSurfaceDark
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (highRiskCount > 0) RiskHighRed.copy(alpha = 0.5f) else CardBorderDark
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (highRiskCount > 0) RiskHighRed.copy(alpha = 0.2f) else RiskLowGreen.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (highRiskCount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (highRiskCount > 0) RiskHighRed else RiskLowGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (highRiskCount > 0) stringResource(R.string.alerts_flagged, highRiskCount) else stringResource(R.string.active_protection),
                            style = Typography.titleLarge,
                            fontSize = 16.sp
                        )
                        Text(
                            text = stringResource(R.string.monitoring_subtitle),
                            style = Typography.bodyMedium,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.high_risk),
                    count = highRiskCount,
                    color = RiskHighRed,
                    bgColor = RiskHighBg,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.medium_risk),
                    count = mediumRiskCount,
                    color = RiskMediumYellow,
                    bgColor = RiskMediumBg,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.total_scanned),
                    count = alerts.size,
                    color = ElectricBlue,
                    bgColor = CardSurfaceDark,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text(stringResource(R.string.filter_all, alerts.size)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CardBorderDark,
                        selectedLabelColor = TextPrimary
                    )
                )
                FilterChip(
                    selected = selectedFilter == RiskLevel.HIGH,
                    onClick = { selectedFilter = if (selectedFilter == RiskLevel.HIGH) null else RiskLevel.HIGH },
                    label = { Text(stringResource(R.string.filter_high, highRiskCount)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RiskHighRed,
                        selectedLabelColor = TextPrimary
                    )
                )
                FilterChip(
                    selected = selectedFilter == RiskLevel.MEDIUM,
                    onClick = { selectedFilter = if (selectedFilter == RiskLevel.MEDIUM) null else RiskLevel.MEDIUM },
                    label = { Text(stringResource(R.string.filter_medium, mediumRiskCount)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RiskMediumYellow,
                        selectedLabelColor = BackgroundDark
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Alert List Feed
            if (filteredAlerts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ShieldMoon,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.no_alerts_yet),
                            style = Typography.titleLarge,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredAlerts, key = { it.id }) { alert ->
                        AlertCardItem(
                            alert = alert,
                            onClick = { onAlertClick(alert.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    count: Int,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = Typography.labelMedium,
                color = color,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                style = Typography.headlineLarge,
                fontSize = 22.sp,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun AlertCardItem(
    alert: AlertEntity,
    onClick: () -> Unit
) {
    val (riskColor, riskBg) = when (alert.riskLevel) {
        RiskLevel.HIGH -> Pair(RiskHighRed, RiskHighBg)
        RiskLevel.MEDIUM -> Pair(RiskMediumYellow, RiskMediumBg)
        else -> Pair(RiskLowGreen, RiskLowBg)
    }

    val timeAgo = DateUtils.getRelativeTimeSpanString(
        alert.timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(riskColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alert.appName,
                        style = Typography.titleLarge,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = riskBg
                ) {
                    Text(
                        text = "${alert.riskLevel.name} (${alert.riskScore}%)",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = Typography.labelMedium,
                        color = riskColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = alert.title,
                style = Typography.bodyLarge,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = alert.body,
                style = Typography.bodyMedium,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getScamLabel(alert.scamType),
                    style = Typography.labelMedium,
                    color = TechPurple,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = timeAgo,
                    style = Typography.labelMedium,
                    color = TextMuted
                )
            }
        }
    }
}

private fun getScamLabel(scamType: ScamType): String {
    return when (scamType) {
        ScamType.FAKE_UPI_COLLECT -> "🚨 Fake UPI Collect Request"
        ScamType.PHISHING_LINK -> "⚠️ Phishing Link SMS"
        ScamType.SUBSCRIPTION_TRAP -> "🔄 Silent Subscription / Duplicate"
        ScamType.UNKNOWN_SUSPICIOUS -> "⚠️ Suspicious Pattern"
        ScamType.LEGITIMATE -> "✅ Legitimate"
    }
}
