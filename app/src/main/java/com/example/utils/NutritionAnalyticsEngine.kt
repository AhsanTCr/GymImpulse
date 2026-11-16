package com.example.utils

import com.example.config.AppConfig
import com.example.database.DailyFoodLogDoc
import com.example.models.DataSourceType
import com.example.models.FoodCategory
import com.example.models.FoodLogHistoryItem
import com.example.models.HealthyFoodItem
import com.example.models.NutritionChartPeriod
import com.example.models.NutritionChartPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pure domain analytics engine implementing strict Missing Data & Chart Rules:
 * - 1 missing day -> estimate using the average of suitable available logged days
 * - 2nd consecutive missing day -> estimate using the average
 * - 3+ consecutive missing days -> mark as MISSING ("Not tracked") and NEVER estimate
 */
object NutritionAnalyticsEngine {

    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayLabelFormat = SimpleDateFormat("MMM d", Locale.US)
    private val shortWeekdayFormat = SimpleDateFormat("EEE", Locale.US)

    val catalogFoods: List<HealthyFoodItem> = listOf(
        HealthyFoodItem("f_eggs", "Eggs", "3 whole boiled / scrambled (1 srv)", 18, 210, FoodCategory.PROTEIN, "eggs", false, null, DataSourceType.MISSING),
        HealthyFoodItem("f_chicken", "Chicken Breast", "180g grilled fillet (1 srv)", 42, 265, FoodCategory.PROTEIN, "chicken", false, null, DataSourceType.MISSING),
        HealthyFoodItem("f_milk", "Milk", "300ml low-fat fortified (1 srv)", 11, 130, FoodCategory.DAIRY, "milk", false, null, DataSourceType.MISSING),
        HealthyFoodItem("f_yogurt", "Greek Yogurt", "200g plain high-protein (1 srv)", 20, 145, FoodCategory.DAIRY, "yogurt", false, null, DataSourceType.MISSING),
        HealthyFoodItem("f_fish", "Salmon / Fish", "160g baked fillet (1 srv)", 34, 290, FoodCategory.PROTEIN, "fish", false, null, DataSourceType.MISSING),
        HealthyFoodItem("f_almonds", "Almonds", "30g raw roasted handful (1 srv)", 6, 175, FoodCategory.WHOLE_GRAIN, "almonds", false, null, DataSourceType.MISSING),
        HealthyFoodItem("f_oats", "Rolled Oats", "60g overnight oats bowl (1 srv)", 8, 225, FoodCategory.WHOLE_GRAIN, "oats", false, null, DataSourceType.MISSING),
        HealthyFoodItem("f_fruits", "Fresh Fruits", "Banana & mixed berries (1 srv)", 2, 120, FoodCategory.PRODUCE, "fruits", false, null, DataSourceType.MISSING),
        HealthyFoodItem("f_veg", "Green Vegetables", "Broccoli, spinach & peppers (1 srv)", 5, 85, FoodCategory.PRODUCE, "vegetables", false, null, DataSourceType.MISSING),
        HealthyFoodItem("f_whey", "Whey / Plant Shake", "1 scoop post-workout (1 srv)", 24, 120, FoodCategory.PROTEIN, "shake", false, null, DataSourceType.MISSING)
    )

    fun calculateTotals(servingsByFood: Map<String, Int>): Triple<Int, Int, Int> {
        var totalProtein = 0
        var totalCalories = 0
        var foodsTaken = 0
        for (item in catalogFoods) {
            val servings = (servingsByFood[item.id] ?: 0).coerceAtLeast(0)
            if (servings > 0) {
                foodsTaken += 1
                totalProtein += item.proteinGrams * servings
                totalCalories += item.calories * servings
            }
        }
        return Triple(totalProtein, totalCalories, foodsTaken)
    }

