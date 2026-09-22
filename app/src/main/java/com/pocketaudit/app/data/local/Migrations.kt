package com.pocketaudit.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE alerts ADD COLUMN source TEXT NOT NULL DEFAULT 'NOTIFICATION'"
        )
        db.execSQL(
            "ALTER TABLE alerts ADD COLUMN decidedBy TEXT NOT NULL DEFAULT 'RULES'"
        )
    }
}
