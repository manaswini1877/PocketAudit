package com.pocketaudit.app.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentExtractorTest {

    @Test
    fun extractUrls_findsHttpAndHttps() {
        val text = "Verify at http://bit.ly/scam and https://safe.example.com/path"
        val urls = ContentExtractor.extractUrls(text)
        assertEquals(2, urls.size)
        assertTrue(urls.any { it.startsWith("http://bit.ly") })
        assertTrue(urls.any { it.contains("safe.example.com") })
    }

    @Test
    fun extractUrls_normalizesWwwPrefix() {
        val urls = ContentExtractor.extractUrls("Visit www.example.com now")
        assertEquals(1, urls.size)
        assertEquals("https://www.example.com", urls[0])
    }

    @Test
    fun extractVpas_findsHandles() {
        val text = "Pay cashback-claim@ybl or send to merchant@okaxis"
        val vpas = ContentExtractor.extractVpas(text)
        assertTrue(vpas.contains("cashback-claim@ybl"))
        assertTrue(vpas.contains("merchant@okaxis"))
    }

    @Test
    fun extractVpas_ignoresEmailDomains() {
        val vpas = ContentExtractor.extractVpas("Contact me at user@gmail.com")
        assertTrue(vpas.isEmpty())
    }

    @Test
    fun extractVpas_findsLabeledUpiId() {
        val vpas = ContentExtractor.extractVpas("Your UPI ID: shop123@paytm")
        assertTrue(vpas.contains("shop123@paytm"))
    }

    @Test
    fun textForAnalysis_appendsExtractedMarkers() {
        val enriched = ContentExtractor.textForAnalysis("Pay shop@ybl http://x.co/abc")
        assertTrue(enriched.contains("VPA: shop@ybl"))
        assertTrue(enriched.contains("URL:"))
    }
}
