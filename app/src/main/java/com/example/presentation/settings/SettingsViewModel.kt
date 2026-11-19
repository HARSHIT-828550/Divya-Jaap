package com.example.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.UserSettings
import com.example.domain.BackupRepository
import com.example.domain.SettingsRepository
import com.example.settings.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val statusMessage: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val statusMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observeSettings(),
        statusMessage
    ) { settings, status ->
        SettingsUiState(settings = settings, statusMessage = status)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun clearStatusMessage() {
        statusMessage.value = null
    }

    fun setDailyJaapGoal(goal: Int) {
        val safe = goal.coerceIn(1, 10800)
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(dailyJaapGoal = safe) }
        }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(hapticEnabled = enabled) }
        }
    }

    fun setTempleBellEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(templeBellEnabled = enabled) }
        }
    }

    fun setScreenAlwaysOn(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(screenAlwaysOn = enabled) }
        }
    }

    fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(
                    reminderEnabled = enabled,
                    reminderHour = hour.coerceIn(0, 23),
                    reminderMinute = minute.coerceIn(0, 59)
                )
            }
            if (enabled) {
                reminderScheduler.scheduleDailyReminder(hour, minute)
                statusMessage.value = String.format("Daily reminder scheduled for %02d:%02d", hour, minute)
            } else {
                reminderScheduler.cancelDailyReminder()
                statusMessage.value = "Daily reminder paused"
            }
        }
    }

    fun setCloudTtsEndpointUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(cloudTtsEndpointUrl = url.trim()) }
            statusMessage.value = if (url.isBlank()) {
                "Cloud TTS endpoint cleared (Offline Hindi TTS active)"
            } else {
                "Cloud TTS backend endpoint saved"
            }
        }
    }

    fun exportBackupToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val json = backupRepository.exportBackupJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open output stream")
                }
            }.onSuccess {
                statusMessage.value = "Full Divya Jaap backup saved successfully."
            }.onFailure {
                statusMessage.value = "Backup failed: ${it.localizedMessage}"
            }
        }
    }

    fun restoreBackupFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().readText()
                    } ?: error("Unable to read backup file")
                }
                backupRepository.restoreBackupJson(json).getOrThrow()
            }.onSuccess {
                statusMessage.value = "Backup restored cleanly without data loss."
            }.onFailure {
                statusMessage.value = "Restore failed: ${it.localizedMessage}"
            }
        }
    }

    fun exportCsvToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val csv = backupRepository.exportHistoryCsv()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open output stream")
                }
            }.onSuccess {
                statusMessage.value = "Jaap history CSV exported."
            }.onFailure {
                statusMessage.value = "Export failed: ${it.localizedMessage}"
            }
        }
    }
}