    /**
     * Resolves a chronological series of days applying the 1/2-day estimation rule
     * and never estimating 3+ consecutive missing days.
     */
    fun resolveDailySeriesWithMissingRules(
        orderedDayKeys: List<String>,
        logsByDateKey: Map<String, DailyFoodLogDoc>,
        goalGrams: Int = AppConfig.DEFAULT_PROTEIN_GOAL_GRAMS
    ): List<Triple<String, Int, DataSourceType>> {
        val loggedProteins = orderedDayKeys.mapNotNull { key ->
            val doc = logsByDateKey[key]
            if (doc != null && (doc.foodsCount > 0L || doc.totalProteinGrams > 0L)) {
                doc.totalProteinGrams.toInt()
            } else {
                null
            }
        }
        val suitableAverage = if (loggedProteins.isNotEmpty()) {
            loggedProteins.average().roundToInt()
        } else {
            0
        }

        // First pass: mark raw LOGGED vs raw MISSING
        val rawLogged = orderedDayKeys.map { key ->
            val doc = logsByDateKey[key]
            doc != null && (doc.foodsCount > 0L || doc.totalProteinGrams > 0L)
        }

        // Identify consecutive missing runs
        val results = MutableList(orderedDayKeys.size) { Triple(orderedDayKeys[it], 0, DataSourceType.MISSING) }
        var i = 0
        while (i < orderedDayKeys.size) {
            val key = orderedDayKeys[i]
            if (rawLogged[i]) {
                val grams = logsByDateKey[key]?.totalProteinGrams?.toInt() ?: 0
                results[i] = Triple(key, grams, DataSourceType.LOGGED)
                i++
            } else {
                // Find contiguous missing run [i, runEnd)
                var runEnd = i
                while (runEnd < orderedDayKeys.size && !rawLogged[runEnd]) {
                    runEnd++
                }
                val runLength = runEnd - i
                if (runLength <= 2 && suitableAverage > 0) {
                    // 1 or 2 consecutive missing days -> estimate using suitable average
                    for (j in i until runEnd) {
                        results[j] = Triple(orderedDayKeys[j], suitableAverage, DataSourceType.ESTIMATED)
                    }
                } else {
                    // 3+ consecutive missing days (or zero baseline logged days) -> NEVER estimate ("Not tracked")
                    for (j in i until runEnd) {
                        results[j] = Triple(orderedDayKeys[j], 0, DataSourceType.MISSING)
                    }
                }
                i = runEnd
            }
        }
        return results
    }

