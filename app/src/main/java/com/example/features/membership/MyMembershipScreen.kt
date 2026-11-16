package com.example.features.membership

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.models.MembershipStatus
import com.example.models.PaymentMethod
import com.example.models.PaymentRecord
import com.example.models.PaymentRequestItem
import com.example.ui.components.ApexSubScreenTopBar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.InfoCalloutBanner
import com.example.ui.components.MembershipStatusBadge
import com.example.ui.components.PrimaryAthleticButton
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalGymSemanticColors
import com.example.utils.DateTimeUtils

@Composable
fun MyMembershipScreen(
    viewModel: MembershipViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val overview = state.overview
    val semantic = LocalGymSemanticColors.current
    var showPaymentModal by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.UPI_INSTANT) }
    var referenceNote by remember { mutableStateOf("") }

    val nextCycleExample = remember {
        DateTimeUtils.calculateCycleFromPaymentDate(2026, 12, 4)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
        ) {
            ApexSubScreenTopBar(
                title = "My Membership",
                subtitle = overview?.planName ?: "Club Membership",
                onBackClick = onNavigateBack
            )

            if (state.isLoading && overview == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (overview == null) {
                Box(modifier = Modifier.padding(24.dp)) {
                    EmptyStateView(
                        icon = Icons.Rounded.CreditCard,
                        title = "Membership Unavailable",
                        subtitle = state.errorMessage ?: "Please check your connection and try again."
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Status Card & Cycle Details
                    item {
                        val accent = when (overview.status) {
                            MembershipStatus.ACTIVE -> semantic.activeGreen
                            MembershipStatus.EXPIRING_SOON -> semantic.warningAmber
                            MembershipStatus.EXPIRED, MembershipStatus.PAYMENT_OVERDUE -> semantic.alertCoral
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.5.dp, accent.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(accent.copy(alpha = 0.14f), MaterialTheme.colorScheme.surface)
                                        )
                                    )
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = overview.planName,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = overview.monthlyFeeFormatted,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    MembershipStatusBadge(status = overview.status)
                                }

                                Text(
                                    text = overview.status.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                MembershipDetailRow("Membership Start Date", overview.membershipStartDate)
                                MembershipDetailRow("Current Cycle", overview.currentCycle.cycleLabel)
                                MembershipDetailRow("Next Renewal Date", overview.nextRenewalDate)
                                MembershipDetailRow(
                                    "Auto-Renew",
                                    if (overview.autoRenewEnabled) "Enabled" else "Manual Renewal"
                                )
                            }
                        }
                    }

                    // Payment-Date Anchored Cycle Rule Callout (Dec 4 -> Jan 4)
                    item {
                        InfoCalloutBanner(
                            title = "Payment-Date Membership Cycles",
                            description = "Cycles follow your exact payment date. For example, paying on ${nextCycleExample.first} starts your next cycle from ${nextCycleExample.first} to ${nextCycleExample.second}.",
                            icon = Icons.Rounded.CalendarMonth,
                            accentColor = semantic.infoCyan,
                            containerColor = semantic.infoCyanContainer
                        )
                    }

                    // Payment Notification Card (Admin-verified security workflow)
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("membership_payment_action_card"),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CreditCard,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Membership Renewal & Payment",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Paid at reception, via bank transfer, or by card? Notify the club desk with your reference so an admin verifies and activates your cycle.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (!state.submissionMessage.isNullOrBlank()) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = semantic.activeGreenContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = semantic.activeGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = state.submissionMessage.orEmpty(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = semantic.activeGreen
                                            )
                                        }
                                    }
                                }

                                if (!state.errorMessage.isNullOrBlank()) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = semantic.alertCoralContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = state.errorMessage.orEmpty(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = semantic.alertCoral,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }

                                PrimaryAthleticButton(
                                    text = when (overview.status) {
                                        MembershipStatus.ACTIVE -> "Submit Early Renewal Notification (${overview.monthlyFeeFormatted.substringBefore(" /")})"
                                        MembershipStatus.EXPIRING_SOON -> "Submit Renewal Payment (${overview.monthlyFeeFormatted.substringBefore(" /")})"
                                        MembershipStatus.EXPIRED, MembershipStatus.PAYMENT_OVERDUE -> "Submit Payment Notification"
                                    },
                                    icon = Icons.Rounded.CreditCard,
                                    enabled = !state.isSubmittingPayment,
                                    testTag = "membership_pay_now_button",
                                    onClick = { showPaymentModal = true }
                                )
                            }
                        }
                    }

                    // Pending / Submitted Payment Notifications
                    if (overview.paymentRequests.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(2.dp))
                            SectionHeader(title = "Payment Verification Requests")
                        }
                        items(overview.paymentRequests, key = { it.id }) { req ->
                            PaymentRequestCard(request = req)
                        }
                    }

                    // Verified Payment History
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        SectionHeader(title = "Verified Payment History")
                    }

                    if (overview.paymentHistory.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                                title = "No Verified Receipts Yet",
                                subtitle = "Once your membership payment is verified by a club admin, official receipts appear here."
                            )
                        }
                    } else {
                        items(overview.paymentHistory, key = { it.id }) { payment ->
                            PaymentHistoryItemCard(payment = payment)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showPaymentModal) {
        AlertDialog(
            onDismissRequest = { showPaymentModal = false },
            icon = { Icon(Icons.Rounded.CreditCard, contentDescription = null) },
            title = { Text("Submit Payment Notification") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Select the method you used to pay ${overview?.monthlyFeeFormatted ?: "your fee"}. Your membership updates once verified by club admin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentMethod.entries.forEach { method ->
                            FilterChip(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                label = { Text(method.label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = referenceNote,
                        onValueChange = { referenceNote = it },
                        label = { Text("Transaction Ref / Receipt Note (Optional)") },
                        placeholder = { Text("e.g. UPI-994182 or Cash Desk") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_reference_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.submitPaymentNotification(
                            paymentMethod = selectedMethod.label,
                            referenceNote = referenceNote
                        )
                        referenceNote = ""
                        showPaymentModal = false
                    },
                    modifier = Modifier.testTag("payment_confirm_submit_button")
                ) {
                    Text("Submit Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MembershipDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PaymentRequestCard(request: PaymentRequestItem) {
    val semantic = LocalGymSemanticColors.current
    val isPending = !request.isVerified && request.status.contains("PENDING", ignoreCase = true)
    val accent = when {
        request.isVerified -> semantic.activeGreen
        isPending -> semantic.warningAmber
        else -> semantic.alertCoral
    }
    val container = when {
        request.isVerified -> semantic.activeGreenContainer
        isPending -> semantic.warningAmberContainer
        else -> semantic.alertCoralContainer
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = container,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.paymentMethod,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Submitted ${request.paymentDateLabel}" +
                        (request.referenceNote?.let { " • Ref: $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = request.amountFormatted,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = request.status.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
        }
    }
}

@Composable
private fun PaymentHistoryItemCard(payment: PaymentRecord) {
    val semantic = LocalGymSemanticColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = semantic.activeGreenContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                        contentDescription = null,
                        tint = semantic.activeGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cycle: ${payment.cycleCovered}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Paid ${payment.paymentDate} • ${payment.method.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ref: ${payment.referenceNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = payment.amountFormatted,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (payment.isVerified) "PAID" else "PENDING",
                    style = MaterialTheme.typography.labelSmall,
                    color = semantic.activeGreen
                )
            }
        }
    }
}
