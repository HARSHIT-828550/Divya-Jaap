package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        MantraCategory::class,
        Mantra::class,
        FavoriteMantra::class,
        JaapSession::class,
        MalaProgress::class,
        JaapHistory::class,
        UserSettings::class,
        PracticeHistory::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DivyaJaapDatabase : RoomDatabase() {
    abstract fun mantraDao(): MantraDao
    abstract fun jaapDao(): JaapDao
    abstract fun practiceDao(): PracticeDao
    abstract fun settingsDao(): SettingsDao

    suspend fun ensureSeeded() {
        val mDao = mantraDao()
        mDao.insertCategories(MantraSeedData.categories)
        mDao.insertMantrasIgnore(MantraSeedData.mantras)

        val sDao = settingsDao()
        val settings = sDao.getSettings()
        if (settings == null) {
            sDao.upsertSettings(UserSettings())
        }

        val jDao = jaapDao()
        if (jDao.getActiveSession() == null) {
            val activeMantraId = sDao.getSettings()?.activeMantraId ?: MantraSeedData.mantras.first().id
            val target = sDao.getSettings()?.defaultMalaTarget ?: 108
            jDao.insertSession(
                JaapSession(
                    mantraId = activeMantraId,
                    targetCount = target,
                    currentCount = 0,
                    completedMalas = 0,
                    isActive = true
                )
            )
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: DivyaJaapDatabase? = null

        fun getInstance(context: Context): DivyaJaapDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): DivyaJaapDatabase {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            return Room.databaseBuilder(
                appContext,
                DivyaJaapDatabase::class.java,
                "divya_jaap.db"
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        scope.launch {
                            INSTANCE?.ensureSeeded()
                        }
                    }
                })
                .build()
        }
    }
}
