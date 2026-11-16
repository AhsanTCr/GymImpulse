package com.example.models

enum class LeaderboardCategory(
    val title: String,
    val shortTitle: String,
    val description: String,
    val unitLabel: String
) {
    ATTENDANCE_STREAK(
        title = "Attendance Streak",
        shortTitle = "Streak",
        description = "Current & longest consecutive gym days • Monthly winner crown",
        unitLabel = "day streak"
    ),
    HEALTHY_HABITS_RACE(
        title = "Healthy Habits Race",
        shortTitle = "Habits",
        description = "Daily nutrition & healthy food goal completion rankings",
        unitLabel = "habit pts"
    ),
    CONSISTENCY_CHAMPION(
        title = "Consistency Champion",
        shortTitle = "Consistency",
        description = "Weekly target achieved (3+ visits/week over 4+ weeks)",
        unitLabel = "wks on target"
    ),
    REFERRAL_CHAMPION(
        title = "Referral Champion",
        shortTitle = "Referrals",
        description = "Members who referred verified new gym members",
        unitLabel = "verified referrals"
    )
}

data class LeaderboardEntry(
    val rank: Int,
    val memberId: String,
    val memberName: String,
    val avatarUrl: String? = null,
    val primaryScore: Int,
    val primaryMetricText: String,
    val secondaryStatText: String,
    val isCurrentUser: Boolean = false,
    val isMonthlyWinner: Boolean = false
)
