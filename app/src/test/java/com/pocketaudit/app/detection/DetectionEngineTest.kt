package com.pocketaudit.app.detection

import com.pocketaudit.app.data.model.RiskLevel
import com.pocketaudit.app.data.model.ScamType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DetectionEngineTest {

    private lateinit var engine: DetectionEngine

    @Before
    fun setUp() {
        engine = DetectionEngine()
    }

    @Test
    fun `engine evaluates multiple detectors and picks highest risk score clamped to 100`() {
        val title = "URGENT PAYMENT CLAIM"
        val body = "Collect request ₹9,999 from claim.refund@ybl. Enter PIN to claim refund before account blocked at http://bit.ly/sbi-claim"

        val result = engine.analyze("com.phonepe.app", title, body)

        assertEquals(RiskLevel.HIGH, result.riskLevel)
        assertTrue(result.riskScore in 70..100) // Score must be strictly clamped between 0 and 100
        assertTrue(result.riskScore <= 100)
        assertTrue(result.matchedPatterns.size >= 2)
        assertNotEquals(ScamType.LEGITIMATE, result.scamType)
    }

    @Test
    fun `negative test - confirms legitimate merchant payment is NOT flagged`() {
        val title = "Payment Successful"
        val body = "Paid ₹150 to Corner Bakery via PhonePe, Transaction ID 482910492."

        val result = engine.analyze("com.google.android.apps.nbu.paisa.user", title, body)

        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertEquals(0, result.riskScore)
        assertEquals(ScamType.LEGITIMATE, result.scamType)
        assertTrue(result.matchedPatterns.isEmpty())
    }

    @Test
    fun `negative test - confirms normal daily transactions are NOT flagged`() {
        val title = "Payment Successful"
        val body = "Sent ₹50 to Chai Corner using Google Pay."

        val result = engine.analyze("com.google.android.apps.nbu.paisa.user", title, body)

        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertEquals(0, result.riskScore)
        assertEquals(ScamType.LEGITIMATE, result.scamType)
    }
}
