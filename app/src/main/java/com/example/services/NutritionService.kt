package com.example.services

import com.example.models.FoodLogHistoryItem
import com.example.models.HealthyFoodItem
import com.example.models.NutritionChartPeriod
import com.example.models.NutritionChartPoint
import com.example.models.NutritionReminderSettings

interface NutritionService {
    fun getTodayFoods(): List<HealthyFoodItem>
    fun getChartPoints(period: NutritionChartPeriod): List<NutritionChartPoint>
    fun getLogHistory(): List<FoodLogHistoryItem>
    fun getReminderSettings(): NutritionReminderSettings
}

class MockNutritionService : NutritionService {
    override fun getTodayFoods(): List<HealthyFoodItem> = SampleGymData.healthyFoods
    override fun getChartPoints(period: NutritionChartPeriod): List<NutritionChartPoint> =
        SampleGymData.getNutritionChart(period)
    override fun getLogHistory(): List<FoodLogHistoryItem> = SampleGymData.foodLogHistory
    override fun getReminderSettings(): NutritionReminderSettings = SampleGymData.defaultReminderSettings
}
