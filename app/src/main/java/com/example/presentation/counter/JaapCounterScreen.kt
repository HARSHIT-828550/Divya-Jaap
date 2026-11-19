package com.example.presentation.counter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.CounterDisplayMode
import com.example.presentation.theme.IvoryText
import com.example.presentation.theme.NavyBorder
import com.example.presentation.theme.NavyCardHighlight
import com.example.presentation.theme.NavyElevated
import com.example.presentation.theme.NavySanctum
import com.example.presentation.theme.NavyVoid
import com.example.presentation.theme.SacredGold
import com.example.presentation.theme.SacredGoldBright
import com.example.presentation.theme.SaffronAmber
import com.example.presentation.theme.SandText

@Composable
fun JaapCounterScreen(
    viewModel: JaapCounterViewModel,
    onOpenLibrary: () -> Unit,
    onOpenGuruWithMantra: (String, String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    val handleTap = {
        if (state.settings.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        viewModel.onCounterTap()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyVoid)
    ) {
        // Subtle atmospheric temple backdrop in top header
        if (state.displayMode != CounterDisplayMode.MINIMAL_MEDITATION) {
            Image(
                painter = painterResource(id = R.drawable.img_sacred_sanctum),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .alpha(0.24f)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, NavyVoid)
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 640.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mode Switcher Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    CounterDisplayMode.entries.forEach { mode ->
                        val selected = state.displayMode == mode
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setDisplayMode(mode) },
                            label = {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SacredGold,
                                selectedLabelColor = NavyVoid,
                                containerColor = NavySanctum.copy(alpha = 0.85f),
                                labelColor = SandText
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = NavyBorder,
                                selectedBorderColor = SacredGold
                            ),
                            modifier = Modifier.testTag("mode_chip_${mode.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (state.displayMode != CounterDisplayMode.MINIMAL_MEDITATION) {
                    // Active Mantra Card
                    val mantraTitle = state.activeMantra?.title ?: "Panchakshari Shiv Mantra"
                    val mantraText = state.activeMantra?.sanskritText ?: "ॐ नमः शिवाय"
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NavySanctum.copy(alpha = 0.9f)),
                        border = BorderStroke(1.dp, SacredGold.copy(alpha = 0.35f)),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenLibrary() }
                            .testTag("active_mantra_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mantraTitle.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SacredGold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = mantraText,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp),
                                    color = IvoryText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onOpenGuruWithMantra(mantraTitle, mantraText) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Listen in AI Mantra Guru",
                                    tint = SacredGold
                                )
                            }
                            IconButton(
                                onClick = onOpenLibrary,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = "Change mantra from library",
                                    tint = SandText
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = state.activeMantra?.sanskritText ?: "ॐ नमः शिवाय",
                        style = MaterialTheme.typography.headlineSmall,
                        color = SacredGold.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Mala Completion Celebration Banner
                AnimatedVisibility(
                    visible = state.showMalaCompletedBanner,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NavyCardHighlight),
                        border = BorderStroke(1.5.dp, SacredGoldBright),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("mala_completed_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Mala completed",
                                tint = SacredGoldBright,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mala #${state.lastCompletedMalaNumber} Completed",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SacredGoldBright
                                )
                                Text(
                                    text = "Next Mala is ready. Your count is saved.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = IvoryText
                                )
                            }
                            TextButton(onClick = { viewModel.dismissMalaCompletedBanner() }) {
                                Text("Continue", color = SacredGoldBright)
                            }
                        }
                    }
                }

                // Centerpiece Counter
                when (state.displayMode) {
                    CounterDisplayMode.RUDRAKSHA_MALA -> {
                        RudrakshaMalaWheel(
                            currentCount = state.currentCount,
                            targetCount = state.targetCount,
                            onTap = handleTap
                        )
                    }
                    CounterDisplayMode.DIGITAL -> {
                        DigitalCircularTapCounter(
                            currentCount = state.currentCount,
                            targetCount = state.targetCount,
                            progress = state.progressFraction,
                            onTap = handleTap
                        )
                    }
                    CounterDisplayMode.MINIMAL_MEDITATION -> {
                        MinimalMeditationCounter(
                            currentCount = state.currentCount,
                            targetCount = state.targetCount,
                            completedMalas = state.totalMalasCompleted,
                            onTap = handleTap
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (state.displayMode != CounterDisplayMode.MINIMAL_MEDITATION) {
                    // Stats Row: Current Mala, Completed Malas, Today, Lifetime
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CounterStatTile(
                            label = "Mala Progress",
                            value = "${state.currentCount}/${state.targetCount}",
                            modifier = Modifier.weight(1f)
                        )
                        CounterStatTile(
                            label = "Completed Malas",
                            value = "${state.totalMalasCompleted}",
                            modifier = Modifier.weight(1f),
                            tag = "completed_malas_value"
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CounterStatTile(
                            label = "Today's Jaap",
                            value = "${state.todayJaapCount}",
                            modifier = Modifier.weight(1f),
                            tag = "today_jaap_value"
                        )
                        CounterStatTile(
                            label = "Lifetime Jaap",
                            value = "${state.lifetimeJaapCount}",
                            modifier = Modifier.weight(1f),
                            tag = "lifetime_jaap_value"
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.onUndoTap() },
                            enabled = state.currentCount > 0,
                            border = BorderStroke(1.dp, NavyBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("undo_tap_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Undo")
                        }
                        OutlinedButton(
                            onClick = { viewModel.openTargetPicker() },
                            border = BorderStroke(1.dp, SacredGold.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("target_picker_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = SacredGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${state.targetCount}", color = SacredGold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.onStartNextMala() },
                            border = BorderStroke(1.dp, NavyBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("next_mala_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Next Mala")
                        }
                        OutlinedButton(
                            onClick = { viewModel.requestResetConfirmation() },
                            enabled = state.currentCount > 0,
                            border = BorderStroke(1.dp, NavyBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("reset_mala_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Mala")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (state.showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissResetConfirmation() },
            containerColor = NavySanctum,
            title = { Text("Reset Current Mala?", color = IvoryText) },
            text = {
                Text(
                    "This resets only the active Mala bead count (${state.currentCount}) back to 0. All completed Malas and lifetime counts remain safely saved.",
                    color = SandText
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmResetCurrentMala() },
                    colors = ButtonDefaults.buttonColors(containerColor = SacredGold, contentColor = NavyVoid)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissResetConfirmation() }) {
                    Text("Cancel", color = SandText)
                }
            }
        )
    }

    if (state.showTargetPickerDialog) {
        MalaTargetDialog(
            currentTarget = state.targetCount,
            onDismiss = { viewModel.closeTargetPicker() },
            onSelectTarget = { viewModel.updateMalaTarget(it) }
        )
    }
}

@Composable
private fun DigitalCircularTapCounter(
    currentCount: Int,
    targetCount: Int,
    progress: Float,
    onTap: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 450f),
        label = "digital_tap_scale"
    )

    Box(
        modifier = Modifier
            .size(290.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 12.dp,
            color = NavyElevated,
            strokeCap = StrokeCap.Round
        )
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 12.dp,
            color = SacredGold,
            strokeCap = StrokeCap.Round
        )
        Box(
            modifier = Modifier
                .scale(scale)
                .size(228.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NavyCardHighlight, NavySanctum, NavyVoid)
                    )
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = SacredGold),
                    onClick = onTap
                )
                .testTag("jaap_tap_button"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$currentCount",
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = SacredGoldBright,
                        fontSize = 62.sp
                    )
                )
                Text(
                    text = "of $targetCount",
                    style = MaterialTheme.typography.titleMedium,
                    color = SandText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "TAP TO COUNT",
                    style = MaterialTheme.typography.labelSmall,
                    color = SacredGold
                )
            }
        }
    }
}

