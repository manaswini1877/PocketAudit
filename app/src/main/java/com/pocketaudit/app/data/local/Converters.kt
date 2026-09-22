package com.pocketaudit.app.data.local

import androidx.room.TypeConverter
import com.pocketaudit.app.data.model.RiskLevel
import com.pocketaudit.app.data.model.ScamType
import com.pocketaudit.app.detection.DecidedBy
import com.pocketaudit.app.detection.RiskSource

class Converters {

    @TypeConverter
    fun fromRiskLevel(level: RiskLevel): String = level.name

    @TypeConverter
    fun toRiskLevel(value: String): RiskLevel = try {
        RiskLevel.valueOf(value)
    } catch (e: Exception) {
        RiskLevel.SAFE
    }

    @TypeConverter
    fun fromScamType(type: ScamType): String = type.name

    @TypeConverter
    fun toScamType(value: String): ScamType = try {
        ScamType.valueOf(value)
    } catch (e: Exception) {
        ScamType.UNKNOWN_SUSPICIOUS
    }

    @TypeConverter
    fun fromRiskSource(source: RiskSource): String = source.name

    @TypeConverter
    fun toRiskSource(value: String): RiskSource = try {
        RiskSource.valueOf(value)
    } catch (e: Exception) {
        RiskSource.NOTIFICATION
    }

    @TypeConverter
    fun fromDecidedBy(decidedBy: DecidedBy): String = decidedBy.name

    @TypeConverter
    fun toDecidedBy(value: String): DecidedBy = try {
        DecidedBy.valueOf(value)
    } catch (e: Exception) {
        DecidedBy.RULES
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString(";;;")

    @TypeConverter
    fun toStringList(value: String): List<String> = if (value.isBlank()) {
        emptyList()
    } else {
        value.split(";;;")
    }
}
