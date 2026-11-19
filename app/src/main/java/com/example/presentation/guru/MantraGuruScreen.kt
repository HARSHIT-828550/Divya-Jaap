package com.example.presentation.guru

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.GuruPracticeMode
import com.example.domain.RepeatOption
import com.example.domain.TtsEnginePreference
import com.example.domain.VoiceGenderFilter
import com.example.presentation.theme.IvoryText
import com.example.presentation.theme.MutedText
import com.example.presentation.theme.NavyBorder
import com.example.presentation.theme.NavyCardHighlight
import com.example.presentation.theme.NavyElevated
import com.example.presentation.theme.NavySanctum
import com.example.presentation.theme.NavyVoid
import com.example.presentation.theme.SacredGold
import com.example.presentation.theme.SacredGoldBright
import com.example.presentation.theme.SandText
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MantraGuruScreen(
    viewModel: MantraGuruViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playback = state.playbackState
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setBackgroundPlaybackEnabled(granted)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyVoid)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = SacredGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI Mantra Guru",
                            style = MaterialTheme.typography.headlineMedium,
                            color = SacredGoldBright
                        )
                        Text(
                            text = playback.ttsStatusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SandText
                        )
                    }
                }

                // Quick Mantra Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.quickMantras.forEach { m ->
                        FilterChip(
                            selected = state.inputText.trim() == m.sanskritText.trim(),
                            onClick = { viewModel.selectPresetMantra(m.id) },
                            label = { Text(m.title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SacredGold,
                                selectedLabelColor = NavyVoid,
                                containerColor = NavySanctum,
                                labelColor = SandText
                            )
                        )
                    }
                }

                // Mantra Input Box
                OutlinedTextField(
                    value = state.inputText,
                    onValueChange = { viewModel.onTextChanged(it) },
                    label = { Text("Enter Sanskrit Shloka, Mantra, or Hindi Prayer") },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 18.sp,
                        color = IvoryText
                    ),
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guru_mantra_input")
                )

                // Synchronized Word / Syllable Highlighting Stage
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavySanctum),
                    border = BorderStroke(1.dp, SacredGold.copy(alpha = 0.35f)),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Interactive Pronunciation Stage",
                                style = MaterialTheme.typography.labelLarge,
                                color = SacredGold
                            )
                            if (playback.isPlaying || playback.isPaused) {
                                val repLabel = playback.totalRepetitions?.let { total ->
                                    "Rep ${playback.currentRepetition}/$total"
                                } ?: "Rep ${playback.currentRepetition} (∞)"
                                Text(
                                    text = repLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SacredGoldBright
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap any word or syllable to hear it spoken slowly:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedText
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            playback.words.forEachIndexed { wIndex, wordUnit ->
                                val isWordActive = wIndex == playback.activeWordIndex
                                if (playback.practiceMode == GuruPracticeMode.SYLLABLE_BY_SYLLABLE) {
                                    Surface(
                                        color = if (isWordActive) NavyCardHighlight else NavyElevated,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isWordActive) SacredGold else NavyBorder
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            wordUnit.syllables.forEachIndexed { sIndex, syl ->
                                                val isSylActive = isWordActive && sIndex == playback.activeSyllableIndex
                                                Surface(
                                                    color = if (isSylActive) SacredGold else NavySanctum,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.clickable { viewModel.speakWord(syl) }
                                                ) {
                                                    Text(
                                                        text = syl,
                                                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp),
                                                        color = if (isSylActive) NavyVoid else IvoryText,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Surface(
                                        color = if (isWordActive) SacredGold else NavyElevated,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isWordActive) SacredGoldBright else NavyBorder
                                        ),
                                        modifier = Modifier.clickable { viewModel.speakWord(wordUnit.word) }
                                    ) {
                                        Text(
                                            text = wordUnit.word,
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontSize = 19.sp,
                                                fontWeight = if (isWordActive) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isWordActive) NavyVoid else IvoryText,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Transport Bar: Play/Pause/Resume & Stop
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.playOrResume() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SacredGold,
                            contentColor = NavyVoid
                        ),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(52.dp)
                            .testTag("guru_play_pause_button")
                    ) {
                        if (playback.isLoadingCloudAudio) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NavyVoid,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Synthesizing...")
                        } else {
                            Icon(
                                imageVector = if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    playback.isPlaying -> "Pause"
                                    playback.isPaused -> "Resume"
                                    else -> "Play Mantra"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.stop() },
                        border = BorderStroke(1.dp, NavyBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("guru_stop_button")
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = IvoryText)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop", color = IvoryText)
                    }
                }

                // Practice Mode Selector
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavySanctum),
                    border = BorderStroke(1.dp, NavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Teaching Mode", style = MaterialTheme.typography.titleMedium, color = IvoryText)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GuruPracticeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = playback.practiceMode == mode,
                                    onClick = { viewModel.setPracticeMode(mode) },
                                    label = { Text(mode.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SacredGold,
                                        selectedLabelColor = NavyVoid
                                    )
                                )
                            }
                        }

                        Text("Repeat Count", style = MaterialTheme.typography.titleMedium, color = IvoryText)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RepeatOption.entries.forEach { opt ->
                                FilterChip(
                                    selected = playback.repeatOption == opt,
                                    onClick = { viewModel.setRepeatOption(opt) },
                                    label = { Text(opt.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SacredGold,
                                        selectedLabelColor = NavyVoid
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Speech Speed", style = MaterialTheme.typography.titleMedium, color = IvoryText)
                            Text(
                                text = String.format(Locale.US, "%.2f×", playback.speechRate),
                                style = MaterialTheme.typography.labelLarge,
                                color = SacredGoldBright
                            )
                        }
                        Slider(
                            value = playback.speechRate,
                            onValueChange = { viewModel.setSpeechRate(it) },
                            valueRange = 0.5f..1.75f,
                            colors = SliderDefaults.colors(
                                thumbColor = SacredGoldBright,
                                activeTrackColor = SacredGold
                            )
                        )
                    }
                }

                // Voice Engine, Gender, Specific Voices & Background Playback
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavySanctum),
                    border = BorderStroke(1.dp, NavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Voice Engine & Selection", style = MaterialTheme.typography.titleMedium, color = IvoryText)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TtsEnginePreference.entries.forEach { pref ->
                                FilterChip(
                                    selected = playback.enginePreference == pref,
                                    onClick = { viewModel.setEnginePreference(pref) },
                                    label = { Text(pref.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SacredGold,
                                        selectedLabelColor = NavyVoid
                                    )
                                )
                            }
                        }

                        Text("Voice Gender Filter", style = MaterialTheme.typography.labelLarge, color = SandText)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VoiceGenderFilter.entries.forEach { gender ->
                                FilterChip(
                                    selected = playback.genderFilter == gender,
                                    onClick = { viewModel.setGenderFilter(gender) },
                                    label = { Text(gender.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SacredGold,
                                        selectedLabelColor = NavyVoid
                                    )
                                )
                            }
                        }

                        val filteredVoices = playback.availableVoices.filter {
                            playback.genderFilter == VoiceGenderFilter.ALL || it.inferredGender == playback.genderFilter
                        }
                        if (filteredVoices.isNotEmpty()) {
                            Text("Available Hindi / Indian Voices", style = MaterialTheme.typography.labelLarge, color = SandText)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                filteredVoices.forEach { voice ->
                                    FilterChip(
                                        selected = playback.selectedVoiceName == voice.name,
                                        onClick = { viewModel.selectVoice(voice.name) },
                                        label = { Text(voice.displayLabel) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SacredGold,
                                            selectedLabelColor = NavyVoid
                                        )
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Background Lock-Screen Playback",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IvoryText
                                )
                                Text(
                                    text = "Plays via Media3 Foreground Service with lock-screen Play, Pause & Stop controls",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SandText
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = playback.backgroundPlaybackEnabled,
                                onCheckedChange = { enable ->
                                    if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!granted) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            return@Switch
                                        }
                                    }
                                    viewModel.setBackgroundPlaybackEnabled(enable)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NavyVoid,
                                    checkedTrackColor = SacredGold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
