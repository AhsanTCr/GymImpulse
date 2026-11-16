package com.example.features.nutrition

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Egg
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SetMeal
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.models.DataSourceType
import com.example.models.FoodLogHistoryItem
import com.example.models.HealthyFoodItem
import com.example.models.NutritionChartPeriod
import com.example.models.NutritionReminderSettings
import com.example.ui.components.CircularGoalRing
import com.example.ui.components.DataSourceBadge
import com.example.ui.components.NutritionBarChart
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalGymSemanticColors

@Composable
fun DailyGoalsScreen(
    viewModel: NutritionGoalsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val semantic = LocalGymSemanticColors.current
    var showReminderDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header + Reminder Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NUTRITION & HABITS",
                            style = MaterialTheme.typography.labelSmall,
                            color = semantic.accentVolt
                        )
                        Text(
                            text = "Daily Fitness Goals",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        IconButton(
                            onClick = { showReminderDialog = true },
                            modifier = Modifier.testTag("nutrition_reminders_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Alarm,
                                contentDescription = "Nutrition Reminder Settings",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (!state.errorMessage.isNullOrBlank()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = semantic.alertCoralContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.alertCoral,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Dual Progress Rings: Daily Goal + Protein Goal
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularGoalRing(
                                progress = state.takenCount.toFloat() / state.dailyFoodTarget.coerceAtLeast(1),
                                centerTitle = "${state.takenCount}/${state.dailyFoodTarget}",
                                centerSubtitle = "foods",
                                ringColor = semantic.activeGreen,
                                size = 96.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Daily Food Goal",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            DataSourceBadge(
                                source = if (state.takenCount > 0) DataSourceType.LOGGED else DataSourceType.MISSING
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularGoalRing(
                                progress = state.loggedProteinGrams.toFloat() / state.proteinGoalGrams.coerceAtLeast(1),
                                centerTitle = "${state.loggedProteinGrams}g",
                                centerSubtitle = "of ${state.proteinGoalGrams}g",
                                ringColor = semantic.infoCyan,
                                size = 96.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Protein Target",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(state.proteinGoalGrams - state.loggedProteinGrams).coerceAtLeast(0)}g left • ${state.loggedCalories} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Daily / Weekly / Monthly Charts distinguishing Logged vs Estimated vs Missing ("Not tracked")
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Protein Intake Trends",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NutritionChartPeriod.entries.forEach { period ->
                                FilterChip(
                                    selected = state.selectedPeriod == period,
                                    onClick = { viewModel.selectChartPeriod(period) },
                                    modifier = Modifier.testTag("chart_period_${period.name.lowercase()}"),
                                    label = { Text(period.label) }
                                )
                            }
                        }

                        NutritionBarChart(points = state.chartPoints)
                    }
                }
            }

            // Healthy Foods Checklist with Servings +/-
            item {
                SectionHeader(
                    title = "Healthy Foods Checklist",
                    actionLabel = "Reminders",
                    onActionClick = { showReminderDialog = true }
                )
            }

            items(state.foods, key = { it.id }) { food ->
                HealthyFoodRowCard(
                    food = food,
                    onToggleTaken = { viewModel.toggleFoodTaken(food.id) },
                    onIncrementServing = { viewModel.incrementServing(food.id) },
                    onDecrementServing = { viewModel.decrementServing(food.id) }
                )
            }

            // Food Logging History
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionHeader(title = "Food Logging History (7 Days)")
            }

            items(state.logHistory, key = { it.id }) { history ->
                FoodHistoryRowCard(history = history)
            }
        }
    }

    if (showReminderDialog) {
        ReminderSettingsDialog(
            settings = state.reminderSettings,
            onDismiss = { showReminderDialog = false },
            onSave = { updated ->
                viewModel.updateReminderSettings(updated)
                showReminderDialog = false
            }
        )
    }
}

@Composable
private fun HealthyFoodRowCard(
    food: HealthyFoodItem,
    onToggleTaken: () -> Unit,
    onIncrementServing: () -> Unit,
    onDecrementServing: () -> Unit
) {
    val semantic = LocalGymSemanticColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("food_card_${food.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (food.isTakenToday) semantic.activeGreen.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (food.isTakenToday) semantic.activeGreenContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = iconForFood(food.iconKey),
                            contentDescription = food.name,
                            tint = if (food.isTakenToday) semantic.activeGreen
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = food.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "+${food.proteinGrams}g",
                            style = MaterialTheme.typography.labelSmall,
                            color = semantic.infoCyan
                        )
                    }
                    Text(
                        text = food.servingDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DataSourceBadge(source = food.dataSource)
                        if (food.servingsCount > 0) {
                            Text(
                                text = "${food.servingsCount} srv (${food.proteinGrams * food.servingsCount}g)",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.activeGreen
                            )
                        }
                    }
                }

                if (food.isTakenToday) {
                    Button(
                        onClick = onToggleTaken,
                        modifier = Modifier.testTag("food_taken_btn_${food.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = semantic.activeGreen,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Taken",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Taken", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                } else {
                    OutlinedButton(
                        onClick = onToggleTaken,
                        modifier = Modifier.testTag("food_taken_btn_${food.id}"),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Take", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (food.isTakenToday) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Servings:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = onDecrementServing,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("food_dec_${food.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = "Decrease servings",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "${food.servingsCount}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onIncrementServing,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("food_inc_${food.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Increase servings",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodHistoryRowCard(history: FoodLogHistoryItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = history.dateLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (history.dataSource) {
                        DataSourceType.MISSING -> "Not tracked (3+ missing days)"
                        DataSourceType.ESTIMATED -> "Estimated ~${history.proteinLoggedGrams}g / ${history.proteinGoalGrams}g protein"
                        DataSourceType.LOGGED -> "${history.foodsTakenCount}/${history.totalFoodsCount} foods • ${history.proteinLoggedGrams}g / ${history.proteinGoalGrams}g protein"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DataSourceBadge(source = history.dataSource)
        }
    }
}

@Composable
private fun ReminderSettingsDialog(
    settings: NutritionReminderSettings,
    onDismiss: () -> Unit,
    onSave: (NutritionReminderSettings) -> Unit
) {
    var draft by remember(settings) { mutableStateOf(settings) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nutrition Reminders") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ReminderToggleRow(
                    title = "Morning Fuel (${draft.morningTime})",
                    subtitle = "Eggs, oats & dairy checklist reminder",
                    checked = draft.morningFuelEnabled,
                    onCheckedChange = { draft = draft.copy(morningFuelEnabled = it) }
                )
                ReminderToggleRow(
                    title = "Post-Workout Protein (${draft.postWorkoutTime})",
                    subtitle = "High-protein meal or shake alert",
                    checked = draft.postWorkoutProteinEnabled,
                    onCheckedChange = { draft = draft.copy(postWorkoutProteinEnabled = it) }
                )
                ReminderToggleRow(
                    title = "Evening Log Check (${draft.eveningTime})",
                    subtitle = "Fill missing food entries before midnight",
                    checked = draft.eveningCheckInEnabled,
                    onCheckedChange = { draft = draft.copy(eveningCheckInEnabled = it) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ReminderToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun iconForFood(key: String): ImageVector = when (key) {
    "eggs" -> Icons.Rounded.Egg
    "chicken", "fish" -> Icons.Rounded.SetMeal
    "milk", "yogurt", "shake" -> Icons.Rounded.LocalDrink
    "almonds", "oats" -> Icons.Rounded.Grain
    "fruits" -> Icons.Rounded.Spa
    "vegetables" -> Icons.Rounded.Eco
    else -> Icons.Rounded.Restaurant
}
