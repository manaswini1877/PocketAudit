package com.pocketaudit.app.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiUriParserTest {

    @Test
    fun parse_standardUpiPay() {
        val parsed = UpiUriParser.parse(
            "upi://pay?pa=ravi.kumar@oksbi&pn=Ravi Kumar&am=120.50&cu=INR&tn=Tea&mc=5411&tr=TXN1"
        )
        assertTrue(parsed.isUpiPay)
        assertEquals("ravi.kumar@oksbi", parsed.pa)
        assertEquals("Ravi Kumar", parsed.pn)
        assertEquals("120.50", parsed.am)
        assertEquals("INR", parsed.cu)
        assertEquals("Tea", parsed.tn)
        assertEquals("5411", parsed.mc)
        assertEquals("TXN1", parsed.tr)
        assertEquals(120.50, parsed.amountOrNull()!!, 0.001)
    }

    @Test
    fun parse_decodesUrlEncoding() {
        val parsed = UpiUriParser.parse("upi://pay?pa=shop@ybl&pn=Suresh%20Stores&tn=Order%2312")
        assertEquals("Suresh Stores", parsed.pn)
        assertEquals("Order#12", parsed.tn)
    }

    @Test
    fun parse_handlesPlusAndPercent20InPayeeName() {
        val parsedPlus = UpiUriParser.parse("upi://pay?pa=ravi@oksbi&pn=Ravi+Kumar")
        val parsedPercent = UpiUriParser.parse("upi://pay?pa=ravi@oksbi&pn=Ravi%20Kumar")
        assertEquals("Ravi Kumar", parsedPlus.pn)
        assertEquals("Ravi Kumar", parsedPercent.pn)
    }

    @Test
    fun parse_acceptsUppercaseKeysAndScheme() {
        val parsed = UpiUriParser.parse("UPI://PAY?PA=merchant@okaxis&PN=Merchant&AM=10&CU=INR")
        assertTrue(parsed.isUpiPay)
        assertEquals("merchant@okaxis", parsed.pa)
        assertEquals("Merchant", parsed.pn)
        assertEquals("10", parsed.am)
    }

    @Test
    fun parse_missingFieldsStayNull() {
        val parsed = UpiUriParser.parse("upi://pay?pa=onlyvpa@ybl")
        assertTrue(parsed.isUpiPay)
        assertEquals("onlyvpa@ybl", parsed.pa)
        assertNull(parsed.pn)
        assertNull(parsed.am)
        assertNull(parsed.tn)
        assertNull(parsed.url)
        assertNull(parsed.amountOrNull())
    }

    @Test
    fun parse_keepsFirstDuplicateKey() {
        val parsed = UpiUriParser.parse("upi://pay?pa=first@ybl&pa=second@ybl&pn=One&pn=Two")
        assertEquals("first@ybl", parsed.pa)
        assertEquals("One", parsed.pn)
    }

    @Test
    fun parse_readsUrlParam() {
        val parsed = UpiUriParser.parse("upi://pay?pa=x@ybl&url=https%3A%2F%2Fexample.com%2Fpay")
        assertEquals("https://example.com/pay", parsed.url)
    }

    @Test
    fun parse_garbageIsNotUpiPay() {
        val parsed = UpiUriParser.parse("definitely not a qr")
        assertFalse(parsed.isUpiPay)
        assertNull(parsed.pa)
    }

    @Test
    fun parse_emptyInput() {
        val parsed = UpiUriParser.parse("   ")
        assertFalse(parsed.isUpiPay)
    }

    @Test
    fun parse_httpUrlIsNotUpi() {
        val parsed = UpiUriParser.parse("https://bit.ly/pay-now")
        assertFalse(parsed.isUpiPay)
    }

    @Test
    fun amountOrNull_stripsCommas() {
        val parsed = UpiUriParser.parse("upi://pay?am=50,000.00")
        assertEquals(50000.00, parsed.amountOrNull()!!, 0.001)
    }
}
