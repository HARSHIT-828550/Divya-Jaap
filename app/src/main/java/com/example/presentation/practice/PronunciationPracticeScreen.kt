package com.example.presentation.practice

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.WordMatchStatus
import com.example.presentation.theme.AmberMissing
import com.example.presentation.theme.CoralMistake
import com.example.presentation.theme.EmeraldMatch
import com.example.presentation.theme.IvoryText
import com.example.presentation.theme.MutedText
import com.example.presentation.theme.NavyBorder
import com.example.presentation.theme.NavyElevated
import com.example.presentation.theme.NavySanctum
import com.example.presentation.theme.NavyVoid
import com.example.presentation.theme.SacredGold
import com.example.presentation.theme.SacredGoldBright
import com.example.presentation.theme.SandText
import com.example.speech.SpeechRecognitionState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PronunciationPracticeScreen(
    viewModel: PronunciationPracticeViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedMessage = null
            viewModel.startSpeechCapture()
        } else {
            permissionDeniedMessage = "Microphone permission is required so Divya Jaap can listen to your chanting."
        }
    }

    val startMicWithPermission = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            permissionDeniedMessage = null
            viewModel.startSpeechCapture()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
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
                Text(
                    text = "Pronunciation Practice",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SacredGoldBright
                )

                // Disclaimer Banner (STT != Phonetic Swara Assessment)
                Surface(
                    color = NavyElevated,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, SacredGold.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SacredGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(id = R.string.speech_disclaimer),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SandText
                        )
                    }
                }

                // Mantra Quick Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.availableMantras.forEach { m ->
                        FilterChip(
                            selected = state.selectedMantraId == m.id,
                            onClick = { viewModel.selectMantra(m.id) },
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

                // Editable Target Mantra Card
                OutlinedTextField(
                    value = state.targetText,
                    onValueChange = { viewModel.setCustomTargetText(it) },
                    label = { Text("Practice Text (Sanskrit / Hindi)") },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 18.sp,
                        color = IvoryText
                    ),
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                // Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GuidedPracticeStep.entries.forEach { step ->
                        val active = state.step == step
                        Surface(
                            color = if (active) SacredGold else NavySanctum,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (active) SacredGold else NavyBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${step.stepNumber}. ${step.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) NavyVoid else SandText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Listen & Speak Action Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavySanctum),
                    border = BorderStroke(1.dp, NavyBorder),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.listenSlowly() },
                            border = BorderStroke(1.dp, SacredGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("practice_listen_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = SacredGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Step 1: Listen Slowly to AI Guru", color = SacredGoldBright)
                        }

                        val isListening = state.speechState is SpeechRecognitionState.Listening
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clip(CircleShape)
                                .background(if (isListening) CoralMistake else SacredGold)
                                .clickable {
                                    if (isListening) viewModel.stopSpeechCapture()
                                    else startMicWithPermission()
                                }
                                .testTag("practice_mic_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isListening) "Stop listening" else "Tap microphone and chant",
                                tint = NavyVoid,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        when (val speech = state.speechState) {
                            is SpeechRecognitionState.Idle -> {
                                Text(
                                    text = "Step 2: Tap the microphone and recite the mantra in Hindi/Sanskrit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SandText
                                )
                            }
                            is SpeechRecognitionState.Listening -> {
                                Text(
                                    text = if (speech.partialText.isBlank()) "Listening... chant clearly" else "Heard: ${speech.partialText}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SacredGoldBright
                                )
                            }
                            is SpeechRecognitionState.Error -> {
                                Text(
                                    text = speech.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CoralMistake
                                )
                            }
                            is SpeechRecognitionState.Result -> {
                                Text(
                                    text = "Recognized: ${speech.recognizedText}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = IvoryText
                                )
                            }
                        }

                        if (!state.isSpeechAvailable) {
                            Text(
                                text = "Device speech recognition service is unavailable. Ensure Google Speech Services is enabled.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmberMissing
                            )
                        }

                        permissionDeniedMessage?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CoralMistake
                            )
                        }
                    }
                }

                // Word-by-Word Comparison Card
                state.evaluation?.let { eval ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NavySanctum),
                        border = BorderStroke(1.dp, SacredGold.copy(alpha = 0.45f)),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("practice_feedback_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Word Recognition: ${eval.matchPercentage}%",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = SacredGoldBright
                                )
                                Text(
                                    text = "${eval.matchedCount}/${eval.totalTargetWords} matched",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldMatch
                                )
                            }

                            Text(
                                text = "Tap any highlighted word to hear slow pronunciation:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedText
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                eval.wordResults.forEach { wr ->
                                    val (bg, border, labelColor) = when (wr.status) {
                                        WordMatchStatus.MATCHED -> Triple(
                                            EmeraldMatch.copy(alpha = 0.14f),
                                            EmeraldMatch,
                                            EmeraldMatch
                                        )
                                        WordMatchStatus.MISSING -> Triple(
                                            AmberMissing.copy(alpha = 0.14f),
                                            AmberMissing,
                                            AmberMissing
                                        )
                                        WordMatchStatus.MISTAKE -> Triple(
                                            CoralMistake.copy(alpha = 0.14f),
                                            CoralMistake,
                                            CoralMistake
                                        )
                                    }
                                    Surface(
                                        color = bg,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, border),
                                        modifier = Modifier.clickable { viewModel.speakDifficultWord(wr.expectedWord) }
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                            Text(
                                                text = wr.expectedWord,
                                                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 17.sp),
                                                color = IvoryText
                                            )
                                            val subLabel = when (wr.status) {
                                                WordMatchStatus.MATCHED -> "Matched"
                                                WordMatchStatus.MISSING -> "Missed"
                                                WordMatchStatus.MISTAKE -> "Heard: ${wr.recognizedWord ?: "?"}"
                                            }
                                            Text(
                                                text = subLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = labelColor
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { viewModel.retryPractice() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SacredGold,
                                    contentColor = NavyVoid
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("practice_retry_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry Pronunciation", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Practice History
                if (state.recentHistory.isNotEmpty()) {
                    Text(
                        text = "Recent Practice History",
                        style = MaterialTheme.typography.titleMedium,
                        color = SacredGold
                    )
                    state.recentHistory.take(8).forEach { item ->
                        Surface(
                            color = NavySanctum,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, NavyBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (item.wordMatchPercentage >= 75) EmeraldMatch else AmberMissing,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.mantraTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = IvoryText
                                    )
                                    Text(
                                        text = "Heard: ${item.recognizedText}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SandText
                                    )
                                }
                                Text(
                                    text = "${item.wordMatchPercentage}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SacredGoldBright
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
