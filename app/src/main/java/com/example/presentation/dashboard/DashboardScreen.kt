package com.example.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.TimeUtils
import com.example.presentation.theme.IvoryText
import com.example.presentation.theme.MutedText
import com.example.presentation.theme.NavyBorder
import com.example.presentation.theme.NavyElevated
import com.example.presentation.theme.NavySanctum
import com.example.presentation.theme.NavyVoid
import com.example.presentation.theme.SacredGold
import com.example.presentation.theme.SacredGoldBright
import com.example.presentation.theme.SaffronAmber
import com.example.presentation.theme.SandText

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                modifier = Modifier.widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Sadhana Insights",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SacredGoldBright
                )

                // Daily Target Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavySanctum),
                    border = BorderStroke(1.dp, SacredGold.copy(alpha = 0.4f)),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Today's Target Achievement", style = MaterialTheme.typography.titleMedium, color = IvoryText)
                            Text(
                                text = "${state.todayJaapCount} / ${state.dailyGoal}",
                                style = MaterialTheme.typography.titleMedium,
                                color = SacredGoldBright
                            )
                        }
                        LinearProgressIndicator(
                            progress = { state.dailyGoalProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            color = SacredGold,
                            trackColor = NavyElevated,
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            text = "${(state.dailyGoalProgress * 100).toInt()}% of daily spiritual goal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SandText
                        )
                    }
                }

                // KPI Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InsightMetricCard("Completed Malas", "${state.totalMalasCompleted}", Modifier.weight(1f))
                    InsightMetricCard("Lifetime Jaap", "${state.lifetimeJaapCount}", Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InsightMetricCard("Practice Streak", "${state.currentStreakDays} days", Modifier.weight(1f))
                    InsightMetricCard(
                        "Meditation Time",
                        TimeUtils.formatDuration(state.totalMeditationDurationSeconds),
                        Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InsightMetricCard("Weekly Jaap (7d)", "${state.weeklyJaapCount}", Modifier.weight(1f))
                    InsightMetricCard("Monthly Jaap (30d)", "${state.monthlyJaapCount}", Modifier.weight(1f))
                }

                // Daily Jaap Bar Chart
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavySanctum),
                    border = BorderStroke(1.dp, NavyBorder),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
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
                            Text("Daily Jaap Chart", style = MaterialTheme.typography.titleMedium, color = IvoryText)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = state.chartRangeDays == 7,
                                    onClick = { viewModel.setChartRangeDays(7) },
                                    label = { Text("7D") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SacredGold,
                                        selectedLabelColor = NavyVoid
                                    )
                                )
                                FilterChip(
                                    selected = state.chartRangeDays == 30,
                                    onClick = { viewModel.setChartRangeDays(30) },
                                    label = { Text("30D") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SacredGold,
                                        selectedLabelColor = NavyVoid
                                    )
                                )
                            }
                        }

                        DailyJaapBarChart(points = state.chartPoints)
                    }
                }

                // Mantra-Wise Breakdown
                Text(
                    text = "Mantra-Wise Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    color = SacredGold
                )

                if (state.mantraStats.isEmpty()) {
                    Surface(
                        color = NavySanctum,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, NavyBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Start tapping the Digital Rudraksha Mala to build your personal mantra analytics.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SandText,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    state.mantraStats.forEach { row ->
                        Surface(
                            color = NavySanctum,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, NavyBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(row.mantraTitle, style = MaterialTheme.typography.titleMedium, color = IvoryText)
                                    Text(
                                        text = "${row.completedMalas} Malas • ${TimeUtils.formatDuration(row.totalDurationSeconds)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MutedText
                                    )
                                }
                                Text(
                                    text = "${row.totalCount} Jaap",
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

@Composable
private fun InsightMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NavySanctum,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, NavyBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = SandText)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = SacredGoldBright,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun DailyJaapBarChart(points: List<DailyChartPoint>) {
    val maxCount = (points.maxOfOrNull { it.count } ?: 0).coerceAtLeast(10)

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            if (points.isEmpty()) return@Canvas
            val barSlot = size.width / points.size.toFloat()
            val barWidth = (barSlot * 0.58f).coerceAtLeast(4.dp.toPx())

            // Horizontal baseline & midlines
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { frac ->
                val y = size.height * (1f - frac)
                drawLine(
                    color = NavyBorder.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            points.forEachIndexed { idx, pt ->
                val ratio = (pt.count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
                val barHeight = (size.height - 8.dp.toPx()) * ratio
                val left = idx * barSlot + (barSlot - barWidth) / 2f
                val top = size.height - barHeight
                if (barHeight > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(SacredGoldBright, SaffronAmber)),
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (points.isNotEmpty()) {
                Text(points.first().label, style = MaterialTheme.typography.labelSmall, color = MutedText)
                if (points.size >= 3) {
                    Text(points[points.size / 2].label, style = MaterialTheme.typography.labelSmall, color = MutedText)
                }
                Text(points.last().label, style = MaterialTheme.typography.labelSmall, color = MutedText)
            }
        }
    }
}
