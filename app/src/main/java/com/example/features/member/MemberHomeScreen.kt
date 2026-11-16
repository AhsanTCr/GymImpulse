package com.example.features.member

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CardMembership
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.models.TodayAttendanceState
import com.example.ui.components.CircularGoalRing
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MemberAvatar
import com.example.ui.components.MembershipStatusBadge
import com.example.ui.components.QuickAccessCard
import com.example.ui.components.RoleBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalGymSemanticColors

@Composable
fun MemberHomeScreen(
    viewModel: HomeDashboardViewModel,
    onNavigateToQrAttendance: () -> Unit,
    onNavigateToMembership: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToAnnouncements: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val semantic = LocalGymSemanticColors.current

    val member = state.member
    val membership = state.membership

    if (state.isLoading && member == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Syncing your club dashboard...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                icon = Icons.Rounded.ErrorOutline,
                title = "Dashboard Unavailable",
                subtitle = state.errorMessage ?: "Unable to load your member profile right now."
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Personalized Greeting + Avatar + Announcements Bell
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            onClick = onNavigateToProfile,
                            modifier = Modifier.testTag("home_avatar_button")
                        ) {
                            MemberAvatar(
                                fullName = member.fullName,
                                avatarUri = member.avatarUri,
                                size = 50.dp,
                                borderColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text(
                                text = state.greeting.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.accentVolt
                            )
                            Text(
                                text = member.fullName,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoleBadge(role = member.role)
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(44.dp)
                                .testTag("home_announcements_bell"),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            onClick = onNavigateToAnnouncements
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = stringResource(R.string.cd_notifications),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 2. Hero Membership Card (Status, Expiry Date, Cycle Progress)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_membership_card"),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    onClick = onNavigateToMembership
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = membership.planName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "ID ${member.memberCode} • Cycle ${membership.currentCycle.cycleLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                MembershipStatusBadge(status = membership.status)
                            }

                            val cycleProgress = (1f - (membership.currentCycle.daysRemaining.toFloat() /
                                membership.currentCycle.totalDaysInCycle.coerceAtLeast(1))).coerceIn(0.05f, 1f)
                            LinearProgressIndicator(
                                progress = { cycleProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Expires ${membership.nextRenewalDate} (${membership.currentCycle.daysRemaining} days left)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Manage",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Attendance Streak + Today's Attendance Status Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_streak_card"),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        onClick = onNavigateToLeaderboard
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = semantic.warningAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Streak",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${member.stats.currentStreakDays} Days",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Best: ${member.stats.longestStreakDays} days",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val checkedIn = state.todayAttendance == TodayAttendanceState.CHECKED_IN
                    Surface(
                        modifier = Modifier
                            .weight(1.15f)
                            .testTag("home_today_attendance_card"),
                        shape = MaterialTheme.shapes.medium,
                        color = if (checkedIn) semantic.activeGreenContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (checkedIn) semantic.activeGreen.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outlineVariant
                        ),
                        onClick = onNavigateToQrAttendance
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (checkedIn) Icons.Rounded.CheckCircle else Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = if (checkedIn) semantic.activeGreen else semantic.infoCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Today's Visit",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (checkedIn) "Checked In" else "Scan Entrance QR",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (checkedIn) "Verified at Main Entrance" else "Tap to scan gym entrance code",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (checkedIn) semantic.activeGreen else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 4. Daily Healthy Food & Protein Progress
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_nutrition_progress_card"),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    onClick = onNavigateToGoals
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val proteinProgress = state.proteinLoggedGrams.toFloat() /
                            state.proteinGoalGrams.coerceAtLeast(1)
                        CircularGoalRing(
                            progress = proteinProgress,
                            centerTitle = "${state.proteinLoggedGrams}g",
                            centerSubtitle = "of ${state.proteinGoalGrams}g",
                            ringColor = semantic.infoCyan,
                            size = 86.dp,
                            strokeWidth = 8.dp
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Daily Nutrition & Protein",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${state.foodsLoggedCount} of ${state.totalFoodsCount} healthy foods logged today",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = semantic.infoCyanContainer
                            ) {
                                Text(
                                    text = "Log Remaining Foods →",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = semantic.infoCyan,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5. Leaderboard Position Banner
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_leaderboard_position_card"),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    onClick = onNavigateToLeaderboard
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(semantic.warningAmberContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EmojiEvents,
                                    contentDescription = "Leaderboard Rank",
                                    tint = semantic.warningAmber
                                )
                            }
                            Column {
                                val rankText = if (member.stats.leaderboardRank > 0) {
                                    "Leaderboard Rank #${member.stats.leaderboardRank}"
                                } else {
                                    "Club Leaderboard"
                                }
                                val subText = if (member.stats.leaderboardRank > 0) {
                                    "Ranked #${member.stats.leaderboardRank} of ${member.stats.totalGymMembers} active members"
                                } else {
                                    "Scan entrance QR or log meals to earn a rank"
                                }
                                Text(
                                    text = rankText,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = subText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "View leaderboards",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 6. Latest Gym Announcement Card
            item {
                SectionHeader(
                    title = "Latest Gym Announcement",
                    actionLabel = "All Updates",
                    onActionClick = onNavigateToAnnouncements
                )
                Spacer(modifier = Modifier.height(8.dp))
                val ann = state.latestAnnouncement
                if (ann == null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_latest_announcement_card"),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        onClick = onNavigateToAnnouncements
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Campaign,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No Club Announcements Yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Events, challenges, and gym notices will appear here live.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_latest_announcement_card"),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        onClick = onNavigateToAnnouncements
                    ) {
                        Column {
                            if (ann.bannerDrawableRes != null) {
                                Image(
                                    painter = painterResource(id = ann.bannerDrawableRes),
                                    contentDescription = stringResource(R.string.cd_announcement_image),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(132.dp)
                                )
                            }
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (ann.isImportant) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = semantic.alertCoralContainer
                                        ) {
                                            Text(
                                                text = "IMPORTANT",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = semantic.alertCoral,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = ann.dateLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = ann.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = ann.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 7. Quick-Access Feature Grid
            item {
                SectionHeader(title = "Quick Access")
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickAccessCard(
                            title = "Entrance QR",
                            subtitle = "Scan gym door code",
                            icon = Icons.Rounded.QrCodeScanner,
                            accentColor = MaterialTheme.colorScheme.primary,
                            testTag = "quick_access_qr",
                            onClick = onNavigateToQrAttendance,
                            modifier = Modifier.weight(1f)
                        )
                        QuickAccessCard(
                            title = "My Membership",
                            subtitle = membership.status.label,
                            icon = Icons.Rounded.CardMembership,
                            accentColor = semantic.activeGreen,
                            testTag = "quick_access_membership",
                            onClick = onNavigateToMembership,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickAccessCard(
                            title = "Healthy Goals",
                            subtitle = "${state.proteinLoggedGrams}g protein logged",
                            icon = Icons.Rounded.Restaurant,
                            accentColor = semantic.infoCyan,
                            testTag = "quick_access_goals",
                            onClick = onNavigateToGoals,
                            modifier = Modifier.weight(1f)
                        )
                        QuickAccessCard(
                            title = "Announcements",
                            subtitle = "Events & challenges",
                            icon = Icons.Rounded.Campaign,
                            accentColor = semantic.warningAmber,
                            testTag = "quick_access_announcements",
                            onClick = onNavigateToAnnouncements,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
