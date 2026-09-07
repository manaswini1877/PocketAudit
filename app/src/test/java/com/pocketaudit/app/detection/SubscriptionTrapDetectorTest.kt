package com.pocketaudit.app.detection

import com.pocketaudit.app.data.model.RiskLevel
import com.pocketaudit.app.data.model.ScamType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SubscriptionTrapDetectorTest {

    private lateinit var detector: SubscriptionTrapDetector

    @Before
    fun setUp() {
        detector = SubscriptionTrapDetector()
    }

    @Test
    fun `detects autopay subscription charge from unfamiliar merchant`() {
        val title = "Autopay Debited"
        val body = "Autopay executed: ₹999 debited by Unknown global services for recurring mandate."

        val result = detector.detect("net.one97.paytm", title, body)

        assertTrue(result.riskScore in 60..100)
        assertEquals(ScamType.SUBSCRIPTION_TRAP, result.scamType)
        assertTrue(result.matchedPatterns.any { it.contains("Autopay") })
        assertTrue(result.matchedPatterns.any { it.contains("merchant") })
    }

    @Test
    fun `detects duplicate charge within short 5-minute window`() {
        val title = "Account Debited"
        val body = "Your A/C debited by ₹1,499 for purchase at Digital Store."

        val now = System.currentTimeMillis()
        val history = listOf(
            TransactionContext(amount = 1499.0, merchant = "Digital Store", timestamp = now - 60_000) // 1 minute ago
        )

        val result = detector.detect("com.sbi.lotusintouch", title, body, history)

        assertTrue(result.riskScore >= 45)
        assertTrue(result.matchedPatterns.any { it.contains("Duplicate charge") })
    }

    @Test
    fun `negative test - confirms legitimate merchant payment is NOT flagged`() {
        val title = "Payment Successful"
        val body = "Paid ₹150 to Corner Bakery via PhonePe, Transaction ID 482910492."

        val result = detector.detect("com.google.android.apps.nbu.paisa.user", title, body)

        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertEquals(0, result.riskScore)
        assertEquals(ScamType.LEGITIMATE, result.scamType)
    }

    @Test
    fun `negative test - confirms verified Netflix subscription is NOT flagged`() {
        val title = "Subscription Renewed"
        val body = "Autopay executed: ₹649 debited for Netflix subscription renewal."

        val result = detector.detect("net.one97.paytm", title, body)

        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertEquals(0, result.riskScore)
    }
}
