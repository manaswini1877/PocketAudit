package com.pocketaudit.app.service

import android.content.Context
import android.provider.Telephony

object NotificationFilter {

    // Verified Android Package Names for Indian Financial Ecosystem
    private val UPI_APP_PACKAGES = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",                       // PhonePe
        "net.one97.paytm",                       // Paytm
        "in.org.npci.upiapp",                    // BHIM UPI
        "com.dreamplug.androidapp",              // CRED
        "in.amazon.mShop.android.shopping",      // Amazon Pay (India)
        "com.amazon.mShop.android.shopping"       // Amazon Pay (Global/Shopping)
    )

    private val BANKING_APP_PACKAGES = setOf(
        "com.sbi.lotusintouch",                  // YONO SBI
        "com.sbi.upi",                           // SBI Pay
        "com.snapwork.hdfc",                     // HDFC Bank MobileBanking
        "com.csam.icici.bank.imobile",           // ICICI iMobile Pay
        "com.axis.mobile",                       // Axis Mobile
        "com.msf.kbank.mobile",                  // Kotak Mobile Banking
        "com.pnb.pnbone",                        // PNB ONE
        "com.bankofbaroda.mconnect",             // BOB World
        "com.canarabank.mob",                    // Canara ai1
        "com.infrasofttech.uboi"                 // Union Bank of India
    )

    private val MESSAGING_APP_PACKAGES = setOf(
        "com.google.android.apps.messaging",     // Google Messages
        "com.samsung.android.messaging",         // Samsung Messages
        "com.android.mms",                       // Default Android MMS
        "com.miui.smsextra",                      // Xiaomi MIUI SMS
        "com.coloros.select",                    // Oppo/Realme ColorOS SMS
        "com.oppo.mms",                          // Oppo MMS
        "com.realme.mms",                        // Realme MMS
        "com.heytap.smsextra",                   // HeyTap SMS (Oppo/Realme)
        "com.vivo.mms"                           // Vivo MMS
    )

    private val EXCLUDED_CHAT_PACKAGES = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "com.instagram.android",
        "com.facebook.katana",
        "com.facebook.orca",
        "com.snapchat.android",
        "com.twitter.android",
        "com.linkedin.android",
        "com.slack"
    )

    private val FINANCIAL_KEYWORDS = listOf(
        "upi", "bank", "account", "a/c", "debited", "credited",
        "collect request", "refund", "cashback", "paytm", "gpay", "phonepe",
        "kyc", "otp", "autopay", "mandate", "subscription", "rs.", "inr", "₹",
        "paid", "payment", "sent", "received", "transfer", "transferred", "charge",
        "charged", "card", "spent", "amount", "vpa", "utr", "ref", "bill", "due",
        "balance", "withdrawn", "atm", "code", "verify", "verification", "link",
        "click", "claim", "reward", "win", "winner", "lottery", "urgent", "alert",
        "notice", "expire", "expired"
    )

    fun isTargetNotification(packageName: String, title: String?, text: String?, context: Context? = null): Boolean {
        if (packageName.isBlank()) {
            logDebug("NotificationFilter", "⛔ Filtered OUT: Blank packageName")
            return false
        }

        // Exclude social chat apps to avoid false positives on general messaging
        if (EXCLUDED_CHAT_PACKAGES.contains(packageName)) {
            logDebug("NotificationFilter", "⛔ Filtered OUT: Excluded social/chat app '$packageName'")
            return false
        }

        // Check if package is in explicit whitelist
        if (UPI_APP_PACKAGES.contains(packageName) || BANKING_APP_PACKAGES.contains(packageName)) {
            logDebug("NotificationFilter", "✅ Filter PASSED: Explicit Banking/UPI app '$packageName'")
            return true
        }

        // Check SMS & MMS messaging apps - analyze all SMS from recognized messaging apps
        if (MESSAGING_APP_PACKAGES.contains(packageName)) {
            val hasKeywords = containsFinancialKeywords(title, text)
            logDebug("NotificationFilter", "✅ Filter PASSED: Messaging app '$packageName' (matchedKeywords=$hasKeywords) -> forwarding to DetectionEngine")
            return true
        }

        // Check default system SMS app if context is provided
        if (context != null) {
            try {
                val defaultSmsPkg = Telephony.Sms.getDefaultSmsPackage(context)
                if (defaultSmsPkg != null && defaultSmsPkg == packageName) {
                    val hasKeywords = containsFinancialKeywords(title, text)
                    logDebug("NotificationFilter", "✅ Filter PASSED: Default SMS app '$packageName' (matchedKeywords=$hasKeywords) -> forwarding to DetectionEngine")
                    return true
                }
            } catch (e: Exception) {
                // Ignore telephony exceptions
            }
        }

        // Fallback for custom launcher/OEM SMS apps containing banking signals
        val isTargetPkg = packageName.contains("message", ignoreCase = true) ||
                packageName.contains("sms", ignoreCase = true) ||
                packageName.contains("mms", ignoreCase = true) ||
                packageName.contains("bank", ignoreCase = true) ||
                packageName.contains("upi", ignoreCase = true) ||
                packageName.contains("pay", ignoreCase = true)

        val passes = isTargetPkg && containsFinancialKeywords(title, text)
        if (passes) {
            logDebug("NotificationFilter", "✅ Filter PASSED: OEM SMS/Banking package '$packageName'")
        } else {
            logDebug("NotificationFilter", "⛔ Filtered OUT: Non-target package '$packageName'")
        }
        return passes
    }

    private fun containsFinancialKeywords(title: String?, text: String?): Boolean {
        val combined = "${title.orEmpty()} ${text.orEmpty()}".lowercase()
        return FINANCIAL_KEYWORDS.any { keyword -> combined.contains(keyword) }
    }

    private fun logDebug(tag: String, msg: String) {
        try {
            android.util.Log.d(tag, msg)
        } catch (t: Throwable) {
            println("[$tag] $msg")
        }
    }

    fun getAppNameForPackage(packageName: String): String {
        return when {
            packageName == "com.google.android.apps.nbu.paisa.user" -> "Google Pay"
            packageName == "com.phonepe.app" -> "PhonePe"
            packageName == "net.one97.paytm" -> "Paytm"
            packageName == "in.org.npci.upiapp" -> "BHIM UPI"
            packageName == "com.dreamplug.androidapp" -> "CRED"
            packageName.contains("amazon") -> "Amazon Pay"
            packageName.contains("sbi") -> "YONO SBI"
            packageName.contains("hdfc") -> "HDFC Bank"
            packageName.contains("icici") -> "ICICI Bank"
            packageName.contains("axis") -> "Axis Bank"
            packageName.contains("messaging") || packageName.contains("sms") || packageName.contains("mms") || packageName.contains("heytap") || packageName.contains("coloros") -> "SMS / Messages"
            else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }
}
