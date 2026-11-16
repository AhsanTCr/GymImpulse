package com.example.features.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.models.AdminDashboardModule
import com.example.models.AdminOverviewPeriod
import com.example.models.DailyAttendanceTrendPoint
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MetricStatCard
import com.example.ui.components.QuickAccessCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalGymSemanticColors

@Composable
fun AdminOverviewScreen(
    state: AdminUiState,
    onSelectPeriod: (AdminOverviewPeriod) -> Unit,
    onNavigateToModule: (AdminDashboardModule) -> Unit,
    onOpenQrManagement: () -> Unit,
    onUpdateAppVersion: (latestVersion: String, releaseDate: String, highlightsRaw: String, githubRepo: String, downloadUrl: String) -> Unit,
    onExportJson: () -> Unit,
    onExportMembersCsv: () -> Unit,
    onExportAttendanceCsv: () -> Unit,
    onPreviewInboundSync: (String) -> Unit,
    onClearSyncDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalGymSemanticColors.current
    val context = LocalContext.current
    val metrics = state.overviewMetrics

    var showAppConfigDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date Filters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminOverviewPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = state.selectedPeriod == period,
                            onClick = { onSelectPeriod(period) },
                            modifier = Modifier.testTag("admin_period_${period.name.lowercase()}"),
                            label = { Text(period.label) }
                        )
                    }
                }
            }

            // 4 Core Club KPIs
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricStatCard(
                            label = "Registered Members",
                            value = "${metrics.totalMembers}",
                            caption = "${metrics.activeMembers} active cycle",
                            icon = Icons.Rounded.Group,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("overview_kpi_total")
                        )
                        MetricStatCard(
                            label = "Today's Attendance",
                            value = "${metrics.todayAttendanceCount}",
                            caption = "${metrics.attendanceRatePercent}% club check-in rate",
                            icon = Icons.Rounded.HowToReg,
                            accentColor = semantic.activeGreen,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("overview_kpi_attendance")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricStatCard(
                            label = "Expiring Soon",
                            value = "${metrics.expiringSoonMembers}",
                            caption = "${metrics.pendingPaymentRequestsCount} payment reqs pending",
                            icon = Icons.Rounded.Schedule,
                            accentColor = semantic.warningAmber,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("overview_kpi_expiring")
                        )
                        MetricStatCard(
                            label = "Expired / Overdue",
                            value = "${metrics.expiredMembers + metrics.overdueMembers}",
                            caption = "${metrics.overdueMembers} overdue • ${metrics.expiredMembers} expired",
                            icon = Icons.Rounded.ErrorOutline,
                            accentColor = semantic.alertCoral,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("overview_kpi_expired")
                        )
                    }
                }
            }

            // Real Firestore Attendance Chart
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Attendance Trend",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${metrics.periodAttendanceTotal} verified check-ins (${state.selectedPeriod.label})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = semantic.activeGreenContainer
                            ) {
                                Text(
                                    text = "${metrics.attendanceRatePercent}% TODAY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = semantic.activeGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        AdminAttendanceBarChart(points = metrics.attendanceTrend)
                    }
                }
            }

            // Important Alerts
            item {
                SectionHeader(title = "Important Club Alerts")
            }

            if (metrics.alerts.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Rounded.CheckCircle,
                        title = "All Operations Clear",
                        subtitle = "No overdue accounts, pending payment verifications, or QR security warnings."
                    )
                }
            } else {
                items(metrics.alerts, key = { it.id }) { alert ->
                    val fg = when (alert.severity) {
                        "CRITICAL" -> semantic.alertCoral
                        "WARNING" -> semantic.warningAmber
                        else -> semantic.infoCyan
                    }
                    val bg = when (alert.severity) {
                        "CRITICAL" -> semantic.alertCoralContainer
                        "WARNING" -> semantic.warningAmberContainer
                        else -> semantic.infoCyanContainer
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("overview_alert_${alert.id}"),
                        shape = MaterialTheme.shapes.medium,
                        color = bg,
                        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f)),
                        onClick = {
                            if (alert.id == "alert_qr_events") onOpenQrManagement()
                            else onNavigateToModule(alert.targetModule)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = alert.severity,
                                tint = fg
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = alert.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = alert.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Operations Tools: Entrance QR, Remote App Updates, External Sync Bridge
            item {
                SectionHeader(title = "Club Infrastructure & Sync")
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickAccessCard(
                            title = "Entrance QR Terminal",
                            subtitle = if (state.gymConfig.isQrEnabled) "Active • ${state.gymConfig.rotationWindowSeconds}s" else "Paused",
                            icon = Icons.Rounded.QrCode2,
                            accentColor = MaterialTheme.colorScheme.primary,
                            testTag = "overview_tool_qr",
                            onClick = onOpenQrManagement,
                            modifier = Modifier.weight(1f)
                        )
                        QuickAccessCard(
                            title = "App Version Config",
                            subtitle = "Latest v${state.appConfig.latestVersion}",
                            icon = Icons.Rounded.SystemUpdate,
                            accentColor = semantic.activeGreen,
                            testTag = "overview_tool_app_version",
                            onClick = { showAppConfigDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    QuickAccessCard(
                        title = "External Gym Software Sync Bridge",
                        subtitle = "Export deduplicated JSON / CSV or validate laptop sync bundles",
                        icon = Icons.Rounded.CloudSync,
                        accentColor = semantic.infoCyan,
                        testTag = "overview_tool_external_sync",
                        onClick = { showSyncDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Recent Member Activity
            item {
                SectionHeader(
                    title = "Recent Member Activity",
                    actionLabel = "All Members",
                    onActionClick = { onNavigateToModule(AdminDashboardModule.MEMBER_INSIGHTS) }
                )
            }

            if (state.recentActivity.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Rounded.HowToReg,
                        title = "No Recent Activity",
                        subtitle = "Member check-ins and payment verifications will appear here in real time."
                    )
                }
            } else {
                items(state.recentActivity, key = { it.id }) { item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (item.isAlert) semantic.warningAmberContainer else semantic.activeGreenContainer
                            ) {
                                Text(
                                    text = item.badgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.isAlert) semantic.warningAmber else semantic.activeGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAppConfigDialog) {
        var verInput by remember(state.appConfig) { mutableStateOf(state.appConfig.latestVersion) }
        var dateInput by remember(state.appConfig) { mutableStateOf(state.appConfig.releaseDate) }
        var notesInput by remember(state.appConfig) {
            mutableStateOf(state.appConfig.releaseHighlights.joinToString("\n"))
        }
        var repoInput by remember(state.appConfig) {
            mutableStateOf(state.appConfig.githubRepo.orEmpty().ifBlank { com.example.config.AppConfig.DEFAULT_GITHUB_REPO })
        }
        var downloadUrlInput by remember(state.appConfig) {
            mutableStateOf(state.appConfig.downloadUrl.orEmpty())
        }
        AlertDialog(
            onDismissRequest = { showAppConfigDialog = false },
            title = { Text("Publish App Version Notice") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = verInput,
                        onValueChange = { verInput = it },
                        label = { Text("Latest Version (e.g. 1.1.0)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("Release Date Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = repoInput,
                        onValueChange = { repoInput = it },
                        label = { Text("GitHub Repo (owner/repo, optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = downloadUrlInput,
                        onValueChange = { downloadUrlInput = it },
                        label = { Text("Direct APK / Release URL (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Release Highlights (1 per line)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateAppVersion(verInput, dateInput, notesInput, repoInput, downloadUrlInput)
                        showAppConfigDialog = false
                    }
                ) { Text("Publish") }
            },
            dismissButton = {
                TextButton(onClick = { showAppConfigDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSyncDialog) {
        var inboundText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                onClearSyncDialog()
                showSyncDialog = false
            },
            title = { Text("Gym Software Sync Compatibility") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Generate standardized, deduplicated exports or validate inbound payloads from front-desk laptop software.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = onExportJson, modifier = Modifier.weight(1f)) {
                            Text("JSON")
                        }
                        OutlinedButton(onClick = onExportMembersCsv, modifier = Modifier.weight(1f)) {
                            Text("Roster CSV")
                        }
                        OutlinedButton(onClick = onExportAttendanceCsv, modifier = Modifier.weight(1f)) {
                            Text("Scans CSV")
                        }
                    }

                    state.exportedSyncPayload?.let { payload ->
                        OutlinedTextField(
                            value = payload.take(800),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Export Preview (${payload.length} chars)") },
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("Gymimpulse Sync", payload))
                            }
                        ) {
                            Text("Copy Full Sync Bundle")
                        }
                    }

                    OutlinedTextField(
                        value = inboundText,
                        onValueChange = { inboundText = it },
                        label = { Text("Paste Inbound JSON / CSV to Validate") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(onClick = { onPreviewInboundSync(inboundText) }) {
                        Text("Validate Inbound Payload")
                    }
                    state.syncPreview?.let { prev ->
                        Text(
                            text = "${prev.formatDetected}: ${prev.statusNote}",
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.infoCyan
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearSyncDialog()
                        showSyncDialog = false
                    }
                ) { Text("Done") }
            }
        )
    }
}

@Composable
private fun AdminAttendanceBarChart(points: List<DailyAttendanceTrendPoint>) {
    val barColor = MaterialTheme.colorScheme.primary
    val semantic = LocalGymSemanticColors.current
    val maxCount = (points.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val count = points.size.coerceAtLeast(1)
            val slotW = size.width / count
            val barW = (slotW * 0.48f).coerceAtMost(32.dp.toPx())
            val maxH = size.height - 6.dp.toPx()

            points.forEachIndexed { idx, pt ->
                val left = slotW * idx + (slotW - barW) / 2f
                val ratio = (pt.count.toFloat() / maxCount).coerceAtLeast(if (pt.count > 0) 0.12f else 0.05f)
                val barH = maxH * ratio
                drawRoundRect(
                    color = if (idx == points.lastIndex) semantic.activeGreen else barColor.copy(alpha = 0.78f),
                    topLeft = Offset(left, maxH - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            points.forEach { pt ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${pt.count}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = pt.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
