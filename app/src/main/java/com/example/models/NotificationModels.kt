package com.example.models

data class NotificationPreferences(
    val attendanceReminders: Boolean = true,
    val nutritionGoalReminders: Boolean = true,
    val membershipExpiryAlerts: Boolean = true,
    val gymAnnouncements: Boolean = true,
    val leaderboardUpdates: Boolean = false
)

enum class UpdateAvailabilityState {
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    CHECKING,
    READY_TO_INSTALL
}

data class AppUpdateInfo(
    val installedVersion: String,
    val latestVersion: String,
    val releaseDate: String,
    val availabilityState: UpdateAvailabilityState,
    val releaseHighlights: List<String>,
    val downloadUrl: String? = null,
    val githubRepo: String? = null
)
