package com.rench.kvartstone.data

import android.content.Context
import android.util.Log
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
import com.rench.kvartstone.data.repositories.CardRepository
import com.rench.kvartstone.data.repositories.DeckRepository
import com.rench.kvartstone.data.repositories.HeroPowerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
@Database(
    entities = [CardEntity::class, HeroPowerEntity::class, DeckEntity::class],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun heroPowerDao(): HeroPowerDao
    abstract fun deckDao(): DeckDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        @Volatile
        private var isInitialized = false
        private val initializationLock = Object()


        suspend fun waitForInitialization(timeoutSeconds: Int): Boolean {
            return withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                val timeoutMillis = timeoutSeconds * 1000L

                synchronized(initializationLock) {
                    while (!isInitialized) {
                        if (System.currentTimeMillis() - startTime > timeoutMillis) {
                            return@withContext false
                        }
                        try {
                            (initializationLock as Object).wait(100)
                        } catch (e: InterruptedException) {
                            Thread.currentThread().interrupt()
                            return@withContext false
                        }
                    }
                }
                true
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {

                val cursor = db.query("PRAGMA table_info(cards)")
                var columnExists = false

                while (cursor.moveToNext()) {
                    val columnName = cursor.getString(1)
                    if (columnName == "description") {
                        columnExists = true
                        break
                    }
                }
                cursor.close()

                if (!columnExists) {
                    db.execSQL("ALTER TABLE cards ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                    Log.d("AppDatabase", "Added description column to cards table")
                } else {
                    Log.d("AppDatabase", "Description column already exists, skipping migration")
                }
            }
        }


        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kvartstone_database"
                )
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Log.d("AppDatabase", "Database created for the first time, populating data.")

                INSTANCE?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            populateDatabaseWithInitialData(context)
                            synchronized(initializationLock) {
                                isInitialized = true
                                (initializationLock as Object).notifyAll()
                            }
                        } catch (e: Exception) {
                            Log.e("AppDatabase", "Failed to populate database", e)
                            synchronized(initializationLock) {
                                isInitialized = true
                                (initializationLock as Object).notifyAll()
                            }
                        }
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                synchronized(initializationLock) {
                    isInitialized = true
                    (initializationLock as Object).notifyAll()
                }
            }

            private suspend fun populateDatabaseWithInitialData(context: Context) {
                try {
                    Log.d("AppDatabase", "Starting initial data population...")

                    val heroPowerRepository = HeroPowerRepository(context)
                    val cardRepository = CardRepository(context)
                    val deckRepository = DeckRepository(context)

                    heroPowerRepository.initializeDefaultHeroPowers()
                    cardRepository.initializeDefaultCards()
                    deckRepository.initializeDefaultDecks()

                    Log.d("AppDatabase", "Initial data population completed successfully.")
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Failed to populate database with initial data", e)
                }
            }
        }
    }
}
