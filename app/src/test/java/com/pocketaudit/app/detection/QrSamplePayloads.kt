package com.pocketaudit.app.detection

object QrSamplePayloads {
    const val SAFE_MERCHANT_TEA =
        "upi://pay?pa=ravi.kumar@oksbi&pn=Ravi%20Kumar&am=120.00&cu=INR&tn=Tea"

    const val SAFE_MERCHANT_NO_AMOUNT =
        "upi://pay?pa=suresh.stores@ybl&pn=Suresh%20Stores&cu=INR"

    const val SAFE_HTTPS_INFO =
        "https://www.npci.org.in/what-we-do/upi/product-overview"

    const val SCAM_LOTTERY_HIGH_AMOUNT =
        "upi://pay?pa=xqzvnmwptrkb@ybl&pn=Amazon%20Pay&am=75000&cu=INR&tn=Claim%20lottery%20prize%20urgent"

    const val SCAM_KYC_SHORTENER =
        "upi://pay?pa=refundnow@paytm&pn=Refund%20Desk&am=1&tn=KYC%20update&url=http://bit.ly/upi-kyc"

    const val SCAM_IP_OBFUSCATED =
        "http://185.22.11.9/login?next=%2Fpay%2F%2F%2F%2F%2F%2F%2F%2F%2F%2Fverify%2Fkyc%2F" +
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
}
