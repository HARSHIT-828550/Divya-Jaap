package com.example.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.presentation.counter.JaapCounterViewModel
import com.example.presentation.dashboard.DashboardViewModel
import com.example.presentation.guru.MantraGuruViewModel
import com.example.presentation.library.MantraLibraryViewModel
import com.example.presentation.practice.PronunciationPracticeViewModel
import com.example.presentation.settings.SettingsViewModel

class DivyaJaapViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(JaapCounterViewModel::class.java) -> JaapCounterViewModel(
                jaapRepository = container.jaapRepository,
                mantraRepository = container.mantraRepository,
                settingsRepository = container.settingsRepository,
                settingsDataStore = container.settingsDataStore,
                templeBellPlayer = container.templeBellPlayer
            ) as T

            modelClass.isAssignableFrom(MantraGuruViewModel::class.java) -> MantraGuruViewModel(
                guruEngine = container.mantraGuruEngine,
                mantraRepository = container.mantraRepository,
                settingsRepository = container.settingsRepository,
                settingsDataStore = container.settingsDataStore
            ) as T

            modelClass.isAssignableFrom(PronunciationPracticeViewModel::class.java) -> PronunciationPracticeViewModel(
                guruEngine = container.mantraGuruEngine,
                speechRecognitionManager = container.speechRecognitionManager,
                mantraRepository = container.mantraRepository,
                practiceRepository = container.practiceRepository
            ) as T

            modelClass.isAssignableFrom(MantraLibraryViewModel::class.java) -> MantraLibraryViewModel(
                mantraRepository = container.mantraRepository,
                jaapRepository = container.jaapRepository
            ) as T

            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(
                jaapRepository = container.jaapRepository,
                settingsRepository = container.settingsRepository
            ) as T

            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(
                settingsRepository = container.settingsRepository,
                backupRepository = container.backupRepository,
                reminderScheduler = container.reminderScheduler
            ) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
