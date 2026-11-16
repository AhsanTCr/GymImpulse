package com.example.utils

import com.example.config.AppConfig
import com.example.database.AttendanceRecordDoc
import com.example.database.LeaderboardStatDoc
import com.example.models.LeaderboardCategory
import com.example.models.LeaderboardEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ComputedAttendanceMetrics(
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val monthlyVisits: Int,
    val weeklyTargetMetWeeks: Int,
    val totalWorkouts: Int
)

/**
 * Pure domain engine computing streaks, weekly consistency, and leaderboard rankings.
 */
object StreakAndLeaderboardEngine {

    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun computeAttendanceMetrics(
        records: List<AttendanceRecordDoc>,
        todayCalendar: Calendar = Calendar.getInstance()
    ): ComputedAttendanceMetrics {
        val uniqueDates = records
            .mapNotNull { doc ->
                runCatching { dateKeyFormat.parse(doc.dateKey) }.getOrNull()?.let { doc.dateKey }
            }
            .distinct()
            .sorted()

        if (uniqueDates.isEmpty()) {
            return ComputedAttendanceMetrics(0, 0, 0, 0, 0)
        }

        val dayNumbers = uniqueDates.mapNotNull { key ->
            runCatching {
                val parsed = dateKeyFormat.parse(key) ?: return@mapNotNull null
                val cal = Calendar.getInstance().apply {
                    time = parsed
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis / (24L * 60L * 60L * 1000L)
            }.getOrNull()
        }.distinct().sorted()

        var longest = 1
        var run = 1
        for (i in 1 until dayNumbers.size) {
            if (dayNumbers[i] == dayNumbers[i - 1] + 1L) {
                run++
                if (run > longest) longest = run
            } else {
                run = 1
            }
        }

        val todayDay = Calendar.getInstance().apply {
            timeInMillis = todayCalendar.timeInMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis / (24L * 60L * 60L * 1000L)

        // Current streak counts consecutive days ending today OR yesterday (streak stays alive until day ends)
        val daySet = dayNumbers.toHashSet()
        var currentStreak = 0
        var probe = if (daySet.contains(todayDay)) todayDay else todayDay - 1L
        while (daySet.contains(probe)) {
            currentStreak++
            probe -= 1L
        }

        val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.US).format(todayCalendar.time)
        val monthlyVisits = uniqueDates.count { it.startsWith(currentMonthPrefix) }

        // Weekly Consistency Champion: count weeks meeting 3+ visits
        val weekCounts = mutableMapOf<String, Int>()
        for (key in uniqueDates) {
            val parsed = runCatching { dateKeyFormat.parse(key) }.getOrNull() ?: continue
            val cal = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                minimalDaysInFirstWeek = 4
                time = parsed
            }
            val weekId = "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
            weekCounts[weekId] = (weekCounts[weekId] ?: 0) + 1
        }
        val weeklyTargetMetWeeks = weekCounts.values.count { it >= AppConfig.DEFAULT_WEEKLY_ATTENDANCE_TARGET }

        return ComputedAttendanceMetrics(
            currentStreakDays = currentStreak,
            longestStreakDays = longest,
            monthlyVisits = monthlyVisits,
            weeklyTargetMetWeeks = weeklyTargetMetWeeks,
            totalWorkouts = uniqueDates.size
        )
    }

    fun rankEntries(
        stats: List<LeaderboardStatDoc>,
        category: LeaderboardCategory,
        currentUserId: String
    ): List<LeaderboardEntry> {
        val sorted = when (category) {
            LeaderboardCategory.ATTENDANCE_STREAK -> stats.sortedWith(
                compareByDescending<LeaderboardStatDoc> { it.currentStreakDays }
                    .thenByDescending { it.longestStreakDays }
                    .thenByDescending { it.monthlyVisits }
            )
            LeaderboardCategory.HEALTHY_HABITS_RACE -> stats.sortedWith(
                compareByDescending<LeaderboardStatDoc> { it.healthyHabitsScore }
                    .thenByDescending { it.currentStreakDays }
            )
            LeaderboardCategory.CONSISTENCY_CHAMPION -> stats.sortedWith(
                compareByDescending<LeaderboardStatDoc> { it.weeklyTargetMetWeeks }
                    .thenByDescending { it.monthlyVisits }
            )
            LeaderboardCategory.REFERRAL_CHAMPION -> stats.sortedWith(
                compareByDescending<LeaderboardStatDoc> { it.verifiedReferralsCount }
                    .thenByDescending { it.monthlyVisits }
            )
        }

        return sorted.mapIndexed { index, doc ->
            val rank = index + 1
            val isMe = doc.userId == currentUserId
            val youTag = if (isMe) " • You" else ""
            val (primaryScore, primaryText, secondaryText) = when (category) {
                LeaderboardCategory.ATTENDANCE_STREAK -> Triple(
                    doc.currentStreakDays.toInt(),
                    "${doc.currentStreakDays}d streak",
                    "Best: ${doc.longestStreakDays}d • ${doc.monthlyVisits} visits/mo$youTag"
                )
                LeaderboardCategory.HEALTHY_HABITS_RACE -> Triple(
                    doc.healthyHabitsScore.toInt(),
                    "${doc.healthyHabitsScore}% goal",
                    "${doc.currentStreakDays}d streak • Nutrition habits$youTag"
                )
                LeaderboardCategory.CONSISTENCY_CHAMPION -> Triple(
                    doc.weeklyTargetMetWeeks.toInt(),
                    "${doc.weeklyTargetMetWeeks} wks",
                    "${AppConfig.DEFAULT_WEEKLY_ATTENDANCE_TARGET}+ visits/wk target • ${doc.monthlyVisits} this mo$youTag"
                )
                LeaderboardCategory.REFERRAL_CHAMPION -> Triple(
                    doc.verifiedReferralsCount.toInt(),
                    "${doc.verifiedReferralsCount} referrals",
                    "${doc.verifiedReferralsCount} verified club referrals$youTag"
                )
            }

            LeaderboardEntry(
                rank = rank,
                memberId = doc.userId,
                memberName = doc.memberName.ifBlank { "Club Member" },
                avatarUrl = doc.avatarData,
                primaryScore = primaryScore,
                primaryMetricText = primaryText,
                secondaryStatText = secondaryText,
                isCurrentUser = isMe,
                isMonthlyWinner = doc.isMonthlyWinner || (rank == 1 && primaryScore > 0)
            )
        }
    }
}
