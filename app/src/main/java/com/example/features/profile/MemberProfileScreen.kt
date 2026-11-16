package com.example.features.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CardMembership
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.features.notifications.NotificationPreferencesDialog
import com.example.features.notifications.NotificationsViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MemberAvatar
import com.example.ui.components.MembershipStatusBadge
import com.example.ui.components.MetricStatCard
import com.example.ui.components.RoleBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LocalGymSemanticColors

@Composable
fun MemberProfileScreen(
    profileViewModel: ProfileViewModel,
    notificationsViewModel: NotificationsViewModel,
    currentThemeMode: AppThemeMode,
    onChangeThemeMode: (AppThemeMode) -> Unit,
    onNavigateToMembership: () -> Unit,
    onNavigateToAppUpdates: () -> Unit,
    isAdminAuthorized: Boolean = false,
    onNavigateToAdminPreview: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by profileViewModel.uiState.collectAsStateWithLifecycle()
    val notifPrefs by notificationsViewModel.preferences.collectAsStateWithLifecycle()
    val notifStatus by notificationsViewModel.statusMessage.collectAsStateWithLifecycle()
    val semantic = LocalGymSemanticColors.current
    val context = LocalContext.current

    var showNotificationDialog by remember { mutableStateOf(false) }
    var showAttendanceHistoryDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var copiedReferral by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { profileViewModel.updateProfilePhoto(context, it.toString()) }
        }
    )

    val member = state.member
    val membership = state.membership

    if (state.isLoading && member == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (member == null || membership == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateView(
                icon = Icons.Rounded.FitnessCenter,
                title = "Profile Unavailable",
                subtitle = state.profileStatusMessage ?: "Unable to load member details."
            )
        }
        return
    }

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
            // 1. Profile Hero Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                MemberAvatar(
                                    fullName = member.fullName,
                                    avatarUri = member.avatarUri,
                                    size = 76.dp,
                                    borderColor = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("profile_edit_photo_button"),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Change profile photo",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RoleBadge(role = member.role)
                                    TextButton(
                                        onClick = { showEditProfileDialog = true },
                                        modifier = Modifier.testTag("profile_edit_details_button")
                                    ) {
                                        Text("Edit Profile")
                                    }
                                }
                                Text(
                                    text = member.fullName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Member ID ${member.memberCode} • Since ${member.joinedDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!state.profileStatusMessage.isNullOrBlank()) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = semantic.infoCyanContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.profileStatusMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = semantic.infoCyan,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = member.phone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = member.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = member.fitnessGoal,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (state.referralCode.isNotBlank()) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CardGiftcard,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Your Referral Code: ${state.referralCode}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (copiedReferral) "Copied to clipboard!"
                                                else "${member.stats.referralsCount} verified referrals",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            clipboard?.setPrimaryClip(ClipData.newPlainText("GymPulse Referral", state.referralCode))
                                            copiedReferral = true
                                        },
                                        modifier = Modifier.testTag("profile_copy_referral_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentCopy,
                                            contentDescription = "Copy referral code",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Membership Information Summary Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_membership_card"),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    onClick = onNavigateToMembership
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CardMembership,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = membership.planName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Renews ${membership.nextRenewalDate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MembershipStatusBadge(status = membership.status)
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "Open My Membership",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. Personal Statistics Grid
            item {
                SectionHeader(title = "Personal Statistics")
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricStatCard(
                            label = "Active Streak",
                            value = "${member.stats.currentStreakDays}d",
                            caption = "Longest: ${member.stats.longestStreakDays} days",
                            icon = Icons.Rounded.LocalFireDepartment,
                            accentColor = semantic.warningAmber,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            label = "Total Workouts",
                            value = "${member.stats.totalWorkouts}",
                            caption = "${member.stats.monthlyWorkouts} visits this month",
                            icon = Icons.Rounded.FitnessCenter,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricStatCard(
                            label = "Club Rank",
                            value = if (member.stats.leaderboardRank > 0) "#${member.stats.leaderboardRank}" else "--",
                            caption = "Of ${member.stats.totalGymMembers} members",
                            icon = Icons.Rounded.EmojiEvents,
                            accentColor = semantic.infoCyan,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            label = "Weekly Target",
                            value = "${member.stats.weeklyTargetMetWeeks} wks",
                            caption = "${member.stats.referralsCount} verified referrals",
                            icon = Icons.Rounded.CalendarToday,
                            accentColor = semantic.activeGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Theme Settings (Light / Dark / System)
            item {
                SectionHeader(title = "Appearance & Settings")
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "App Theme",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = currentThemeMode == mode,
                                    onClick = { onChangeThemeMode(mode) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_chip_${mode.name.lowercase()}"),
                                    label = { Text(mode.label) }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Attendance History, Notification Preferences, App Updates, Reserved Admin
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        ProfileActionRow(
                            icon = Icons.Rounded.History,
                            title = "Attendance History",
                            subtitle = "${state.attendanceHistory.size} verified entrance scans",
                            accentColor = MaterialTheme.colorScheme.primary,
                            testTag = "profile_attendance_history_row",
                            onClick = { showAttendanceHistoryDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileActionRow(
                            icon = Icons.Rounded.NotificationsActive,
                            title = "Notification Preferences",
                            subtitle = "Attendance, protein goals & club alerts",
                            accentColor = semantic.infoCyan,
                            testTag = "profile_notifications_row",
                            onClick = {
                                notificationsViewModel.clearStatusMessage()
                                showNotificationDialog = true
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileActionRow(
                            icon = Icons.Rounded.SystemUpdate,
                            title = "App Updates",
                            subtitle = "Installed v${state.updateInfo.installedVersion} • Latest v${state.updateInfo.latestVersion}",
                            accentColor = semantic.activeGreen,
                            testTag = "profile_app_updates_row",
                            onClick = onNavigateToAppUpdates
                        )
                        if (isAdminAuthorized) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            ProfileActionRow(
                                icon = Icons.Rounded.AdminPanelSettings,
                                title = "Gymimpulse Admin Console",
                                subtitle = "Passcode-protected club management",
                                accentColor = semantic.warningAmber,
                                testTag = "profile_admin_preview_row",
                                onClick = onNavigateToAdminPreview
                            )
                        }
                    }
                }
            }

            // 6. Logout Option
            item {
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("profile_logout_button"),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, semantic.alertCoral.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = "Log out",
                        tint = semantic.alertCoral
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Log Out of Account",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = semantic.alertCoral
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showNotificationDialog) {
        NotificationPreferencesDialog(
            preferences = notifPrefs,
            statusMessage = notifStatus,
            onUpdate = notificationsViewModel::updatePreferences,
            onSendTestReminder = { notificationsViewModel.sendTestReminder(context) },
            onDismiss = { showNotificationDialog = false }
        )
    }

    if (showAttendanceHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showAttendanceHistoryDialog = false },
            title = { Text("Attendance History") },
            text = {
                if (state.attendanceHistory.isEmpty()) {
                    Text(
                        text = "No verified entrance scans recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.attendanceHistory.take(8).forEach { rec ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = rec.dateLabel, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = rec.checkInTime,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAttendanceHistoryDialog = false }) { Text("Close") }
            }
        )
    }

    if (showEditProfileDialog) {
        var draftName by remember(member.fullName) { mutableStateOf(member.fullName) }
        var draftPhone by remember(member.phone) {
            mutableStateOf(if (member.phone == "Not added") "" else member.phone)
        }
        var draftGoal by remember(member.fitnessGoal) { mutableStateOf(member.fitnessGoal) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Member Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { draftName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_name_input")
                    )
                    OutlinedTextField(
                        value = draftPhone,
                        onValueChange = { draftPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_phone_input")
                    )
                    OutlinedTextField(
                        value = draftGoal,
                        onValueChange = { draftGoal = it },
                        label = { Text("Fitness Goal") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_goal_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        profileViewModel.updateProfileDetails(draftName, draftPhone, draftGoal)
                        showEditProfileDialog = false
                    },
                    modifier = Modifier.testTag("edit_profile_save_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        color = Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.14f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
