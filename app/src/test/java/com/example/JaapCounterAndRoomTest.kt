package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.BackupRepositoryImpl
import com.example.data.JaapRepositoryImpl
import com.example.database.DivyaJaapDatabase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JaapCounterAndRoomTest {

    private lateinit var db: DivyaJaapDatabase
    private lateinit var jaapRepo: JaapRepositoryImpl
    private lateinit var backupRepo: BackupRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DivyaJaapDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        jaapRepo = JaapRepositoryImpl(db)
        backupRepo = BackupRepositoryImpl(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `108 taps completes one Mala, saves MalaProgress, and prepares next Mala without losing counts`() = runTest {
        db.ensureSeeded()
        jaapRepo.updateMalaTarget(108)

        repeat(107) {
            val res = jaapRepo.recordTap(2)
            assertFalse(res.malaJustCompleted)
        }

        val completingTap = jaapRepo.recordTap(2)
        assertTrue(completingTap.malaJustCompleted)
        assertEquals(0, completingTap.currentCount)
        assertEquals(1, completingTap.completedMalasInSession)

        assertEquals(108, jaapRepo.observeTodayJaapCount().first())
        assertEquals(108, jaapRepo.observeLifetimeJaapCount().first())
        assertEquals(1, jaapRepo.observeTotalMalasCompleted().first())
        assertEquals(1, jaapRepo.getAllMalaProgress().size)
        // Unique constraint guarantees only 1 JaapHistory row per date + mantra
        assertEquals(1, jaapRepo.getAllHistory().size)
    }

    @Test
    fun `undo and reset preserve previously completed Malas and counts`() = runTest {
        db.ensureSeeded()
        jaapRepo.updateMalaTarget(27)

        // Complete 1 Mala (27 taps) + 3 taps in second Mala
        repeat(30) { jaapRepo.recordTap(1) }
        assertEquals(1, jaapRepo.observeTotalMalasCompleted().first())
        assertEquals(30, jaapRepo.observeTodayJaapCount().first())
        assertEquals(3, jaapRepo.observeActiveSession().first()?.currentCount)

        // Undo 1 tap -> currentCount becomes 2, today becomes 29
        jaapRepo.undoLastTap()
        assertEquals(2, jaapRepo.observeActiveSession().first()?.currentCount)
        assertEquals(29, jaapRepo.observeTodayJaapCount().first())

        // Reset current Mala -> currentCount becomes 0, completed Mala remains 1
        jaapRepo.resetCurrentMala()
        assertEquals(0, jaapRepo.observeActiveSession().first()?.currentCount)
        assertEquals(1, jaapRepo.observeTotalMalasCompleted().first())
    }

    @Test
    fun `concurrent rapid taps never duplicate or drop counts`() = runTest {
        db.ensureSeeded()
        jaapRepo.updateMalaTarget(108)

        val jobs = (1..50).map {
            async { jaapRepo.recordTap(1) }
        }
        jobs.awaitAll()

        assertEquals(50, jaapRepo.observeTodayJaapCount().first())
        assertEquals(50, jaapRepo.observeActiveSession().first()?.currentCount)
    }

    @Test
    fun `backup export and restore preserves history and sessions`() = runTest {
        db.ensureSeeded()
        repeat(5) { jaapRepo.recordTap(1) }
        val json = backupRepo.exportBackupJson()

        db.jaapDao().clearJaapHistory()
        db.jaapDao().clearSessions()
        assertEquals(0, jaapRepo.observeLifetimeJaapCount().first())

        val restored = backupRepo.restoreBackupJson(json)
        assertTrue(restored.isSuccess)
        assertEquals(5, jaapRepo.observeLifetimeJaapCount().first())
    }
}
