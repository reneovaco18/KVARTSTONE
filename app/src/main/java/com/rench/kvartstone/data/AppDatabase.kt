package com.rench.kvartstone.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rench.kvartstone.data.dao.CardDao
import com.rench.kvartstone.data.dao.DeckDao
import com.rench.kvartstone.data.dao.HeroPowerDao
import com.rench.kvartstone.data.entities.CardEntity
import com.rench.kvartstone.data.entities.DeckEntity
import com.rench.kvartstone.data.entities.HeroPowerEntity

@Database(
    entities = [CardEntity::class, HeroPowerEntity::class, DeckEntity::class],
    version = 3, // Increment version
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun heroPowerDao(): HeroPowerDao
    abstract fun deckDao(): DeckDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kvartstone_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration() // For development only
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create hero_powers table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS hero_powers (
                        id INTEGER PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        manaCost INTEGER NOT NULL,
                        imageResName TEXT NOT NULL,
                        effectType TEXT NOT NULL,
                        effectValue INTEGER NOT NULL,
                        targetType TEXT NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1
                    )
                """)

                // Create decks table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS decks (
                        id INTEGER PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        heroClass TEXT NOT NULL,
                        cardIds TEXT NOT NULL,
                        isCustom INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add missing columns to cards table
                val columnsToAdd = listOf(
                    "description TEXT DEFAULT ''",
                    "rarity TEXT DEFAULT 'common'",
                    "imageUri TEXT DEFAULT NULL",
                    "keywords TEXT DEFAULT NULL",
                    "heroClass TEXT DEFAULT 'neutral'",
                    "isCustom INTEGER DEFAULT 0",
                    "createdAt INTEGER DEFAULT 0"
                )

                columnsToAdd.forEach { columnDef ->
                    try {
                        database.execSQL("ALTER TABLE cards ADD COLUMN $columnDef")
                    } catch (e: Exception) {
                        // Column might already exist, continue
                    }
                }
            }
        }
    }
}
