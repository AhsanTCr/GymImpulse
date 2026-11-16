package com.example.models

enum class DataSourceType(val label: String) {
    LOGGED("Actual Logged"),
    ESTIMATED("Estimated"),
    MISSING("No Data")
}

enum class NutritionChartPeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

enum class FoodCategory(val label: String) {
    PROTEIN("High Protein"),
    DAIRY("Dairy"),
    WHOLE_GRAIN("Whole Grain & Nuts"),
    PRODUCE("Produce")
}

data class HealthyFoodItem(
    val id: String,
    val name: String,
    val servingDescription: String,
    val proteinGrams: Int,
    val calories: Int,
    val category: FoodCategory,
    val iconKey: String,
    val isTakenToday: Boolean,
    val loggedTime: String? = null,
    val dataSource: DataSourceType = DataSourceType.MISSING,
    val servingsCount: Int = if (isTakenToday) 1 else 0
)

data class NutritionChartPoint(
    val label: String,
    val proteinGrams: Int,
    val goalGrams: Int,
    val source: DataSourceType
)

data class FoodLogHistoryItem(
    val id: String,
    val dateLabel: String,
    val foodsTakenCount: Int,
    val totalFoodsCount: Int,
    val proteinLoggedGrams: Int,
    val proteinGoalGrams: Int,
    val dataSource: DataSourceType
)

data class NutritionReminderSettings(
    val morningFuelEnabled: Boolean = true,
    val morningTime: String = "08:30 AM",
    val postWorkoutProteinEnabled: Boolean = true,
    val postWorkoutTime: String = "06:30 PM",
    val eveningCheckInEnabled: Boolean = false,
    val eveningTime: String = "09:00 PM"
)
