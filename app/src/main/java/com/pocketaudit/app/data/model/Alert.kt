package com.pocketaudit.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RiskLevel {
    HIGH,
    MEDIUM,
    LOW,
    SAFE
}

enum class ScamType {
    FAKE_UPI_COLLECT,
    PHISHING_LINK,
    SUBSCRIPTION_TRAP,
    UNKNOWN_SUSPICIOUS,
    LEGITIMATE
}

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val body: String,
    val riskLevel: RiskLevel,
    val riskScore: Int, // 0 - 100
    val scamType: ScamType,
    val matchedPatterns: List<String>,
    val explanation: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isSafe: Boolean = false
)
