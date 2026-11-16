package com.example.features.qr

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.config.AppConfig
import com.example.ui.theme.LocalGymSemanticColors

@Composable
fun QrScannerViewfinder(
    hasCameraPermission: Boolean,
    isConfirmed: Boolean,
    isProcessingScan: Boolean,
    entranceCode: String,
    onRequestCameraPermission: () -> Unit,
    onScanLiveEntranceQr: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalGymSemanticColors.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraReady by remember { mutableStateOf(false) }
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    if (hasCameraPermission) {
        DisposableEffect(lifecycleOwner, previewView) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            var boundProvider: ProcessCameraProvider? = null
            val executor = ContextCompat.getMainExecutor(context)
            cameraProviderFuture.addListener({
                runCatching {
                    val provider = cameraProviderFuture.get()
                    boundProvider = provider
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview
                    )
                    cameraReady = true
                }.onFailure {
                    cameraReady = false
                }
            }, executor)

            onDispose {
                runCatching { boundProvider?.unbindAll() }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "qr_laser_beam")
    val beamProgress by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beam_y"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(310.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = Color(0xFF0A0E17),
        border = BorderStroke(
            1.5.dp,
            if (isConfirmed) semantic.activeGreen else MaterialTheme.colorScheme.outline
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                // Real CameraX PreviewView backing the scanner viewfinder
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
                // Dark scrim so viewfinder HUD and text stay readable over live camera feed
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color(0xFF0A0E17).copy(alpha = if (cameraReady) 0.45f else 0.92f)
                        )
                )

                val bracketColor = if (isConfirmed) semantic.activeGreen else semantic.accentVolt
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp)
                ) {
                    val corner = 30.dp.toPx()
                    val stroke = 4.dp.toPx()
                    drawLine(bracketColor, Offset(0f, 0f), Offset(corner, 0f), stroke, StrokeCap.Round)
                    drawLine(bracketColor, Offset(0f, 0f), Offset(0f, corner), stroke, StrokeCap.Round)
                    drawLine(bracketColor, Offset(size.width - corner, 0f), Offset(size.width, 0f), stroke, StrokeCap.Round)
                    drawLine(bracketColor, Offset(size.width, 0f), Offset(size.width, corner), stroke, StrokeCap.Round)
                    drawLine(bracketColor, Offset(0f, size.height), Offset(corner, size.height), stroke, StrokeCap.Round)
                    drawLine(bracketColor, Offset(0f, size.height - corner), Offset(0f, size.height), stroke, StrokeCap.Round)
                    drawLine(bracketColor, Offset(size.width - corner, size.height), Offset(size.width, size.height), stroke, StrokeCap.Round)
                    drawLine(bracketColor, Offset(size.width, size.height - corner), Offset(size.width, size.height), stroke, StrokeCap.Round)

                    if (!isConfirmed) {
                        val y = size.height * beamProgress
                        drawLine(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, bracketColor, Color.Transparent)
                            ),
                            start = Offset(10f, y),
                            end = Offset(size.width - 10f, y),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_qr_entrance),
                        contentDescription = stringResource(R.string.cd_qr_scanner),
                        tint = Color.White.copy(alpha = 0.88f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = if (isConfirmed) "Entrance QR Verified" else "Align Common Gym Entrance QR",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Entrance Code: ${entranceCode.ifBlank { AppConfig.COMMON_ENTRANCE_QR_CODE }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = semantic.accentVolt
                    )
                    if (isProcessingScan) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = semantic.accentVolt,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Button(
                            onClick = onScanLiveEntranceQr,
                            modifier = Modifier.testTag("qr_simulate_scan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(if (isConfirmed) "Checked In Today" else "Verify Entrance QR")
                        }
                    }
                }
            } else {
                // Camera permission required state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Camera permission needed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Camera Permission Needed",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Allow camera access so GymPulse can scan the single physical QR code posted at the gym entrance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRequestCameraPermission,
                        modifier = Modifier.testTag("qr_grant_camera_button")
                    ) {
                        Text("Enable Camera Scanner")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onScanLiveEntranceQr,
                        modifier = Modifier.testTag("qr_preview_checkin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCode2,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Verify Reception Check-In", color = Color.White)
                    }
                }
            }
        }
    }
}
