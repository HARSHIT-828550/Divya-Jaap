package com.example.core

import android.content.Context
import com.example.ai.MantraGuruEngine
import com.example.audio.MantraBackgroundAudioClient
import com.example.data.BackupRepositoryImpl
import com.example.data.CloudTtsApiService
import com.example.data.CloudTtsRepository
import com.example.data.JaapRepositoryImpl
import com.example.data.MantraRepositoryImpl
import com.example.data.PracticeRepositoryImpl
import com.example.data.SettingsDataStore
import com.example.data.SettingsRepositoryImpl
import com.example.database.DivyaJaapDatabase
import com.example.domain.BackupRepository
import com.example.domain.JaapRepository
import com.example.domain.MantraRepository
import com.example.domain.PracticeRepository
import com.example.domain.SettingsRepository
import com.example.settings.ReminderScheduler
import com.example.speech.SpeechRecognitionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: DivyaJaapDatabase = DivyaJaapDatabase.getInstance(appContext)
    val settingsDataStore: SettingsDataStore = SettingsDataStore(appContext)

    val mantraRepository: MantraRepository = MantraRepositoryImpl(database)
    val jaapRepository: JaapRepository = JaapRepositoryImpl(database)
    val practiceRepository: PracticeRepository = PracticeRepositoryImpl(database)
    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(database)
    val backupRepository: BackupRepository = BackupRepositoryImpl(database)

    val templeBellPlayer: TempleBellPlayer = TempleBellPlayer()
    val reminderScheduler: ReminderScheduler = ReminderScheduler(appContext)

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://localhost/") // Overridden per request via @Url from user Settings
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val cloudTtsApi: CloudTtsApiService = retrofit.create(CloudTtsApiService::class.java)
    val cloudTtsRepository: CloudTtsRepository = CloudTtsRepository(appContext, cloudTtsApi)

    val backgroundAudioClient: MantraBackgroundAudioClient = MantraBackgroundAudioClient(appContext)
    val mantraGuruEngine: MantraGuruEngine = MantraGuruEngine(
        context = appContext,
        cloudTtsRepository = cloudTtsRepository,
        backgroundAudioClient = backgroundAudioClient
    )
    val speechRecognitionManager: SpeechRecognitionManager = SpeechRecognitionManager(appContext)

    init {
        initScope.launch {
            database.ensureSeeded()
        }
    }
}
