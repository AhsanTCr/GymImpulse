package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.models.DataSourceType
import com.example.models.NutritionChartPoint
import com.example.ui.theme.LocalGymSemanticColors

@Composable
fun CircularGoalRing(
    progress: Float,
    centerTitle: String,
    centerSubtitle: String,
    ringColor: Color,
    size: Dp = 104.dp,
    strokeWidth: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = clamped * 360f,
        animationSpec = tween(durationMillis = 600),
        label = "goal_ring_sweep"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = centerSubtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NutritionBarChart(
    points: List<NutritionChartPoint>,
    modifier: Modifier = Modifier
) {
    val semantic = LocalGymSemanticColors.current
    val loggedColor = semantic.activeGreen
    val estimatedColor = semantic.infoCyan
    val missingColor = MaterialTheme.colorScheme.outline
    val goalLineColor = semantic.warningAmber.copy(alpha = 0.7f)

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend explicitly distinguishing actual logged, estimated, and missing data
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChartLegendItem(color = loggedColor, label = "Actual Logged")
            ChartLegendItem(color = estimatedColor, label = "Estimated")
            ChartLegendItem(color = missingColor, label = "Not tracked")
        }
        Spacer(modifier = Modifier.height(14.dp))

        val maxVal = (points.maxOfOrNull { maxOf(it.proteinGrams, it.goalGrams) } ?: 140).coerceAtLeast(1)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val count = points.size.coerceAtLeast(1)
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.46f).coerceAtMost(36.dp.toPx())
            val chartHeight = size.height - 8.dp.toPx()

            // Target goal dashed line
            val goalRatio = points.firstOrNull()?.goalGrams?.toFloat()?.div(maxVal) ?: 0.85f
            val goalY = chartHeight * (1f - goalRatio.coerceIn(0.1f, 0.95f))
            drawLine(
                color = goalLineColor,
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )

            points.forEachIndexed { index, point ->
                val left = slotWidth * index + (slotWidth - barWidth) / 2f
                if (point.source == DataSourceType.MISSING || point.proteinGrams <= 0) {
                    val emptyHeight = 14.dp.toPx()
                    drawRoundRect(
                        color = missingColor.copy(alpha = 0.45f),
                        topLeft = Offset(left, chartHeight - emptyHeight),
                        size = Size(barWidth, emptyHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    )
                } else {
                    val ratio = (point.proteinGrams.toFloat() / maxVal).coerceIn(0.08f, 1f)
                    val barHeight = chartHeight * ratio
                    val top = chartHeight - barHeight
                    val barColor = if (point.source == DataSourceType.LOGGED) loggedColor else estimatedColor.copy(alpha = 0.65f)
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                    if (point.source == DataSourceType.ESTIMATED) {
                        drawRoundRect(
                            color = estimatedColor,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f), 0f)
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            points.forEach { point ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (point.source == DataSourceType.MISSING) "Not tracked" else "${point.proteinGrams}g",
                        style = MaterialTheme.typography.labelSmall,
                        color = when (point.source) {
                            DataSourceType.LOGGED -> MaterialTheme.colorScheme.onSurface
                            DataSourceType.ESTIMATED -> semantic.infoCyan
                            DataSourceType.MISSING -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = point.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
