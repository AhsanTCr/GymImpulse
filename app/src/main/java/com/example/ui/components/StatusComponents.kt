package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.models.DataSourceType
import com.example.models.MembershipStatus
import com.example.ui.theme.LocalGymSemanticColors

@Composable
fun MembershipStatusBadge(
    status: MembershipStatus,
    modifier: Modifier = Modifier
) {
    val semantic = LocalGymSemanticColors.current
    val (fg, bg, icon) = when (status) {
        MembershipStatus.ACTIVE -> Triple(semantic.activeGreen, semantic.activeGreenContainer, Icons.Rounded.CheckCircle)
        MembershipStatus.EXPIRING_SOON -> Triple(semantic.warningAmber, semantic.warningAmberContainer, Icons.Rounded.Schedule)
        MembershipStatus.EXPIRED -> Triple(semantic.alertCoral, semantic.alertCoralContainer, Icons.Rounded.Error)
        MembershipStatus.PAYMENT_OVERDUE -> Triple(semantic.alertCoral, semantic.alertCoralContainer, Icons.Rounded.Warning)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = status.label,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = status.label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = fg
            )
        }
    }
}

@Composable
fun DataSourceBadge(
    source: DataSourceType,
    modifier: Modifier = Modifier
) {
    val semantic = LocalGymSemanticColors.current
    val (fg, bg, icon) = when (source) {
        DataSourceType.LOGGED -> Triple(semantic.activeGreen, semantic.activeGreenContainer, Icons.Rounded.CheckCircle)
        DataSourceType.ESTIMATED -> Triple(semantic.infoCyan, semantic.infoCyanContainer, Icons.Rounded.Schedule)
        DataSourceType.MISSING -> Triple(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant, Icons.AutoMirrored.Rounded.HelpOutline)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = source.label,
                tint = fg,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = source.label,
                style = MaterialTheme.typography.labelSmall,
                color = fg
            )
        }
    }
}

@Composable
fun MetricStatCard(
    label: String,
    value: String,
    caption: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
