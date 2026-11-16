package com.example.features.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.AppConfig
import com.example.database.DailyFoodLogDoc
import com.example.database.MemberFirestoreRepository
import com.example.database.MemberSettingsDoc
import com.example.database.toFriendlyFirestoreMessage
import com.example.models.DataSourceType
import com.example.models.FoodLogHistoryItem
import com.example.models.HealthyFoodItem
import com.example.models.NutritionChartPeriod
import com.example.models.NutritionChartPoint
import com.example.models.NutritionReminderSettings
import com.example.utils.NutritionAnalyticsEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class NutritionGoalsUiState(
    val isLoading: Boolean = true,
    val foods: List<HealthyFoodItem> = NutritionAnalyticsEngine.catalogFoods,
    val servingsByFood: Map<String, Int> = emptyMap(),
    val selectedPeriod: NutritionChartPeriod = NutritionChartPeriod.WEEKLY,
    val chartPoints: List<NutritionChartPoint> = emptyList(),
    val logHistory: List<FoodLogHistoryItem> = emptyList(),
    val reminderSettings: NutritionReminderSettings = NutritionReminderSettings(),
    val proteinGoalGrams: Int = AppConfig.DEFAULT_PROTEIN_GOAL_GRAMS,
    val dailyFoodTarget: Int = AppConfig.DEFAULT_FOOD_TARGET_COUNT,
    val logsByDateKey: Map<String, DailyFoodLogDoc> = emptyMap(),
    val memberName: String = "Club Member",
    val errorMessage: String? = null
) {
    val loggedProteinGrams: Int
        get() = foods.sumOf { it.proteinGrams * it.servingsCount }

    val loggedCalories: Int
        get() = foods.sumOf { it.calories * it.servingsCount }

    val takenCount: Int
        get() = foods.count { it.servingsCount > 0 }
}

class NutritionGoalsViewModel(
    private val repository: MemberFirestoreRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(NutritionChartPeriod.WEEKLY)
    private val _latestSettingsDoc = MutableStateFlow<MemberSettingsDoc?>(null)
    private val _actionError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<NutritionGoalsUiState> = combine(
        repository.observeUserProfile(currentUserId),
        repository.observeFoodLogs(currentUserId),
        repository.observeMemberSettings(currentUserId).catch { emit(null) },
        _selectedPeriod,
        _actionError
    ) { profileDoc, logDocs, settingsDoc, period, actionErr ->
        _latestSettingsDoc.value = settingsDoc
        val cal = Calendar.getInstance()
        val todayKey = repository.todayDateKey(cal)
        val logsMap = logDocs.associateBy { it.dateKey }
        val todayDoc = logsMap[todayKey]

        val servingsMap: Map<String, Int> = buildMap {
            if (todayDoc != null) {
                todayDoc.servingsByFood.forEach { (foodId, count) ->
                    if (count > 0L) put(foodId, count.toInt())
                }
                todayDoc.takenFoodIds.forEach { foodId ->
                    if (!containsKey(foodId)) put(foodId, 1)
                }
            }
        }

        val foods = NutritionAnalyticsEngine.catalogFoods.map { item ->
            val servings = servingsMap[item.id] ?: 0
            val isTaken = servings > 0
            item.copy(
                isTakenToday = isTaken,
                servingsCount = servings,
                loggedTime = if (isTaken) "Logged today" else null,
                dataSource = if (isTaken) DataSourceType.LOGGED else DataSourceType.MISSING
            )
        }

        val chartPoints = NutritionAnalyticsEngine.buildChartPoints(
            period = period,
            logsByDateKey = logsMap,
            todayCalendar = cal,
            goalGrams = AppConfig.DEFAULT_PROTEIN_GOAL_GRAMS
        )

        val logHistory = NutritionAnalyticsEngine.buildLogHistory(
            logsByDateKey = logsMap,
            todayCalendar = cal,
            daysBack = 7,
            goalGrams = AppConfig.DEFAULT_PROTEIN_GOAL_GRAMS
        )

        val reminderSettings = NutritionReminderSettings(
            morningFuelEnabled = settingsDoc?.morningFuelEnabled ?: true,
            postWorkoutProteinEnabled = settingsDoc?.postWorkoutProteinEnabled ?: true,
            eveningCheckInEnabled = settingsDoc?.eveningCheckInEnabled ?: false
        )

        NutritionGoalsUiState(
            isLoading = false,
            foods = foods,
            servingsByFood = servingsMap,
            selectedPeriod = period,
            chartPoints = chartPoints,
            logHistory = logHistory,
            reminderSettings = reminderSettings,
            logsByDateKey = logsMap,
            memberName = profileDoc?.fullName ?: "Club Member",
            errorMessage = actionErr
        )
    }.catch { error ->
        emit(
            NutritionGoalsUiState(
                isLoading = false,
                selectedPeriod = _selectedPeriod.value,
                errorMessage = error.toFriendlyFirestoreMessage("Unable to sync food logs.")
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = NutritionGoalsUiState(isLoading = true)
    )

    fun toggleFoodTaken(foodId: String) {
        val currentCount = uiState.value.servingsByFood[foodId] ?: 0
        val nextCount = if (currentCount > 0) 0 else 1
        updateFoodServings(foodId, nextCount)
    }

    fun incrementServing(foodId: String) {
        val currentCount = uiState.value.servingsByFood[foodId] ?: 0
        updateFoodServings(foodId, (currentCount + 1).coerceAtMost(8))
    }

    fun decrementServing(foodId: String) {
        val currentCount = uiState.value.servingsByFood[foodId] ?: 0
        updateFoodServings(foodId, (currentCount - 1).coerceAtLeast(0))
    }

    private fun updateFoodServings(foodId: String, newCount: Int) {
        val snapshot = uiState.value
        val updatedServings = snapshot.servingsByFood.toMutableMap()
        if (newCount <= 0) {
            updatedServings.remove(foodId)
        } else {
            updatedServings[foodId] = newCount
        }
        _actionError.value = null
        viewModelScope.launch {
            try {
                val cal = Calendar.getInstance()
                repository.saveDailyFoodLog(
                    dateKey = repository.todayDateKey(cal),
                    dateLabel = repository.todayDateLabel(cal),
                    servingsByFood = updatedServings,
                    allLogsMapForScore = snapshot.logsByDateKey,
                    memberNameForLeaderboard = snapshot.memberName
                )
            } catch (e: Exception) {
                _actionError.value = e.toFriendlyFirestoreMessage("Failed to save food log.")
            }
        }
    }

    fun selectChartPeriod(period: NutritionChartPeriod) {
        _selectedPeriod.value = period
    }

    fun updateReminderSettings(updated: NutritionReminderSettings) {
        val base = _latestSettingsDoc.value ?: MemberSettingsDoc(userId = currentUserId)
        val merged = base.copy(
            userId = currentUserId,
            morningFuelEnabled = updated.morningFuelEnabled,
            postWorkoutProteinEnabled = updated.postWorkoutProteinEnabled,
            eveningCheckInEnabled = updated.eveningCheckInEnabled
        )
        viewModelScope.launch {
            try {
                repository.saveMemberSettings(merged)
            } catch (e: Exception) {
                _actionError.value = e.toFriendlyFirestoreMessage("Could not save reminder settings.")
            }
        }
    }
}
