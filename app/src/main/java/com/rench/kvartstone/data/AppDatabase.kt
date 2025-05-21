package com.rench.kvartstone.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rench.kvartstone.data.dao.CardDao
import com.rench.kvartstone.data.entities.CardEntity

@Database(entities = [CardEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kvartstone_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}