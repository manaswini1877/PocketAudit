package com.pocketaudit.app.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QrSamplePayloadsTest {

    @Test
    fun sixPayloads_coverSafeAndScam() {
        val payloads = listOf(
            QrSamplePayloads.SAFE_MERCHANT_TEA,
            QrSamplePayloads.SAFE_MERCHANT_NO_AMOUNT,
            QrSamplePayloads.SAFE_HTTPS_INFO,
            QrSamplePayloads.SCAM_LOTTERY_HIGH_AMOUNT,
            QrSamplePayloads.SCAM_KYC_SHORTENER,
            QrSamplePayloads.SCAM_IP_OBFUSCATED
        )
        assertEquals(6, payloads.size)
        payloads.forEach { assertTrue(it.isNotBlank()) }

        assertTrue(QrRiskRules.evaluate(QrSamplePayloads.SAFE_MERCHANT_TEA).signals.isEmpty())
        assertTrue(QrRiskRules.evaluate(QrSamplePayloads.SAFE_MERCHANT_NO_AMOUNT).signals.isEmpty())
        assertTrue(QrRiskRules.evaluate(QrSamplePayloads.SAFE_HTTPS_INFO).signals.isEmpty())

        val lottery = QrRiskRules.evaluate(QrSamplePayloads.SCAM_LOTTERY_HIGH_AMOUNT)
        assertTrue(lottery.signals.map { it.id }.containsAll(listOf("high_amount", "suspicious_note", "random_vpa", "name_vpa_mismatch")))
        assertTrue(lottery.score >= 75)

        val kyc = QrRiskRules.evaluate(QrSamplePayloads.SCAM_KYC_SHORTENER)
        assertTrue(kyc.signals.any { it.id == "upi_url_param" })
        assertTrue(kyc.signals.any { it.id == "upi_url_shortener" })
        assertTrue(kyc.signals.any { it.id == "upi_url_insecure_http" })

        val ip = QrRiskRules.evaluate(QrSamplePayloads.SCAM_IP_OBFUSCATED)
        assertTrue(ip.signals.any { it.id == "ip_host" })
        assertTrue(ip.signals.any { it.id == "insecure_http" })
        assertFalse(UpiUriParser.parse(QrSamplePayloads.SCAM_IP_OBFUSCATED).isUpiPay)
    }
}
