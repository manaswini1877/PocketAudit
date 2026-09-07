package com.pocketaudit.app.ui.demo

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketaudit.app.ui.theme.*

data class DemoScenario(
    val id: String,
    val title: String,
    val description: String,
    val packageName: String,
    val notifTitle: String,
    val notifBody: String,
    val expectedRisk: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoModeScreen(
    onTriggerScenario: (packageName: String, title: String, body: String) -> Unit,
    onBackToDashboard: () -> Unit
) {
    var lastTriggered by remember { mutableStateOf<String?>(null) }

    val scenarios = listOf(
        DemoScenario(
            id = "upi_collect",
            title = "1. Fake UPI Collect Request",
            description = "Simulates a scammer sending a collect request disguised as a ₹4,999 cashback refund asking for UPI PIN.",
            packageName = "com.phonepe.app",
            notifTitle = "UPI Collect Request Received",
            notifBody = "Collect request of ₹4,999 from cashback-claim@ybl. Enter UPI PIN to claim refund.",
            expectedRisk = "HIGH RISK (85%)",
            color = RiskHighRed
        ),
        DemoScenario(
            id = "phishing_sms",
            title = "2. Phishing Link SMS",
            description = "Simulates an SMS claiming account suspension with shortened URL bit.ly and 24-hour urgency panic.",
            packageName = "com.google.android.apps.messaging",
            notifTitle = "URGENT BANK ALERT",
            notifBody = "ALERT: Your HDFC bank account is blocked. Verify KYC immediately at http://bit.ly/hdfc-kyc-verify within 24 hours.",
            expectedRisk = "HIGH RISK (80%)",
            color = RiskHighRed
        ),
        DemoScenario(
            id = "subscription_trap",
            title = "3. Silent Subscription Trap",
            description = "Simulates an unauthorized recurring mandate debited by an unknown descriptor merchant.",
            packageName = "net.one97.paytm",
            notifTitle = "Autopay Mandate Executed",
            notifBody = "Autopay executed: ₹999 debited by Unknown global services for recurring mandate.",
            expectedRisk = "MEDIUM RISK (65%)",
            color = RiskMediumYellow
        ),
        DemoScenario(
            id = "legitimate_tx",
            title = "4. Legitimate Payment (Safe)",
            description = "Simulates a standard GPay transaction to verify benign notifications are NOT flagged.",
            packageName = "com.google.android.apps.nbu.paisa.user",
            notifTitle = "Payment Successful",
            notifBody = "Sent ₹150 to Corner Bakery using Google Pay. Transaction ID 9382910492.",
            expectedRisk = "SAFE (0%)",
            color = RiskLowGreen
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hackathon Live Demo Mode", style = Typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackToDashboard) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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
            // Demo Explanation Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TechPurple)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = TechPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Judge Demo Simulator Pipeline",
                            style = Typography.titleLarge,
                            fontSize = 17.sp,
                            color = TechPurple
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tap any scenario button below to inject a simulated notification payload into the real on-device DetectionEngine. High risk alerts will immediately save to Room DB and update the live dashboard.",
                        style = Typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            scenarios.forEach { scenario ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = scenario.title,
                                style = Typography.titleLarge,
                                fontSize = 16.sp
                            )

                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = scenario.color.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = scenario.expectedRisk,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = Typography.labelMedium,
                                    color = scenario.color,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = scenario.description,
                            style = Typography.bodyMedium,
                            color = TextSecondary,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Payload snippet box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundDark)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = scenario.notifTitle,
                                    style = Typography.bodyLarge,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = scenario.notifBody,
                                    style = Typography.bodyMedium,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                onTriggerScenario(scenario.packageName, scenario.notifTitle, scenario.notifBody)
                                lastTriggered = scenario.title
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate Notification Pipeline", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (lastTriggered != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RiskLowBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RiskLowGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Fired '$lastTriggered' through DetectionEngine!",
                            color = RiskLowGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = onBackToDashboard,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RiskLowGreen)
            ) {
                Text("View Results on Live Dashboard", style = Typography.titleLarge, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
