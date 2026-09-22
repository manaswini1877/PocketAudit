package com.pocketaudit.app.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QrRiskRulesTest {

    private fun ids(raw: String): Set<String> =
        QrRiskRules.evaluate(raw).signals.map { it.id }.toSet()

    @Test
    fun highAmount_flagsPreFilledUpiAmount() {
        val ids = ids("upi://pay?pa=shop@ybl&pn=Shop&am=50000")
        assertTrue(ids.contains("high_amount"))
    }

    @Test
    fun highAmount_ignoresSmallAmount() {
        val ids = ids("upi://pay?pa=shop@ybl&pn=Shop&am=120")
        assertFalse(ids.contains("high_amount"))
    }

    @Test
    fun suspiciousNote_flagsUrgencyLotteryRefundReward() {
        val ids = ids("upi://pay?pa=shop@ybl&pn=Shop&tn=Claim%20lottery%20prize%20urgent")
        assertTrue(ids.contains("suspicious_note"))
    }

    @Test
    fun randomVpa_flagsConsonantHeavyHandle() {
        assertTrue(QrRiskRules.looksRandomVpa("xqzvnmwptrkb@ybl"))
        val ids = ids("upi://pay?pa=xqzvnmwptrkb@ybl&pn=Shop")
        assertTrue(ids.contains("random_vpa"))
    }

    @Test
    fun randomVpa_ignoresReadableHandle() {
        assertFalse(QrRiskRules.looksRandomVpa("ravi.kumar@oksbi"))
        val ids = ids("upi://pay?pa=ravi.kumar@oksbi&pn=Ravi Kumar")
        assertFalse(ids.contains("random_vpa"))
    }

    @Test
    fun nameVpaMismatch_whenNameDoesNotResembleHandle() {
        assertFalse(QrRiskRules.nameResemblesVpa("Amazon Pay", "xqzvnmwptrkb@ybl"))
        val ids = ids("upi://pay?pa=xqzvnmwptrkb@ybl&pn=Amazon%20Pay")
        assertTrue(ids.contains("name_vpa_mismatch"))
    }

    @Test
    fun nameVpaMatch_whenNameSharesHandleTokens() {
        assertTrue(QrRiskRules.nameResemblesVpa("Ravi Kumar", "ravi.kumar@oksbi"))
        val ids = ids("upi://pay?pa=ravi.kumar@oksbi&pn=Ravi%20Kumar")
        assertFalse(ids.contains("name_vpa_mismatch"))
    }

    @Test
    fun upiUrlParam_isASignalNotProof() {
        val assessment = QrRiskRules.evaluate("upi://pay?pa=shop@ybl&pn=Shop&url=https://example.com/pay")
        assertTrue(assessment.signals.any { it.id == "upi_url_param" })
        assertTrue(assessment.reasons.any { it.contains("url parameter") })
    }

    @Test
    fun nonUpi_insecureHttp() {
        val ids = ids("http://example.com/pay")
        assertTrue(ids.contains("insecure_http"))
    }

    @Test
    fun nonUpi_urlShortener() {
        val ids = ids("https://bit.ly/upi-kyc")
        assertTrue(ids.contains("shortener"))
    }

    @Test
    fun nonUpi_ipHost() {
        val ids = ids("http://185.22.11.9/login")
        assertTrue(ids.contains("ip_host"))
    }

    @Test
    fun nonUpi_punycodeLookalike() {
        val ids = ids("https://xn--80ak6aa92e.com/verify")
        assertTrue(ids.contains("lookalike"))
    }

    @Test
    fun nonUpi_suspiciousTld() {
        val ids = ids("https://secure-upi.xyz/pay")
        assertTrue(ids.contains("suspicious_tld"))
    }

    @Test
    fun nonUpi_obfuscatedLongUrl() {
        val longUrl = "https://example.com/pay?" +
            List(12) { i -> "p$i=%3D%3D%3D%3D" }.joinToString("&") +
            "&pad=" + "a".repeat(80)
        val ids = ids(longUrl)
        assertTrue(ids.contains("obfuscated"))
    }

    @Test
    fun safeMerchantQr_hasNoSignals() {
        val assessment = QrRiskRules.evaluate(QrSamplePayloads.SAFE_MERCHANT_TEA)
        assertTrue(assessment.signals.isEmpty())
        assertEquals(0, assessment.score)
    }
}
