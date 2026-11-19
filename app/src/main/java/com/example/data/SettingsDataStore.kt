package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.CounterDisplayMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.divyaDataStore: DataStore<Preferences> by preferencesDataStore(name = "divya_jaap_prefs")

class SettingsDataStore(private val context: Context) {
    private val counterModeKey = stringPreferencesKey("counter_display_mode")
    private val lastGuruTextKey = stringPreferencesKey("last_guru_custom_text")

    val counterDisplayMode: Flow<CounterDisplayMode> = context.divyaDataStore.data.map { prefs ->
        val raw = prefs[counterModeKey] ?: CounterDisplayMode.RUDRAKSHA_MALA.name
        runCatching { CounterDisplayMode.valueOf(raw) }.getOrDefault(CounterDisplayMode.RUDRAKSHA_MALA)
    }

    val lastGuruText: Flow<String> = context.divyaDataStore.data.map { prefs ->
        prefs[lastGuruTextKey] ?: "ॐ ऐं ह्रीं क्लीं चामुण्डायै विच्चे"
    }

    suspend fun setCounterDisplayMode(mode: CounterDisplayMode) {
        context.divyaDataStore.edit { prefs ->
            prefs[counterModeKey] = mode.name
        }
    }

    suspend fun setLastGuruText(text: String) {
        context.divyaDataStore.edit { prefs ->
            prefs[lastGuruTextKey] = text
        }
    }
}
