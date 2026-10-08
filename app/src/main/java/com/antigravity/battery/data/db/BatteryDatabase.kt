package com.antigravity.battery.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SessionEntity::class,
        PeriodicSnapshotEntity::class,
        DailyRollupEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BatteryDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun continuousSnapshotDao(): ContinuousSnapshotDao

    companion object {
        @Volatile
        private var INSTANCE: BatteryDatabase? = null

        fun getInstance(context: Context): BatteryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BatteryDatabase::class.java,
                    "battery_diagnostics.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
