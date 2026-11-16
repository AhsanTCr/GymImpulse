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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.database.PaymentRequestDoc
import com.example.models.AdminMemberSummary
import com.example.models.MemberActivitySegment
import com.example.models.MembershipStatus
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MemberAvatar
import com.example.ui.components.MembershipStatusBadge
import com.example.ui.theme.LocalGymSemanticColors

@Composable
fun AdminMemberInsightsScreen(
    state: AdminUiState,
    onSelectSegment: (MemberActivitySegment) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onConfirmPayment: (
        member: AdminMemberSummary,
        pendingRequest: PaymentRequestDoc?,
        year: Int,
        monthOneBased: Int,
        dayOfMonth: Int,
        amountUsd: Double,
        method: String,
        reference: String
    ) -> Unit,
    onRejectRequest: (AdminMemberSummary, PaymentRequestDoc, String) -> Unit,
    onUpdateMembershipStatus: (AdminMemberSummary, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalGymSemanticColors.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Member Activity & Directory, 1 = Payments & Overdue
    var selectedMemberForDetail by remember { mutableStateOf<AdminMemberSummary?>(null) }
    var paymentTarget by remember { mutableStateOf<Pair<AdminMemberSummary, PaymentRequestDoc?>?>(null) }

    val overdueAndPendingMembers = remember(state.filteredMembers) {
        state.filteredMembers.filter { summary ->
            summary.derivedStatus == MembershipStatus.PAYMENT_OVERDUE ||
                summary.derivedStatus == MembershipStatus.EXPIRED ||
                summary.derivedStatus == MembershipStatus.EXPIRING_SOON ||
                summary.paymentRequests.any { !it.isVerified && it.status == "PENDING_VERIFICATION" }
        }
    }

    val displayedMembers = if (selectedTab == 0) state.filteredMembers else overdueAndPendingMembers

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag("members_tab_directory"),
                    text = { Text("Activity & Roster (${state.filteredMembers.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("members_tab_billing"),
                    text = { Text("Billing Queue (${overdueAndPendingMembers.size})") }
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search name, member ID, or email") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_member_search_input")
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MemberActivitySegment.entries.forEach { segment ->
                            FilterChip(
                                selected = state.selectedSegment == segment,
                                onClick = { onSelectSegment(segment) },
                                modifier = Modifier.testTag("admin_segment_${segment.name.lowercase()}"),
                                label = { Text(segment.label) }
                            )
                        }
                    }
                }

                if (displayedMembers.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = if (selectedTab == 0) Icons.Rounded.Group else Icons.Rounded.Payments,
                            title = if (selectedTab == 0) "No Matching Members" else "Billing Queue Clear",
                            subtitle = if (selectedTab == 0) "Try clearing the search query or selecting All Members."
                            else "No overdue memberships or unverified payment requests match this filter."
                        )
                    }
                } else {
                    items(displayedMembers, key = { it.userId }) { summary ->
                        val pendingReq = summary.paymentRequests.firstOrNull {
                            !it.isVerified && it.status == "PENDING_VERIFICATION"
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_member_card_${summary.profile.memberCode}"),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (pendingReq != null) semantic.warningAmber.copy(alpha = 0.65f)
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            onClick = { selectedMemberForDetail = summary }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    MemberAvatar(
                                        fullName = summary.profile.fullName,
                                        avatarUri = summary.profile.avatarData,
                                        size = 48.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = summary.profile.fullName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${summary.profile.memberCode} • Last visit: ${summary.lastCheckInDateKey ?: "Never"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    MembershipStatusBadge(status = summary.derivedStatus)
                                }

                                // Activity Metrics Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Streak: ${summary.currentStreakDays}d",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = semantic.warningAmber
                                    )
                                    Text(
                                        text = "7d: ${summary.visitsLast7Days} • 30d: ${summary.visitsLast30Days}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Renews: ${summary.membership?.nextRenewalDate ?: "--"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Segment Badges
                                val displaySegments = summary.activitySegments
                                    .filter { it != MemberActivitySegment.ALL }
                                    .take(3)
                                if (displaySegments.isNotEmpty() || pendingReq != null) {
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (pendingReq != null) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = semantic.warningAmberContainer
                                            ) {
                                                Text(
                                                    text = "PAYMENT PENDING: ${pendingReq.amountFormatted}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = semantic.warningAmber,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        displaySegments.forEach { seg ->
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = seg.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (selectedTab == 1 || pendingReq != null) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { paymentTarget = summary to pendingReq },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(if (pendingReq != null) "Confirm & Activate" else "Record Payment")
                                        }
                                        OutlinedButton(
                                            onClick = { selectedMemberForDetail = summary },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Full Details")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }

    selectedMemberForDetail?.let { member ->
        // Refresh from live list so updates show immediately
        val liveMember = state.allMembers.firstOrNull { it.userId == member.userId } ?: member
        MemberDetailAdminDialog(
            member = liveMember,
            onDismiss = { selectedMemberForDetail = null },
            onOpenConfirmPayment = { req ->
                paymentTarget = liveMember to req
            },
            onRejectRequest = { req, reason ->
                onRejectRequest(liveMember, req, reason)
            },
            onChangeStatus = { newStatus ->
                onUpdateMembershipStatus(liveMember, newStatus)
            }
        )
    }

    paymentTarget?.let { (member, pendingReq) ->
        ConfirmPaymentCycleDialog(
            member = member,
            pendingRequest = pendingReq,
            onDismiss = { paymentTarget = null },
            onConfirm = { y, m, d, amt, method, ref ->
                onConfirmPayment(member, pendingReq, y, m, d, amt, method, ref)
                paymentTarget = null
            }
        )
    }
}
