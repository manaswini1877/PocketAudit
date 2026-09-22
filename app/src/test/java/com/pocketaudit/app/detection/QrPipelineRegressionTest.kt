package com.pocketaudit.app.detection

import com.pocketaudit.app.data.model.RiskLevel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QrPipelineRegressionTest {

    private val analyzer = RulesRiskAnalyzer()

    private fun analyzeEndToEnd(raw: String, source: RiskSource = RiskSource.QR_SCAN): RiskResult = runBlocking {
        val parsed = UpiUriParser.parse(raw)
        val qr = QrRiskRules.evaluate(raw, parsed)
        val input = RiskInput(
            text = raw,
            source = source,
            displayTitle = "Test",
            displayBody = raw,
            extraReasons = qr.reasons,
            extraScore = qr.score
        )
        val engineResult = analyzer.analyze(input)
        val (mergedScore, mergedLevel) = QrRiskRules.mergeWithEngine(
            engineScore = engineResult.score,
            engineLevel = engineResult.level,
            qrScore = input.extraScore
        )
        val mergedReasons = (engineResult.reasons + input.extraReasons).distinct()
        engineResult.copy(
            level = mergedLevel,
            score = mergedScore,
            reasons = mergedReasons
        )
    }

    @Test
    fun scamLotteryHighAmount_scoresHighWithExpectedReasons() {
        val raw = "upi://pay?pa=win.crorepati99@ybl&pn=KBC%20Lottery%20Head%20Office&am=25000&tn=Claim%20your%20lottery%20prize%20immediately"
        val result = analyzeEndToEnd(raw)

        assertTrue("Expected score >= 70, was ${result.score}", result.score >= 70)
        assertEquals(RiskLevel.HIGH, result.level)
        assertTrue(result.reasons.any { it.contains("25000") || it.contains("unusually high") })
        assertTrue(result.reasons.any { it.contains("urgency") || it.contains("lottery") })
        assertTrue(result.reasons.any { it.contains("resemble") || it.contains("mismatch") })
    }

    @Test
    fun scamPhishingUrl_scoresHighWithExpectedReasons() {
        val raw = "upi://pay?pa=support@icici&pn=ICICI%20Bank&url=http://185.22.11.9/login?ref=secure"
        val result = analyzeEndToEnd(raw)

        assertEquals(100, result.score)
        assertEquals(RiskLevel.HIGH, result.level)
        assertTrue(result.reasons.any { it.contains("IP address") })
        assertTrue(result.reasons.any { it.contains("http (not https)") })
        assertTrue(result.reasons.any { it.contains("url parameter") })
    }

    @Test
    fun scamNameMismatch_scoresMediumWithExpectedReasons() {
        val raw = "upi://pay?pa=unknown.random9876@okhdfcbank&pn=Electricity%20Bill%20Desk&am=3450&tn=Urgent%20bill%20payment%20due"
        val result = analyzeEndToEnd(raw)

        assertTrue("Expected score >= 50, was ${result.score}", result.score >= 50)
        assertEquals(RiskLevel.MEDIUM, result.level)
        assertTrue(result.reasons.any { it.contains("urgency") })
        assertTrue(result.reasons.any { it.contains("resemble") || it.contains("mismatch") })
    }

    @Test
    fun safeMerchantPayloads_remainSafeWithZeroScore() {
        val safeTea = analyzeEndToEnd(QrSamplePayloads.SAFE_MERCHANT_TEA)
        assertEquals(0, safeTea.score)
        assertEquals(RiskLevel.SAFE, safeTea.level)
        assertTrue(safeTea.reasons.isEmpty())

        val safeStores = analyzeEndToEnd(QrSamplePayloads.SAFE_MERCHANT_NO_AMOUNT)
        assertEquals(0, safeStores.score)
        assertEquals(RiskLevel.SAFE, safeStores.level)
        assertTrue(safeStores.reasons.isEmpty())

        val safeHttps = analyzeEndToEnd(QrSamplePayloads.SAFE_HTTPS_INFO)
        assertEquals(0, safeHttps.score)
        assertEquals(RiskLevel.SAFE, safeHttps.level)
        assertTrue(safeHttps.reasons.isEmpty())
    }

    @Test
    fun isSafeEvaluation_isFalseForMediumAndHigh() {
        val scam1 = analyzeEndToEnd("upi://pay?pa=win.crorepati99@ybl&pn=KBC%20Lottery%20Head%20Office&am=25000&tn=Claim%20your%20lottery%20prize%20immediately")
        val isSafe1 = (scam1.level == RiskLevel.SAFE || scam1.level == RiskLevel.LOW)
        assertFalse(isSafe1)

        val scam3 = analyzeEndToEnd("upi://pay?pa=unknown.random9876@okhdfcbank&pn=Electricity%20Bill%20Desk&am=3450&tn=Urgent%20bill%20payment%20due")
        val isSafe3 = (scam3.level == RiskLevel.SAFE || scam3.level == RiskLevel.LOW)
        assertFalse(isSafe3)
    }
}