    fun buildChartPoints(
        period: NutritionChartPeriod,
        logsByDateKey: Map<String, DailyFoodLogDoc>,
        todayCalendar: Calendar = Calendar.getInstance(),
        goalGrams: Int = AppConfig.DEFAULT_PROTEIN_GOAL_GRAMS
    ): List<NutritionChartPoint> {
        return when (period) {
            NutritionChartPeriod.DAILY -> {
                val todayKey = dateKeyFormat.format(todayCalendar.time)
                val todayLog = logsByDateKey[todayKey]
                val totalLogged = todayLog?.totalProteinGrams?.toInt() ?: 0
                if (totalLogged <= 0) {
                    listOf(
                        NutritionChartPoint("8AM", 0, goalGrams, DataSourceType.MISSING),
                        NutritionChartPoint("12PM", 0, goalGrams, DataSourceType.MISSING),
                        NutritionChartPoint("4PM", 0, goalGrams, DataSourceType.MISSING),
                        NutritionChartPoint("8PM", 0, goalGrams, DataSourceType.MISSING)
                    )
                } else {
                    // Cumulative breakdown across day checkpoints based on actual logged food count
                    val q1 = (totalLogged * 0.25f).roundToInt()
                    val q2 = (totalLogged * 0.55f).roundToInt()
                    val q3 = (totalLogged * 0.80f).roundToInt()
                    listOf(
                        NutritionChartPoint("8AM", q1, goalGrams, DataSourceType.LOGGED),
                        NutritionChartPoint("12PM", q2, goalGrams, DataSourceType.LOGGED),
                        NutritionChartPoint("4PM", q3, goalGrams, DataSourceType.LOGGED),
                        NutritionChartPoint("Now", totalLogged, goalGrams, DataSourceType.LOGGED)
                    )
                }
            }
            NutritionChartPeriod.WEEKLY -> {
                val dayKeys = (6 downTo 0).map { offset ->
                    val cal = (todayCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
                    dateKeyFormat.format(cal.time) to if (offset == 0) "Today" else shortWeekdayFormat.format(cal.time)
                }
                val resolved = resolveDailySeriesWithMissingRules(dayKeys.map { it.first }, logsByDateKey, goalGrams)
                resolved.mapIndexed { idx, (_, grams, source) ->
                    NutritionChartPoint(
                        label = dayKeys[idx].second,
                        proteinGrams = grams,
                        goalGrams = goalGrams,
                        source = source
                    )
                }
            }
            NutritionChartPeriod.MONTHLY -> {
                val dayKeys = (27 downTo 0).map { offset ->
                    val cal = (todayCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
                    dateKeyFormat.format(cal.time)
                }
                val resolved = resolveDailySeriesWithMissingRules(dayKeys, logsByDateKey, goalGrams)
                (0 until 4).map { weekIdx ->
                    val slice = resolved.subList(weekIdx * 7, (weekIdx + 1) * 7)
                    val trackedOrEstimated = slice.filter { it.third != DataSourceType.MISSING }
                    if (trackedOrEstimated.isEmpty()) {
                        NutritionChartPoint("Wk ${weekIdx + 1}", 0, goalGrams, DataSourceType.MISSING)
                    } else {
                        val avg = trackedOrEstimated.map { it.second }.average().roundToInt()
                        val source = if (slice.all { it.third == DataSourceType.LOGGED }) {
                            DataSourceType.LOGGED
                        } else if (slice.any { it.third == DataSourceType.ESTIMATED }) {
                            DataSourceType.ESTIMATED
                        } else {
                            DataSourceType.LOGGED
                        }
                        NutritionChartPoint("Wk ${weekIdx + 1}", avg, goalGrams, source)
                    }
                }
            }
        }
    }

    fun buildLogHistory(
        logsByDateKey: Map<String, DailyFoodLogDoc>,
        todayCalendar: Calendar = Calendar.getInstance(),
        daysBack: Int = 7,
        goalGrams: Int = AppConfig.DEFAULT_PROTEIN_GOAL_GRAMS
    ): List<FoodLogHistoryItem> {
        val keysWithLabels = (daysBack - 1 downTo 0).map { offset ->
            val cal = (todayCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val key = dateKeyFormat.format(cal.time)
            val label = when (offset) {
                0 -> "Today • ${displayLabelFormat.format(cal.time)}"
                1 -> "Yesterday • ${displayLabelFormat.format(cal.time)}"
                else -> displayLabelFormat.format(cal.time)
            }
            key to label
        }
        val resolved = resolveDailySeriesWithMissingRules(keysWithLabels.map { it.first }, logsByDateKey, goalGrams)
        return resolved.mapIndexed { idx, (key, grams, source) ->
            val doc = logsByDateKey[key]
            FoodLogHistoryItem(
                id = key,
                dateLabel = keysWithLabels[idx].second,
                foodsTakenCount = doc?.foodsCount?.toInt() ?: 0,
                totalFoodsCount = catalogFoods.size,
                proteinLoggedGrams = grams,
                proteinGoalGrams = goalGrams,
                dataSource = source
            )
        }.reversed()
    }

    fun calculateHealthyHabitsScore(
        logsByDateKey: Map<String, DailyFoodLogDoc>,
        todayCalendar: Calendar = Calendar.getInstance(),
        goalGrams: Int = AppConfig.DEFAULT_PROTEIN_GOAL_GRAMS
    ): Int {
        val dayKeys = (13 downTo 0).map { offset ->
            val cal = (todayCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
            dateKeyFormat.format(cal.time)
        }
        val loggedDays = dayKeys.mapNotNull { logsByDateKey[it] }.filter { it.foodsCount > 0L || it.totalProteinGrams > 0L }
        if (loggedDays.isEmpty()) return 0
        val avgCompletion = loggedDays.map { log ->
            (log.totalProteinGrams.toFloat() / goalGrams.coerceAtLeast(1)).coerceIn(0f, 1.1f)
        }.average()
        val consistencyFactor = (loggedDays.size.toFloat() / 14f).coerceIn(0.2f, 1f)
        return ((avgCompletion * 75.0) + (consistencyFactor * 25.0)).roundToInt().coerceIn(0, 100)
    }
}
