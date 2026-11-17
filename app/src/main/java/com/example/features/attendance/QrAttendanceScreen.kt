package com.example.features.attendance

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.features.qr.QrScannerViewfinder
import com.example.models.AttendanceRecord
import com.example.models.QrScanUiState
import com.example.models.TodayAttendanceState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.InfoCalloutBanner
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalGymSemanticColors

@Composable
fun QrAttendanceScreen(
    viewModel: QrAttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val semantic = LocalGymSemanticColors.current
    val context = LocalContext.current

    var showManualCodeDialog by remember { mutableStateOf(false) }
    var manualPayloadInput by remember { mutableStateOf(state.expectedEntranceCode) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            viewModel.onPermissionResult(granted)
        }
    )

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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "COMMON ENTRANCE CHECK-IN",
                        style = MaterialTheme.typography.labelSmall,
                        color = semantic.accentVolt
                    )
                    Text(
                        text = "QR Attendance",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    val member = state.currentMember
                    if (member != null) {
                        Text(
                            text = "Signed in as ${member.fullName} (${member.memberCode})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Today's Attendance Status Card
            item {
                val checkedIn = state.todayStatus == TodayAttendanceState.CHECKED_IN
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qr_today_status_card"),
                    shape = MaterialTheme.shapes.medium,
                    color = if (checkedIn) semantic.activeGreenContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (checkedIn) semantic.activeGreen.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (checkedIn) Icons.Rounded.CheckCircle else Icons.Rounded.Schedule,
                            contentDescription = state.todayStatus.label,
                            tint = if (checkedIn) semantic.activeGreen else semantic.warningAmber,
                            modifier = Modifier.size(26.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Today's Status: ${state.todayStatus.label}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (checkedIn) "Your attendance is synced for today's streak."
                                else "Point your camera at the reception QR poster when entering the club.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (!state.isQrEnabled) {
                item {
                    InfoCalloutBanner(
                        title = "Entrance QR Temporarily Paused",
                        description = "Club administration has temporarily paused entrance QR check-ins. Contact the front desk for assistance.",
                        icon = Icons.Rounded.ErrorOutline,
                        accentColor = semantic.alertCoral,
                        containerColor = semantic.alertCoralContainer
                    )
                }
            }

            // Large QR Scanner Area + CameraX Preview + Camera Permission State
            item {
                QrScannerViewfinder(
                    hasCameraPermission = hasCameraPermission,
                    isConfirmed = state.scanState is QrScanUiState.Confirmed ||
                        state.todayStatus == TodayAttendanceState.CHECKED_IN,
                    isProcessingScan = state.scanState is QrScanUiState.ScanningEntranceQr,
                    entranceCode = state.expectedEntranceCode,
                    onRequestCameraPermission = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onScanLiveEntranceQr = {
                        viewModel.scanLiveReceptionQr(context)
                    }
                )
            }

            // Optional Reception Code Entry for Desk/Accessibility Fallback
            if (state.allowStaticFallback && state.isQrEnabled) {
                item {
                    OutlinedButton(
                        onClick = {
                            manualPayloadInput = state.expectedEntranceCode
                            showManualCodeDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qr_manual_code_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCode2,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Enter Reception Desk Code Manually")
                    }
                }
            }

            // Scan Error Card (Duplicate Check-In / Expired Screenshot / Unrecognized Code)
            if (state.scanState is QrScanUiState.Error) {
                val error = state.scanState as QrScanUiState.Error
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qr_error_card"),
                        shape = MaterialTheme.shapes.large,
                        color = semantic.alertCoralContainer,
                        border = BorderStroke(1.5.dp, semantic.alertCoral)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = "Scan blocked",
                                tint = semantic.alertCoral,
                                modifier = Modifier.size(30.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Check-In Blocked",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = semantic.alertCoral
                                )
                                Text(
                                    text = error.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            TextButton(onClick = { viewModel.resetScanState() }) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            // Attendance Confirmation State
            if (state.scanState is QrScanUiState.Confirmed) {
                val confirmed = state.scanState as QrScanUiState.Confirmed
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qr_confirmation_card"),
                        shape = MaterialTheme.shapes.large,
                        color = semantic.activeGreenContainer,
                        border = BorderStroke(1.5.dp, semantic.activeGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = "Attendance Confirmed",
                                tint = semantic.activeGreen,
                                modifier = Modifier.size(34.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Attendance Confirmed!",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${confirmed.memberName} (${confirmed.memberCode}) • ${confirmed.timestamp}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Streak synced (${confirmed.updatedStreakDays}d) via ${confirmed.entranceCode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = semantic.activeGreen
                                )
                            }
                        }
                    }
                }
            }

            // Common Entrance QR Instructions
            item {
                InfoCalloutBanner(
                    title = "How Common Entrance QR Works",
                    description = "All members scan the same physical QR code displayed at the Gymimpulse entrance desk. Rotating security tokens and daily duplicate guards protect your club streak.",
                    icon = Icons.Rounded.Info,
                    accentColor = semantic.infoCyan,
                    containerColor = semantic.infoCyanContainer
                )
            }

            // Recent Attendance History
            item {
                Spacer(modifier = Modifier.height(2.dp))
                SectionHeader(title = "Recent Attendance History")
            }

            if (state.recentHistory.isEmpty() && !state.isLoading) {
                item {
                    EmptyStateView(
                        icon = Icons.Rounded.QrCode2,
                        title = "No Check-Ins Yet",
                        subtitle = "Scan the common entrance QR on your next visit to start your attendance streak."
                    )
                }
            } else {
                items(state.recentHistory, key = { it.id }) { record ->
                    AttendanceHistoryRow(record = record)
                }
            }
        }
    }

    if (showManualCodeDialog) {
        AlertDialog(
            onDismissRequest = { showManualCodeDialog = false },
            title = { Text("Enter Reception Entrance Code") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter the entrance code displayed at reception (e.g. ${state.expectedEntranceCode}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = manualPayloadInput,
                        onValueChange = { manualPayloadInput = it },
                        singleLine = true,
                        label = { Text("Entrance QR Code") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qr_manual_payload_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.verifyAndSubmitQr(context, manualPayloadInput)
                        showManualCodeDialog = false
                    }
                ) {
                    Text("Verify")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualCodeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AttendanceHistoryRow(record: AttendanceRecord) {
    val semantic = LocalGymSemanticColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = semantic.activeGreenContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.QrCode2,
                        contentDescription = null,
                        tint = semantic.activeGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.dateLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = record.zoneLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = record.checkInTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