@Composable
private fun MinimalMeditationCounter(
    currentCount: Int,
    targetCount: Int,
    completedMalas: Int,
    onTap: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "minimal_tap_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .size(280.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(SacredGold.copy(alpha = 0.18f), NavySanctum, NavyVoid)
                    )
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = SacredGold),
                    onClick = onTap
                )
                .testTag("jaap_tap_button"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.img_rudraksha_meditation),
                    contentDescription = "Sacred Rudraksha meditation focus",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "$currentCount / $targetCount",
                    style = MaterialTheme.typography.displaySmall,
                    color = SacredGoldBright
                )
                Text(
                    text = "$completedMalas Malas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SandText
                )
            }
        }
    }
}

@Composable
private fun CounterStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tag: String? = null
) {
    Surface(
        color = NavySanctum,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, NavyBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SandText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = SacredGoldBright,
                    fontWeight = FontWeight.Bold
                ),
                modifier = if (tag != null) Modifier.testTag(tag) else Modifier
            )
        }
    }
}

@Composable
private fun MalaTargetDialog(
    currentTarget: Int,
    onDismiss: () -> Unit,
    onSelectTarget: (Int) -> Unit
) {
    val presets = listOf(27, 54, 108, 1008)
    var customInput by remember { mutableStateOf(currentTarget.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySanctum,
        title = { Text("Select Mala Target", color = IvoryText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Choose traditional bead counts or enter a custom target:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SandText
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = currentTarget == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectTarget(preset) },
                            label = { Text("$preset") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SacredGold,
                                selectedLabelColor = NavyVoid
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { value -> customInput = value.filter { it.isDigit() }.take(5) },
                    label = { Text("Custom Target (1 – 10008)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = customInput.toIntOrNull()?.coerceIn(1, 10008) ?: 108
                    onSelectTarget(parsed)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SacredGold, contentColor = NavyVoid)
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SandText)
            }
        }
    )
}
