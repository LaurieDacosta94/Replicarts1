package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocalModEntity::class,
        LocalModpackEntity::class,
        SavedSkirmishPresetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CrownDominionDatabase : RoomDatabase() {
    abstract fun dao(): CrownDominionDao

    companion object {
        @Volatile
        private var INSTANCE: CrownDominionDatabase? = null

        fun getInstance(context: Context): CrownDominionDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CrownDominionDatabase::class.java,
                    "crown_dominion_rts.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
