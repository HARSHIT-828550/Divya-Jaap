package com.example.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restore
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.theme.IvoryText
import com.example.presentation.theme.MutedText
import com.example.presentation.theme.NavyBorder
import com.example.presentation.theme.NavyElevated
import com.example.presentation.theme.NavySanctum
import com.example.presentation.theme.NavyVoid
import com.example.presentation.theme.SacredGold
import com.example.presentation.theme.SacredGoldBright
import com.example.presentation.theme.SandText
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings
    val context = LocalContext.current

    var goalInput by remember(settings.dailyJaapGoal) { mutableStateOf(settings.dailyJaapGoal.toString()) }
    var cloudUrlInput by remember(settings.cloudTtsEndpointUrl) { mutableStateOf(settings.cloudTtsEndpointUrl) }
    var reminderHourInput by remember(settings.reminderHour) { mutableStateOf(settings.reminderHour.toString()) }
    var reminderMinInput by remember(settings.reminderMinute) { mutableStateOf(String.format(Locale.US, "%02d", settings.reminderMinute)) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val h = reminderHourInput.toIntOrNull()?.coerceIn(0, 23) ?: 6
            val m = reminderMinInput.toIntOrNull()?.coerceIn(0, 59) ?: 30
            viewModel.setDailyReminder(true, h, m)
        }
    }

    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackupToUri(context, it) }
    }

    val backupRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.restoreBackupFromUri(context, it) }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportCsvToUri(context, it) }
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
                    text = "Settings & Backup",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SacredGoldBright
                )

                state.statusMessage?.let { msg ->
                    Surface(
                        color = NavyElevated,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, SacredGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(msg, color = SacredGoldBright, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.clearStatusMessage() }) {
                                Text("OK", color = SacredGold)
                            }
                        }
                    }
                }

                // Daily Jaap Goal
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavySanctum),
                    border = BorderStroke(1.dp, NavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Daily Jaap Goal", style = MaterialTheme.typography.titleMedium, color = IvoryText)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(54, 108, 324, 1008).forEach { preset ->
                                FilterChip(
                                    selected = settings.dailyJaapGoal == preset,
                                    onClick = { viewModel.setDailyJaapGoal(preset) },
                                    label = { Text("$preset") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SacredGold,
                                        selectedLabelColor = NavyVoid
                                    )
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = goalInput,
                                onValueChange = { goalInput = it.filter { c -> c.isDigit() }.take(5) },
                                label = { Text("Custom Daily Goal") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    viewModel.setDailyJaapGoal(goalInput.toIntOrNull() ?: 108)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SacredGold, contentColor = NavyVoid),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text("Set")
                            }
                        }
                    }
                }

                // Sensory & Screen Settings
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavySanctum),
                    border = BorderStroke(1.dp, NavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Meditation Experience", style = MaterialTheme.typography.titleMedium, color = IvoryText)

                        SettingsToggleRow(
                            title = "Haptic Bead Feedback",
                            subtitle = "Gentle tactile pulse on every Jaap tap",
                            checked = settings.hapticEnabled,
                            onCheckedChange = { viewModel.setHapticEnabled(it) }
                        )
                        SettingsToggleRow(
                            title = "Temple Bell Sound",
                            subtitle = "Play resonant bronze bell when Mala completes",
                            checked = settings.templeBellEnabled,
                            onCheckedChange = { viewModel.setTempleBellEnabled(it) }
                        )
                        SettingsToggleRow(
                            title = "Keep Screen Always On",
                            subtitle = "Prevents screen timeout during active meditation",
                            checked = settings.screenAlwaysOn,
                            onCheckedChange = { viewModel.setScreenAlwaysOn(it) }
                        )
                    }
                }

                // Daily Reminders
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavySanctum),
                    border = BorderStroke(1.dp, NavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = SacredGold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Daily Jaap Reminder", style = MaterialTheme.typography.titleMedium, color = IvoryText)
                            }
                            Switch(
                                checked = settings.reminderEnabled,
                                onCheckedChange = { enable ->
                                    val h = reminderHourInput.toIntOrNull()?.coerceIn(0, 23) ?: settings.reminderHour
                                    val m = reminderMinInput.toIntOrNull()?.coerceIn(0, 59) ?: settings.reminderMinute
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
                                    viewModel.setDailyReminder(enable, h, m)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = NavyVoid, checkedTrackColor = SacredGold)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = reminderHourInput,
                                onValueChange = { reminderHourInput = it.filter { c -> c.isDigit() }.take(2) },
                                label = { Text("Hour (0-23)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = reminderMinInput,
                                onValueChange = { reminderMinInput = it.filter { c -> c.isDigit() }.take(2) },
                                label = { Text("Minute") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = {
                                    val h = reminderHourInput.toIntOrNull()?.coerceIn(0, 23) ?: 6
                                    val m = reminderMinInput.toIntOrNull()?.coerceIn(0, 59) ?: 30
                                    viewModel.setDailyReminder(true, h, m)
                                },
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text("Save Time", color = SacredGoldBright)
                            }
                        }
                    }
                }

                // Secure Cloud TTS Backend Endpoint (Zero hardcoded API keys)
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavySanctum),
                    border = BorderStroke(1.dp, NavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Cloud Natural AI Voice Endpoint", style = MaterialTheme.typography.titleMedium, color = IvoryText)
                        Text(
                            text = "Optional backend proxy URL for neural Cloud TTS. API credentials stay strictly on your backend server, never in the APK. Offline Hindi TTS always works without internet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedText
                        )
                        OutlinedTextField(
                            value = cloudUrlInput,
                            onValueChange = { cloudUrlInput = it },
                            label = { Text("https://your-backend.example.com/api/tts") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { viewModel.setCloudTtsEndpointUrl(cloudUrlInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = SacredGold, contentColor = NavyVoid)
                        ) {
                            Text("Save Endpoint")
                        }
                    }
                }

                // Backup, Restore & Export
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavySanctum),
                    border = BorderStroke(1.dp, NavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Backup, Restore & Export", style = MaterialTheme.typography.titleMedium, color = IvoryText)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { backupExportLauncher.launch("divya_jaap_backup.json") },
                                border = BorderStroke(1.dp, SacredGold.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Backup, contentDescription = null, tint = SacredGold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup JSON", color = SacredGoldBright)
                            }
                            OutlinedButton(
                                onClick = { backupRestoreLauncher.launch(arrayOf("application/json", "*/*")) },
                                border = BorderStroke(1.dp, NavyBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, tint = IvoryText)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore", color = IvoryText)
                            }
                        }

                        Button(
                            onClick = { csvExportLauncher.launch("divya_jaap_history.csv") },
                            colors = ButtonDefaults.buttonColors(containerColor = SacredGold, contentColor = NavyVoid),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Jaap History (CSV)")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = IvoryText)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = SandText)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = NavyVoid, checkedTrackColor = SacredGold)
        )
    }
}
