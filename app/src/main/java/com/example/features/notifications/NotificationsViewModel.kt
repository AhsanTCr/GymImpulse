package com.example.features.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.MemberFirestoreRepository
import com.example.database.MemberSettingsDoc
import com.example.models.NotificationPreferences
import com.example.utils.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val repository: MemberFirestoreRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _latestDoc = MutableStateFlow(MemberSettingsDoc(userId = currentUserId))
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val preferences: StateFlow<NotificationPreferences> = repository
        .observeMemberSettings(currentUserId)
        .map { doc ->
            val resolved = doc ?: MemberSettingsDoc(userId = currentUserId)
            _latestDoc.value = resolved
            NotificationPreferences(
                attendanceReminders = resolved.attendanceReminders,
                nutritionGoalReminders = resolved.nutritionGoalReminders,
                membershipExpiryAlerts = resolved.membershipExpiryAlerts,
                gymAnnouncements = resolved.gymAnnouncements,
                leaderboardUpdates = resolved.leaderboardUpdates
            )
        }
        .catch { emit(NotificationPreferences()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = NotificationPreferences()
        )

    fun updatePreferences(transform: (NotificationPreferences) -> NotificationPreferences) {
        val current = preferences.value
        val next = transform(current)
        val mergedDoc = _latestDoc.value.copy(
            userId = currentUserId,
            attendanceReminders = next.attendanceReminders,
            nutritionGoalReminders = next.nutritionGoalReminders,
            membershipExpiryAlerts = next.membershipExpiryAlerts,
            gymAnnouncements = next.gymAnnouncements,
            leaderboardUpdates = next.leaderboardUpdates
        )
        _latestDoc.value = mergedDoc
        viewModelScope.launch {
            runCatching { repository.saveMemberSettings(mergedDoc) }
                .onFailure { _statusMessage.value = "Could not save notification preferences." }
        }
    }

    fun sendTestReminder(context: Context) {
        val sent = NotificationHelper.showReminderNotification(
            context = context,
            notificationId = 2001,
            title = "GymPulse Daily Reminder",
            body = "Keep your streak alive! Scan the entrance QR at reception and log your daily protein."
        )
        _statusMessage.value = if (sent) {
            "Reminder notification dispatched to your device."
        } else {
            "Enable notification permission in Android settings to receive reminders."
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
