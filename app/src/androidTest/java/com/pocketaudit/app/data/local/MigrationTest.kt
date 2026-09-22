package com.pocketaudit.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2_preservesExistingRowsAndDefaults() {
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                """
                INSERT INTO alerts (
                    packageName, appName, title, body, riskLevel, riskScore, scamType,
                    matchedPatterns, explanation, timestamp, isRead, isSafe
                ) VALUES (
                    'com.phonepe.app', 'PhonePe', 'Collect', 'Pay 100', 'HIGH', 80, 'FAKE_UPI_COLLECT',
                    'rule1', 'Suspicious collect', 1000, 0, 0
                )
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(testDb, 2, true, MIGRATION_1_2).apply {
            query("SELECT source, decidedBy, title FROM alerts").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("NOTIFICATION", cursor.getString(0))
                assertEquals("RULES", cursor.getString(1))
                assertEquals("Collect", cursor.getString(2))
            }
            close()
        }
    }
}
