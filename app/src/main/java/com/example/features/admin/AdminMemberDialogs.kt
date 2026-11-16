package com.example.features.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.config.AppConfig
import com.example.database.PaymentRequestDoc
import com.example.models.AdminMemberSummary
import com.example.ui.components.MembershipStatusBadge
import com.example.ui.theme.LocalGymSemanticColors
import com.example.utils.DateTimeUtils
import java.util.Calendar

@Composable
fun MemberDetailAdminDialog(
    member: AdminMemberSummary,
    onDismiss: () -> Unit,
    onOpenConfirmPayment: (PaymentRequestDoc?) -> Unit,
    onRejectRequest: (PaymentRequestDoc, String) -> Unit,
    onChangeStatus: (String) -> Unit
) {
    val semantic = LocalGymSemanticColors.current
    val pendingReqs = member.paymentRequests.filter { !it.isVerified && it.status == "PENDING_VERIFICATION" }
    var rejectReason by remember { mutableStateOf("Receipt not matched") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(member.profile.fullName, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${member.profile.memberCode} • ${member.profile.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MembershipStatusBadge(status = member.derivedStatus)
                    Text(
                        text = "${member.totalVisits} total visits",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Cycle: ${member.membership?.cycleStartDate ?: "--"} – ${member.membership?.cycleEndDate ?: "--"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Phone: ${member.profile.phone ?: "Not added"} • Joined ${member.profile.joinedDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Streak: ${member.currentStreakDays}d (Best ${member.longestStreakDays}d) • 7d: ${member.visitsLast7Days} • 30d: ${member.visitsLast30Days}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = { onOpenConfirmPayment(pendingReqs.firstOrNull()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_detail_confirm_payment_button")
                ) {
                    Text("Confirm Payment & Renew Cycle")
                }

                // Quick Status Controls
                Text("Membership Status Control", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ACTIVE", "EXPIRING_SOON", "PAYMENT_OVERDUE", "EXPIRED").forEach { st ->
                        OutlinedButton(
                            onClick = { onChangeStatus(st) },
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when (st) {
                                    "ACTIVE" -> "Active"
                                    "EXPIRING_SOON" -> "Soon"
                                    "PAYMENT_OVERDUE" -> "Overdue"
                                    else -> "Expired"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                if (pendingReqs.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "Pending Payment Requests (${pendingReqs.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = semantic.warningAmber
                    )
                    pendingReqs.forEach { req ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = semantic.warningAmberContainer,
                            border = BorderStroke(1.dp, semantic.warningAmber.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${req.amountFormatted} • ${req.paymentMethod} (${req.paymentDateLabel})",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                if (!req.referenceNote.isNullOrBlank()) {
                                    Text("Ref: ${req.referenceNote}", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onOpenConfirmPayment(req) },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Verify") }
                                    OutlinedButton(
                                        onClick = { onRejectRequest(req, rejectReason) },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Reject", color = semantic.alertCoral) }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()
                Text("Recent Payments (${member.verifiedPayments.size})", style = MaterialTheme.typography.labelLarge)
                if (member.verifiedPayments.isEmpty()) {
                    Text(
                        text = "No verified payments recorded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    member.verifiedPayments.take(4).forEach { pay ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${pay.paymentDate} (${pay.method})", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = pay.amountFormatted,
                                style = MaterialTheme.typography.labelMedium,
                                color = semantic.activeGreen
                            )
                        }
                    }
                }

                HorizontalDivider()
                Text("Recent Check-Ins (${member.attendanceRecords.size})", style = MaterialTheme.typography.labelLarge)
                if (member.attendanceRecords.isEmpty()) {
                    Text(
                        text = "No QR entrance scans yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    member.attendanceRecords.take(5).forEach { att ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(att.dateLabel, style = MaterialTheme.typography.bodySmall)
                            Text(att.checkInTime, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ConfirmPaymentCycleDialog(
    member: AdminMemberSummary,
    pendingRequest: PaymentRequestDoc?,
    onDismiss: () -> Unit,
    onConfirm: (
        year: Int,
        monthOneBased: Int,
        dayOfMonth: Int,
        amountUsd: Double,
        method: String,
        reference: String
    ) -> Unit
) {
    val now = Calendar.getInstance()
    var year by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(now.get(Calendar.MONTH) + 1) }
    var day by remember { mutableIntStateOf(now.get(Calendar.DAY_OF_MONTH)) }
    var amountInput by remember {
        mutableStateOf(
            member.membership?.monthlyFeeUsd?.toInt()?.toString()
                ?: AppConfig.DEFAULT_MONTHLY_FEE_USD.toInt().toString()
        )
    }
    var methodInput by remember { mutableStateOf(pendingRequest?.paymentMethod ?: "Front Desk Cash / Card") }
    var refInput by remember { mutableStateOf(pendingRequest?.referenceNote ?: "") }

    val (previewStart, previewEnd) = remember(year, month, day) {
        val safeYear = year.coerceIn(2024, 2035)
        val safeMonth = month.coerceIn(1, 12)
        val safeDay = day.coerceIn(1, 28)
        DateTimeUtils.calculateCycleFromPaymentDate(safeYear, safeMonth, safeDay)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Payment & Cycle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Member: ${member.profile.fullName} (${member.profile.memberCode})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Select Actual Payment Date (Cycle anchors to this day):",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = year.toString(),
                        onValueChange = { year = it.toIntOrNull() ?: year },
                        label = { Text("Year") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1.1f)
                    )
                    OutlinedTextField(
                        value = month.toString(),
                        onValueChange = { month = (it.toIntOrNull() ?: month).coerceIn(1, 12) },
                        label = { Text("Mon") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.9f)
                    )
                    OutlinedTextField(
                        value = day.toString(),
                        onValueChange = { day = (it.toIntOrNull() ?: day).coerceIn(1, 31) },
                        label = { Text("Day") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("confirm_payment_day_input")
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "ACTIVE CYCLE PREVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$previewStart → $previewEnd",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Amount (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = methodInput,
                    onValueChange = { methodInput = it },
                    label = { Text("Payment Method") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = refInput,
                    onValueChange = { refInput = it },
                    label = { Text("Receipt / Reference Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountInput.toDoubleOrNull() ?: AppConfig.DEFAULT_MONTHLY_FEE_USD
                    onConfirm(
                        year.coerceIn(2024, 2035),
                        month.coerceIn(1, 12),
                        day.coerceIn(1, 31),
                        amt,
                        methodInput.ifBlank { "Front Desk" },
                        refInput
                    )
                },
                modifier = Modifier.testTag("confirm_payment_submit_button")
            ) {
                Text("Activate Membership")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
