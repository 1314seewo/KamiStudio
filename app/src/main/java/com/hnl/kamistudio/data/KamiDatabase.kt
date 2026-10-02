package com.hnl.kamistudio.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [KamiEntity::class], version = 1, exportSchema = false)
abstract class KamiDatabase : RoomDatabase() {
    abstract fun kamiDao(): KamiDao

    companion object {
        @Volatile
        private var INSTANCE: KamiDatabase? = null

        fun getDatabase(context: Context): KamiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KamiDatabase::class.java,
                    "kami_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
