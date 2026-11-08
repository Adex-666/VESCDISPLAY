package pl.replit.jazdanazywo

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Lokalny, trwały zapis SQLite. Nie otwiera BLE i nie wymaga chmury. */
class RideRepository(context: Context) {
    private val helper = RideDatabase(context.applicationContext)

    suspend fun createRide(startedAtMs: Long): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply { put("started_at_ms", startedAtMs) }
        helper.writableDatabase.insertOrThrow("rides", null, values)
    }

    suspend fun finishRide(rideId: Long, endedAtMs: Long) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply { put("ended_at_ms", endedAtMs) }
        helper.writableDatabase.update("rides", values, "id = ?", arrayOf(rideId.toString()))
        Unit
    }

    suspend fun finishOpenRides(endedAtMs: Long) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply { put("ended_at_ms", endedAtMs) }
        helper.writableDatabase.update("rides", values, "ended_at_ms IS NULL", null)
        Unit
    }

    suspend fun addSample(sample: RideSample) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("ride_id", sample.rideId)
            put("timestamp_ms", sample.timestampMs)
            putNullable("speed_kmh", sample.speedKmh)
            putNullable("battery_percent", sample.batteryPercent)
            putNullable("consumption_wh_km", sample.consumptionWhPerKm)
            put("profile", sample.profile)
        }
        helper.writableDatabase.insertOrThrow("ride_samples", null, values)
        Unit
    }

    suspend fun loadSnapshot(): RideSnapshot = withContext(Dispatchers.IO) {
        val db = helper.readableDatabase
        val sessions = db.query("rides", arrayOf("id", "started_at_ms", "ended_at_ms"), null, null, null, null, "started_at_ms DESC").use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(RideSession(cursor.getLong(0), cursor.getLong(1), cursor.longOrNull(2)))
            }
        }
        val samples = db.query(
            "ride_samples", arrayOf("ride_id", "timestamp_ms", "speed_kmh", "battery_percent", "consumption_wh_km", "profile"),
            null, null, null, null, "timestamp_ms ASC, id ASC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(RideSample(cursor.getLong(0), cursor.getLong(1), cursor.doubleOrNull(2), cursor.doubleOrNull(3), cursor.doubleOrNull(4), cursor.getInt(5)))
                }
            }
        }
        RideSnapshot(sessions, samples)
    }

    suspend fun loadBatteryCapacityWh(): Double? = withContext(Dispatchers.IO) {
        helper.readableDatabase.query("settings", arrayOf("value"), "key = ?", arrayOf(CAPACITY_KEY), null, null, null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0).toDoubleOrNull()?.takeIf { it > 0.0 } else null
        }
    }

    suspend fun saveBatteryCapacityWh(value: Double?) = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        if (value == null || !value.isFinite() || value <= 0.0) {
            db.delete("settings", "key = ?", arrayOf(CAPACITY_KEY))
        } else {
            val values = ContentValues().apply { put("key", CAPACITY_KEY); put("value", value.toString()) }
            db.insertWithOnConflict("settings", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
        Unit
    }

    private class RideDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE rides (id INTEGER PRIMARY KEY AUTOINCREMENT, started_at_ms INTEGER NOT NULL, ended_at_ms INTEGER)")
            db.execSQL("CREATE TABLE ride_samples (id INTEGER PRIMARY KEY AUTOINCREMENT, ride_id INTEGER NOT NULL, timestamp_ms INTEGER NOT NULL, speed_kmh REAL, battery_percent REAL, consumption_wh_km REAL, profile INTEGER NOT NULL CHECK(profile BETWEEN 1 AND 5), FOREIGN KEY(ride_id) REFERENCES rides(id) ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX ride_samples_time_idx ON ride_samples(timestamp_ms)")
            db.execSQL("CREATE INDEX ride_samples_ride_idx ON ride_samples(ride_id, timestamp_ms)")
            db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        private companion object {
            const val DATABASE_NAME = "jazda_na_zywo.db"
            const val DATABASE_VERSION = 1
        }
    }

    private companion object { const val CAPACITY_KEY = "usable_battery_capacity_wh" }
}

private fun ContentValues.putNullable(key: String, value: Double?) {
    if (value == null || !value.isFinite()) putNull(key) else put(key, value)
}
private fun Cursor.doubleOrNull(index: Int): Double? = if (isNull(index)) null else getDouble(index)
private fun Cursor.longOrNull(index: Int): Long? = if (isNull(index)) null else getLong(index)
