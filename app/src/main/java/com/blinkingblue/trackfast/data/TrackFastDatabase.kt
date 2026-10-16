package com.blinkingblue.trackfast.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FastSessionEntity::class, WeightEntryEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class TrackFastDatabase : RoomDatabase() {
    abstract fun fastSessionDao(): FastSessionDao
    abstract fun weightEntryDao(): WeightEntryDao
}
