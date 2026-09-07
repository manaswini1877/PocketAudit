package com.pocketaudit.app.detection

import com.pocketaudit.app.data.model.RiskLevel
import com.pocketaudit.app.data.model.ScamType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CollectRequestDetectorTest {

    private lateinit var detector: CollectRequestDetector

    @Before
    fun setUp() {
        detector = CollectRequestDetector()
    }

    @Test
    fun `detects fake UPI collect request disguised as refund`() {
        val title = "Collect Request Received"
        val body = "Collect request of ₹4,999 from cashback-claim@ybl. Enter UPI PIN to claim your refund now."

        val result = detector.detect("com.phonepe.app", title, body)

        assertEquals(RiskLevel.HIGH, result.riskLevel)
        assertEquals(ScamType.FAKE_UPI_COLLECT, result.scamType)
        assertTrue(result.riskScore in 75..100)
        assertTrue(result.matchedPatterns.any { it.contains("refund/cashback/claim") })
        assertTrue(result.matchedPatterns.any { it.contains("PIN Trap") })
        assertTrue(result.explanation.contains("DEBIT money"))
    }

    @Test
    fun `detects suspicious VPA handle prefix in collect request`() {
        val title = "Request from user"
        val body = "UPI Collect Request ₹2,000 from refund.service@okicici"

        val result = detector.detect("com.google.android.apps.nbu.paisa.user", title, body)

        assertTrue(result.riskScore >= 50)
        assertTrue(result.matchedPatterns.any { it.contains("Suspicious VPA") })
    }

    @Test
    fun `negative test - confirms legitimate merchant payment is NOT flagged`() {
        val title = "Payment Successful"
        val body = "Paid ₹150 to Corner Bakery via PhonePe, Transaction ID 482910492."

        val result = detector.detect("com.phonepe.app", title, body)

        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertEquals(0, result.riskScore)
        assertEquals(ScamType.LEGITIMATE, result.scamType)
        assertTrue(result.matchedPatterns.isEmpty())
    }

    @Test
    fun `negative test - confirms generic social request notifications are NOT flagged`() {
        val title = "New Friend Request"
        val body = "Alex sent you a friend request on Instagram."

        val result = detector.detect("com.instagram.android", title, body)

        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertEquals(0, result.riskScore)
    }
}
