package com.farmsaathi.core.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LandMeasurementMigrationTest {
    @Test
    fun `version 3 to 4 migration creates measurement tables indexes and foreign keys`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name("land_measurement_migration_test.db")
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(3) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                db.execSQL("CREATE TABLE farms (`id` INTEGER PRIMARY KEY NOT NULL)")
                                db.execSQL(
                                    "CREATE TABLE plots (`id` INTEGER PRIMARY KEY NOT NULL, `farmId` INTEGER NOT NULL, " +
                                        "`areaMicroAcres` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL)",
                                )
                            }

                            override fun onUpgrade(
                                db: SupportSQLiteDatabase,
                                oldVersion: Int,
                                newVersion: Int,
                            ) = Unit
                        },
                    )
                    .build(),
            )

        try {
            val db = helper.writableDatabase
            db.execSQL("PRAGMA foreign_keys = ON")
            FarmSaathiDatabase.MIGRATION_3_4.migrate(db)

            assertTrue(tableExists(db, "land_measurements"))
            assertTrue(tableExists(db, "land_boundary_points"))
            assertTrue(indexExists(db, "index_land_measurements_plotId"))
            assertTrue(indexExists(db, "index_land_boundary_points_measurementId_sequenceNumber"))

            db.execSQL("INSERT INTO farms (`id`) VALUES (1)")
            db.execSQL("INSERT INTO plots (`id`, `farmId`, `areaMicroAcres`, `updatedAtEpochMillis`) VALUES (10, 1, 1000000, 1)")
            db.execSQL(
                "INSERT INTO land_measurements (`farmId`, `plotId`, `method`, `areaMicroAcres`, " +
                    "`perimeterMeters`, `startedAtEpochMillis`, " +
                    "`completedAtEpochMillis`, `appliedToPlot`, `notes`) VALUES (1, 10, 'GPS_BOUNDARY', 2100000, " +
                    "380, 100, 200, 0, NULL)",
            )
            db.execSQL(
                "INSERT INTO land_boundary_points (`measurementId`, `sequenceNumber`, `latitude`, `longitude`, " +
                    "`horizontalAccuracyMeters`, `capturedAtEpochMillis`) VALUES (1, 0, 17.0, 78.0, 4.0, 100)",
            )

            db.query("SELECT COUNT(*) FROM land_boundary_points WHERE measurementId = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            db.query("PRAGMA foreign_key_check").use { cursor -> assertEquals(false, cursor.moveToFirst()) }

            var measurementDeleteWasRestricted = false
            try {
                db.execSQL("DELETE FROM land_measurements WHERE id = 1")
            } catch (_: SQLiteConstraintException) {
                measurementDeleteWasRestricted = true
            }
            assertTrue("A measurement with boundary history must not be deleted", measurementDeleteWasRestricted)
        } finally {
            helper.close()
            context.deleteDatabase("land_measurement_migration_test.db")
        }
    }

    private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean =
        db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(tableName)).use { it.moveToFirst() }

    private fun indexExists(db: SupportSQLiteDatabase, indexName: String): Boolean =
        db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name = ?", arrayOf(indexName)).use { it.moveToFirst() }
}
