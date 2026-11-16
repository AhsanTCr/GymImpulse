package com.example.features.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.config.AppConfig
import com.example.database.QrSecurityEventDoc
import com.example.ui.components.ApexSubScreenTopBar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalGymSemanticColors
import com.example.utils.QrAntiFraudVerifier
import com.example.utils.QrMatrixGenerator
import kotlinx.coroutines.delay

@Composable
fun AdminQrManagementScreen(
    state: AdminUiState,
    onBack: () -> Unit,
    onUpdateConfig: (
        isQrEnabled: Boolean,
        allowStaticFallback: Boolean,
        rotationWindowSeconds: Long,
        allowedRadiusMeters: Double?
    ) -> Unit,
    onMarkEventReviewed: (QrSecurityEventDoc) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalGymSemanticColors.current
    val cfg = state.gymConfig
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var scanSearch by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val windowSec = cfg.rotationWindowSeconds.coerceAtLeast(30L)
    val rotatingPayload = remember(cfg.staticEntranceCode, nowMillis / (windowSec * 1000L)) {
        QrAntiFraudVerifier.generateRotatingEntrancePayload(
            entranceCode = cfg.staticEntranceCode.ifBlank { AppConfig.COMMON_ENTRANCE_QR_CODE },
            nowMillis = nowMillis,
            windowSeconds = windowSec
        )
    }
    val qrMatrix = remember(rotatingPayload) { QrMatrixGenerator.encode(rotatingPayload) }
    val secondsRemaining = windowSec - ((nowMillis / 1000L) % windowSec)

    val membersByUid = remember(state.allMembers) { state.allMembers.associateBy { it.userId } }
    val filteredScans = remember(state.allAttendance, scanSearch, membersByUid) {
        val q = scanSearch.trim().lowercase()
        state.allAttendance.filter { att ->
            if (q.isEmpty()) true
            else {
                val mName = membersByUid[att.userId]?.profile?.fullName.orEmpty().lowercase()
                val mCode = membersByUid[att.userId]?.profile?.memberCode.orEmpty().lowercase()
                mName.contains(q) || mCode.contains(q) || att.dateKey.contains(q)
            }
        }.take(20)
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
                .widthIn(max = 720.dp)
        ) {
            ApexSubScreenTopBar(
                title = "Entrance QR Terminal",
                subtitle = "Common reception code & check-in security",
                onBackClick = onBack
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Scannable Common Entrance QR Poster
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_qr_poster_card"),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.5.dp,
                            if (cfg.isQrEnabled) semantic.activeGreen else semantic.alertCoral
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (cfg.isQrEnabled) semantic.activeGreenContainer else semantic.alertCoralContainer
                            ) {
                                Text(
                                    text = if (cfg.isQrEnabled) "ACTIVE • ROTATES IN ${secondsRemaining}s" else "CHECK-IN PAUSED",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (cfg.isQrEnabled) semantic.activeGreen else semantic.alertCoral,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .size(210.dp)
                                    .padding(4.dp)
                            ) {
                                QrMatrixCanvas(
                                    matrix = qrMatrix,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .aspectRatio(1f)
                                        .padding(12.dp)
                                )
                            }

                            Text(
                                text = cfg.staticEntranceCode.ifBlank { AppConfig.COMMON_ENTRANCE_QR_CODE },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = rotatingPayload,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Security & Rotation Controls
                item {
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
                            Text("QR Validation Settings", style = MaterialTheme.typography.titleMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Entrance QR Check-In Enabled", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "Disable during club closures or maintenance",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = cfg.isQrEnabled,
                                    onCheckedChange = { enabled ->
                                        onUpdateConfig(
                                            enabled,
                                            cfg.allowStaticFallback,
                                            cfg.rotationWindowSeconds,
                                            cfg.allowedRadiusMeters
                                        )
                                    },
                                    modifier = Modifier.testTag("admin_qr_enable_switch")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Allow Static Poster Fallback", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "Accept printed static entrance code without rotating token",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = cfg.allowStaticFallback,
                                    onCheckedChange = { fallback ->
                                        onUpdateConfig(
                                            cfg.isQrEnabled,
                                            fallback,
                                            cfg.rotationWindowSeconds,
                                            cfg.allowedRadiusMeters
                                        )
                                    },
                                    modifier = Modifier.testTag("admin_qr_static_switch")
                                )
                            }

                            Text("Rotation Window", style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(60L, 90L, 120L, 300L).forEach { sec ->
                                    FilterChip(
                                        selected = cfg.rotationWindowSeconds == sec,
                                        onClick = {
                                            onUpdateConfig(
                                                cfg.isQrEnabled,
                                                cfg.allowStaticFallback,
                                                sec,
                                                cfg.allowedRadiusMeters
                                            )
                                        },
                                        label = { Text("${sec}s") }
                                    )
                                }
                            }
                        }
                    }
                }

                // Failed / Suspicious QR Scan Events
                item {
                    SectionHeader(title = "Suspicious / Blocked Scans (${state.qrSecurityEvents.size})")
                }

                if (state.qrSecurityEvents.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Rounded.Security,
                            title = "Zero Blocked Scan Alerts",
                            subtitle = "Expired screenshots, unrecognized codes, and out-of-range check-ins appear here."
                        )
                    }
                } else {
                    items(state.qrSecurityEvents.take(10), key = { it.id }) { evt ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = if (evt.reviewed) MaterialTheme.colorScheme.surface else semantic.alertCoralContainer,
                            border = BorderStroke(
                                1.dp,
                                if (evt.reviewed) MaterialTheme.colorScheme.outlineVariant else semantic.alertCoral.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${evt.memberName} • ${evt.eventType}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = evt.reason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (evt.reviewed) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Reviewed",
                                        tint = semantic.activeGreen
                                    )
                                } else {
                                    OutlinedButton(onClick = { onMarkEventReviewed(evt) }) {
                                        Text("Review")
                                    }
                                }
                            }
                        }
                    }
                }

                // Member Check-In Log Inspection
                item {
                    SectionHeader(title = "Recent Member Entrance Scans")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = scanSearch,
                        onValueChange = { scanSearch = it },
                        label = { Text("Filter check-ins by member or YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (filteredScans.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Rounded.QrCode2,
                            title = "No Scans Match Filter",
                            subtitle = "Verified entrance check-ins from members appear here live."
                        )
                    }
                } else {
                    items(filteredScans, key = { "${it.userId}_${it.dateKey}" }) { scan ->
                        val member = membersByUid[scan.userId]
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = member?.profile?.fullName ?: scan.userId,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${scan.dateLabel} • Token ${scan.verificationToken.take(14)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = scan.checkInTime,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QrMatrixCanvas(
    matrix: Array<BooleanArray>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val count = matrix.size.coerceAtLeast(1)
        val cellSize = minOf(size.width, size.height) / count.toFloat()
        for (r in 0 until count) {
            for (c in 0 until count) {
                if (matrix[r][c]) {
                    drawRect(
                        color = Color(0xFF0B0E14),
                        topLeft = Offset(c * cellSize, r * cellSize),
                        size = Size(cellSize + 0.4f, cellSize + 0.4f)
                    )
                }
            }
        }
    }
}
