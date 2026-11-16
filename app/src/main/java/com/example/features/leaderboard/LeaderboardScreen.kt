package com.example.features.leaderboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.models.LeaderboardCategory
import com.example.models.LeaderboardEntry
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MemberAvatar
import com.example.ui.theme.LocalGymSemanticColors
import com.example.ui.theme.PodiumBronze
import com.example.ui.theme.PodiumGold
import com.example.ui.theme.PodiumSilver

@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val semantic = LocalGymSemanticColors.current
    var showPastWinnersDialog by remember { mutableStateOf(false) }

    val showPodium = state.entries.size >= 3
    val topThree = if (showPodium) state.entries.take(3) else emptyList()
    val listEntries = if (showPodium) state.entries.drop(3) else state.entries

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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CLUB LEADERBOARDS",
                            style = MaterialTheme.typography.labelSmall,
                            color = semantic.accentVolt
                        )
                        Text(
                            text = state.selectedCategory.title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = state.selectedCategory.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { showPastWinnersDialog = true },
                        modifier = Modifier.testTag("leaderboard_past_winners_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = "Past Monthly Winners",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Winners", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // 4-Category Switcher
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LeaderboardCategory.entries.forEach { category ->
                        val selected = category == state.selectedCategory
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.selectCategory(category) },
                            modifier = Modifier.testTag("leaderboard_tab_${category.name.lowercase()}"),
                            label = {
                                Text(
                                    text = category.shortTitle,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (!state.errorMessage.isNullOrBlank()) {
                item {
                    EmptyStateView(
                        icon = Icons.Rounded.EmojiEvents,
                        title = "Leaderboard Unavailable",
                        subtitle = state.errorMessage.orEmpty()
                    )
                }
            } else if (state.entries.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Rounded.EmojiEvents,
                        title = "No Rankings Yet",
                        subtitle = "Members appear on this leaderboard as soon as attendance scans or nutrition logs are synced."
                    )
                }
            } else {
                // Top 3 Podium Cards when at least 3 club members exist
                if (showPodium) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            PodiumCard(
                                entry = topThree[1],
                                podiumColor = PodiumSilver,
                                elevated = false,
                                modifier = Modifier.weight(1f)
                            )
                            PodiumCard(
                                entry = topThree[0],
                                podiumColor = PodiumGold,
                                elevated = true,
                                modifier = Modifier.weight(1.1f)
                            )
                            PodiumCard(
                                entry = topThree[2],
                                podiumColor = PodiumBronze,
                                elevated = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Remaining rows (or rows 1..2 if club has fewer than 3 members)
                items(listEntries, key = { "${state.selectedCategory.name}_${it.memberId}" }) { entry ->
                    LeaderboardRankRow(entry = entry)
                }
            }
        }
    }

    if (showPastWinnersDialog) {
        AlertDialog(
            onDismissRequest = { showPastWinnersDialog = false },
            title = { Text("Past Monthly Winners") },
            text = {
                if (state.historyItems.isEmpty()) {
                    Text(
                        text = "Monthly winner snapshots are archived by club admins at the end of each month.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.historyItems.take(8).forEachIndexed { idx, winner ->
                            if (idx > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = winner.winnerName.ifBlank { "Champion" },
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${winner.monthKey} • ${winner.category}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = winner.scoreLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPastWinnersDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun PodiumCard(
    entry: LeaderboardEntry,
    podiumColor: Color,
    elevated: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("podium_rank_${entry.rank}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, podiumColor.copy(alpha = if (elevated) 0.85f else 0.45f))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(podiumColor.copy(alpha = 0.16f), Color.Transparent)
                    )
                )
                .padding(horizontal = 10.dp, vertical = if (elevated) 18.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (entry.isMonthlyWinner) {
                Icon(
                    imageVector = Icons.Rounded.WorkspacePremium,
                    contentDescription = "Monthly Winner",
                    tint = podiumColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Box(contentAlignment = Alignment.BottomCenter) {
                MemberAvatar(
                    fullName = entry.memberName,
                    avatarUri = entry.avatarUrl,
                    size = if (elevated) 62.dp else 50.dp,
                    borderColor = podiumColor
                )
                Surface(
                    shape = CircleShape,
                    color = podiumColor
                ) {
                    Text(
                        text = "#${entry.rank}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0B0E14),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = entry.memberName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.primaryMetricText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = podiumColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entry.secondaryStatText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LeaderboardRankRow(
    entry: LeaderboardEntry,
    modifier: Modifier = Modifier
) {
    val isUser = entry.isCurrentUser
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("leaderboard_row_${entry.rank}"),
        shape = MaterialTheme.shapes.medium,
        color = if (isUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isUser) 1.5.dp else 1.dp,
            color = if (isUser) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${entry.rank}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            MemberAvatar(
                fullName = entry.memberName,
                avatarUri = entry.avatarUrl,
                size = 42.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.memberName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isUser) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "YOU",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = entry.secondaryStatText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.primaryMetricText,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
