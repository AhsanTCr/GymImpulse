package com.example.features.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.models.NotificationPreferences
import com.example.utils.NotificationHelper

@Composable
fun NotificationPreferencesDialog(
    preferences: NotificationPreferences,
    statusMessage: String?,
    onUpdate: ((NotificationPreferences) -> NotificationPreferences) -> Unit,
    onSendTestReminder: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) onSendTestReminder()
        }
    )

    fun maybeRequestPostNotifications(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationHelper.hasNotificationPermission(context)
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notification Preferences") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PrefToggleItem(
                    title = "Attendance Streak Reminders",
                    subtitle = "Nudge if you haven't scanned entrance QR today",
                    checked = preferences.attendanceReminders,
                    onCheckedChange = { checked ->
                        maybeRequestPostNotifications(checked)
                        onUpdate { it.copy(attendanceReminders = checked) }
                    }
                )
                PrefToggleItem(
                    title = "Nutrition & Protein Alerts",
                    subtitle = "Reminders for daily healthy food checklists",
                    checked = preferences.nutritionGoalReminders,
                    onCheckedChange = { checked ->
                        maybeRequestPostNotifications(checked)
                        onUpdate { it.copy(nutritionGoalReminders = checked) }
                    }
                )
                PrefToggleItem(
                    title = "Membership Expiry Alerts",
                    subtitle = "Notify 5 days before renewal date",
                    checked = preferences.membershipExpiryAlerts,
                    onCheckedChange = { checked ->
                        maybeRequestPostNotifications(checked)
                        onUpdate { it.copy(membershipExpiryAlerts = checked) }
                    }
                )
                PrefToggleItem(
                    title = "Gym Events & Announcements",
                    subtitle = "Important club notices and competitions",
                    checked = preferences.gymAnnouncements,
                    onCheckedChange = { checked ->
                        maybeRequestPostNotifications(checked)
                        onUpdate { it.copy(gymAnnouncements = checked) }
                    }
                )
                PrefToggleItem(
                    title = "Leaderboard Rank Changes",
                    subtitle = "Weekly podium and rank updates",
                    checked = preferences.leaderboardUpdates,
                    onCheckedChange = { checked ->
                        maybeRequestPostNotifications(checked)
                        onUpdate { it.copy(leaderboardUpdates = checked) }
                    }
                )

                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !NotificationHelper.hasNotificationPermission(context)
                        ) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onSendTestReminder()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notifications_send_test_button")
                ) {
                    Text("Send Test Reminder Notification")
                }

                if (!statusMessage.isNullOrBlank()) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun PrefToggleItem(
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
