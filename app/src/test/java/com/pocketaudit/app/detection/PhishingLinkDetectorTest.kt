package com.pocketaudit.app.detection

import com.pocketaudit.app.data.model.RiskLevel
import com.pocketaudit.app.data.model.ScamType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PhishingLinkDetectorTest {

    private lateinit var detector: PhishingLinkDetector

    @Before
    fun setUp() {
        detector = PhishingLinkDetector()
    }

    @Test
    fun `detects banking phishing link with shortened URL and urgency`() {
        val title = "URGENT BANK ALERT"
        val body = "ALERT: Your HDFC bank account is blocked. Verify KYC immediately at http://bit.ly/hdfc-kyc-verify within 24 hours to avoid penalty."

        val result = detector.detect("com.google.android.apps.messaging", title, body)

        assertEquals(RiskLevel.HIGH, result.riskLevel)
        assertEquals(ScamType.PHISHING_LINK, result.scamType)
        assertTrue(result.riskScore in 75..100)
        assertTrue(result.matchedPatterns.any { it.contains("Shortened URL") })
        assertTrue(result.matchedPatterns.any { it.contains("Urgency") })
        assertTrue(result.explanation.contains("Phishing"))
    }

    @Test
    fun `detects spoofed banking domain with suspicious TLD`() {
        val title = "SBI Security Notice"
        val body = "SBI: Account suspended. Update your PAN card details at http://sbi-bank-verify.xyz/login"

        val result = detector.detect("com.google.android.apps.messaging", title, body)

        assertEquals(RiskLevel.HIGH, result.riskLevel)
        assertTrue(result.riskScore in 75..100)
        assertTrue(result.matchedPatterns.any { it.contains("Spoofed banking domain") || it.contains("TLD") })
    }

    @Test
    fun `negative test - confirms legitimate merchant payment is NOT flagged`() {
        val title = "Payment Successful"
        val body = "Paid ₹150 to Corner Bakery via PhonePe, Transaction ID 482910492."

        val result = detector.detect("com.google.android.apps.messaging", title, body)

        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertEquals(0, result.riskScore)
        assertEquals(ScamType.LEGITIMATE, result.scamType)
    }

    @Test
    fun `negative test - confirms normal bank OTP transaction SMS is NOT flagged`() {
        val title = "HDFC Bank"
        val body = "Your OTP for transaction at Amazon is 492019. Do not share OTP with anyone. Valid for 10 mins."

        val result = detector.detect("com.google.android.apps.messaging", title, body)

        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertEquals(0, result.riskScore)
    }

    @Test
    fun `negative test - confirms chat message without link is NOT flagged`() {
        val title = "Rahul (WhatsApp)"
        val body = "Hey, my Netflix account got blocked yesterday 24 hours ago, can I use yours?"

        val result = detector.detect("com.whatsapp", title, body)

        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertEquals(0, result.riskScore)
    }
}
