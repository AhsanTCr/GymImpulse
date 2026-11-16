package com.example.features.admin

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.database.CompetitionConfigDoc
import com.example.models.LeaderboardCategory
import com.example.models.LeaderboardEntry
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MemberAvatar
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalGymSemanticColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AdminCompetitionControlScreen(
    state: AdminUiState,
    onSelectCategory: (LeaderboardCategory) -> Unit,
    onSaveCompetitionConfig: (CompetitionConfigDoc) -> Unit,
    onPublishMonthlyWinner: (monthKey: String, categoryTitle: String, winnerUid: String, winnerName: String, scoreLabel: String) -> Unit,
    onAdjustMemberScore: (uid: String, name: String, streak: Long, visits: Long, habits: Long, referrals: Long, reason: String) -> Unit,
    onResolveReferral: (com.example.database.ReferralRecordDoc, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalGymSemanticColors.current
    val activeConfig = state.competitionConfigs[state.selectedCompCategory]
    val pendingReferrals = remember(state.referrals) {
        state.referrals.filter { !it.isVerified && it.status == "PENDING_VERIFICATION" }
    }

    var showEditConfigDialog by remember { mutableStateOf(false) }
    var adjustTarget by remember { mutableStateOf<LeaderboardEntry?>(null) }
    var winnerTarget by remember { mutableStateOf<LeaderboardEntry?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 4-Competition Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LeaderboardCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = state.selectedCompCategory == cat,
                            onClick = { onSelectCategory(cat) },
                            modifier = Modifier.testTag("admin_comp_tab_${cat.name.lowercase()}"),
                            label = { Text(cat.shortTitle) }
                        )
                    }
                }
            }

            // Competition Config Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeConfig?.periodLabel?.uppercase() ?: "ACTIVE CYCLE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = semantic.accentVolt
                                )
                                Text(
                                    text = activeConfig?.title ?: state.selectedCompCategory.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            OutlinedButton(
                                onClick = { showEditConfigDialog = true },
                                modifier = Modifier.testTag("admin_comp_edit_config_button")
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Configure")
                            }
                        }
                        Text(
                            text = activeConfig?.targetDescription ?: state.selectedCompCategory.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Target: ${activeConfig?.targetValue ?: 0}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Reward: ${activeConfig?.rewardTitle ?: "Club Prize"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = semantic.warningAmber
                            )
                        }
                    }
                }
            }

            // Pending Referral Verifications (Module 3 requirement)
            if (pendingReferrals.isNotEmpty()) {
                item {
                    SectionHeader(title = "Pending Referral Claims (${pendingReferrals.size})")
                }
                items(pendingReferrals, key = { it.id }) { ref ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = semantic.infoCyanContainer,
                        border = BorderStroke(1.dp, semantic.infoCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = semantic.infoCyan)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${ref.referredName} joined via ${ref.referrerCode}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Verify once referred member completes front-desk registration.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onResolveReferral(ref, true) },
                                    modifier = Modifier.testTag("admin_verify_ref_${ref.id}")
                                ) {
                                    Text("Verify")
                                }
                                OutlinedButton(onClick = { onResolveReferral(ref, false) }) {
                                    Text("Deny")
                                }
                            }
                        }
                    }
                }
            }

            // Live Rankings & Manual Override / Winner Publishing
            item {
                SectionHeader(title = "Live Standings & Controls")
            }

            if (state.leaderboardRankings.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Rounded.EmojiEvents,
                        title = "No Standings Yet",
                        subtitle = "Member check-ins and nutrition logs populate standings in real time."
                    )
                }
            } else {
                items(state.leaderboardRankings, key = { "${state.selectedCompCategory.name}_${it.memberId}" }) { entry ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_comp_rank_${entry.rank}"),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "#${entry.rank}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                MemberAvatar(fullName = entry.memberName, avatarUri = entry.avatarUrl, size = 40.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.memberName, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = entry.secondaryStatText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = entry.primaryMetricText,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = semantic.warningAmber
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { adjustTarget = entry },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Adjust Score")
                                }
                                Button(
                                    onClick = { winnerTarget = entry },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text("Crown Winner")
                                }
                            }
                        }
                    }
                }
            }

            // Recent Audit Logs
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionHeader(title = "Competition & Admin Audit Trail")
            }

            if (state.auditLogs.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Rounded.History,
                        title = "No Audit Entries Yet",
                        subtitle = "Every payment verification, score adjustment, and published winner is logged here."
                    )
                }
            } else {
                items(state.auditLogs.take(10), key = { it.id }) { log ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "${log.actionType} • ${log.summary}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!log.details.isNullOrBlank()) {
                                Text(
                                    text = log.details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditConfigDialog && activeConfig != null) {
        var periodInput by remember(activeConfig) { mutableStateOf(activeConfig.periodLabel) }
        var descInput by remember(activeConfig) { mutableStateOf(activeConfig.targetDescription) }
        var targetInput by remember(activeConfig) { mutableStateOf(activeConfig.targetValue.toString()) }
        var rewardInput by remember(activeConfig) { mutableStateOf(activeConfig.rewardTitle) }

        AlertDialog(
            onDismissRequest = { showEditConfigDialog = false },
            title = { Text("Configure ${activeConfig.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = periodInput,
                        onValueChange = { periodInput = it },
                        label = { Text("Competition Period (e.g. Dec 2026)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Target Description") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetInput,
                        onValueChange = { targetInput = it },
                        label = { Text("Goal Numeric Target") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rewardInput,
                        onValueChange = { rewardInput = it },
                        label = { Text("Winner Reward") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveCompetitionConfig(
                            activeConfig.copy(
                                periodLabel = periodInput.ifBlank { activeConfig.periodLabel },
                                targetDescription = descInput.ifBlank { activeConfig.targetDescription },
                                targetValue = targetInput.toLongOrNull() ?: activeConfig.targetValue,
                                rewardTitle = rewardInput.ifBlank { activeConfig.rewardTitle }
                            )
                        )
                        showEditConfigDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditConfigDialog = false }) { Text("Cancel") }
            }
        )
    }

    adjustTarget?.let { target ->
        val rawStat = state.leaderboardStats.firstOrNull { it.userId == target.memberId }
        var streakStr by remember(target) { mutableStateOf((rawStat?.currentStreakDays ?: 0L).toString()) }
        var visitsStr by remember(target) { mutableStateOf((rawStat?.monthlyVisits ?: 0L).toString()) }
        var habitsStr by remember(target) { mutableStateOf((rawStat?.healthyHabitsScore ?: 0L).toString()) }
        var refsStr by remember(target) { mutableStateOf((rawStat?.verifiedReferralsCount ?: 0L).toString()) }
        var reasonStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { adjustTarget = null },
            title = { Text("Audited Score Override: ${target.memberName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = streakStr,
                            onValueChange = { streakStr = it },
                            label = { Text("Streak") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = visitsStr,
                            onValueChange = { visitsStr = it },
                            label = { Text("Visits") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = habitsStr,
                            onValueChange = { habitsStr = it },
                            label = { Text("Habits %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = refsStr,
                            onValueChange = { refsStr = it },
                            label = { Text("Referrals") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider()
                    OutlinedTextField(
                        value = reasonStr,
                        onValueChange = { reasonStr = it },
                        label = { Text("Audit Reason (Required)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAdjustMemberScore(
                            target.memberId,
                            target.memberName,
                            streakStr.toLongOrNull() ?: 0L,
                            visitsStr.toLongOrNull() ?: 0L,
                            habitsStr.toLongOrNull() ?: 0L,
                            refsStr.toLongOrNull() ?: 0L,
                            reasonStr.ifBlank { "Manual correction at desk" }
                        )
                        adjustTarget = null
                    }
                ) { Text("Save & Log") }
            },
            dismissButton = {
                TextButton(onClick = { adjustTarget = null }) { Text("Cancel") }
            }
        )
    }

    winnerTarget?.let { winner ->
        val defaultMonthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
        var monthKeyInput by remember { mutableStateOf(defaultMonthKey) }
        AlertDialog(
            onDismissRequest = { winnerTarget = null },
            title = { Text("Publish Monthly Winner") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Archive ${winner.memberName} (${winner.primaryMetricText}) as official winner for ${state.selectedCompCategory.title}.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = monthKeyInput,
                        onValueChange = { monthKeyInput = it },
                        label = { Text("Month Key (YYYY-MM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPublishMonthlyWinner(
                            monthKeyInput.ifBlank { defaultMonthKey },
                            state.selectedCompCategory.title,
                            winner.memberId,
                            winner.memberName,
                            winner.primaryMetricText
                        )
                        winnerTarget = null
                    }
                ) { Text("Publish Winner") }
            },
            dismissButton = {
                TextButton(onClick = { winnerTarget = null }) { Text("Cancel") }
            }
        )
    }
}
