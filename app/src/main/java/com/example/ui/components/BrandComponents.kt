package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.models.UserRole
import com.example.ui.theme.LocalGymSemanticColors

@Composable
fun SheildlenzBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFDCEBFF),
                        Color(0xFFCDE2FF),
                        Color(0xFFE8F2FF)
                    )
                )
            )
    ) {
        content()
    }
}

@Composable
fun CyberTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFCDE2FF).copy(alpha = 0.9f),
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, Color(0xFFB4D2FF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
                        .testTag("cyber_topbar_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1A3673)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1A3673),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF3E5894),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (actions != null) {
                Spacer(modifier = Modifier.width(8.dp))
                actions()
            }
        }
    }
}

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = Color(0xFFE8F2FF),
    borderColor: Color = Color(0xFFB4D2FF),
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Surface(
            modifier = modifier.minimumInteractiveComponentSize(),
            shape = shape,
            color = backgroundColor,
            border = BorderStroke(1.dp, borderColor),
            tonalElevation = 2.dp,
            onClick = onClick
        ) {
            content()
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = backgroundColor,
            border = BorderStroke(1.dp, borderColor),
            tonalElevation = 2.dp
        ) {
            content()
        }
    }
}

@Composable
fun LuxurySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF2563EB),
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 6.dp, height = 18.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accentColor)
            )
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1A3673)
            )
        }
        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF2563EB),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    backgroundColor: Color = Color(0xFFDCFCE7),
    textColor: Color = Color(0xFF22C55E),
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.3f))
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ApexPulseLogo(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    showText: Boolean = true
) {
    val semantic = LocalGymSemanticColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, Color(0xFF2563EB).copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_app_icon),
                contentDescription = stringResource(R.string.cd_app_logo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size)
            )
        }
        if (showText) {
            Column {
                Text(
                    text = "GYMIMPULSE",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1A3673)
                )
                Text(
                    text = "PERFORMANCE CLUB",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2563EB)
                )
            }
        }
    }
}

@Composable
fun ApexSubScreenTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null
) {
    CyberTopBar(
        title = title,
        subtitle = subtitle,
        onBackClick = onBackClick,
        modifier = modifier,
        actions = trailingContent
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    LuxurySectionHeader(
        title = title,
        modifier = modifier,
        actionLabel = actionLabel,
        onActionClick = onActionClick
    )
}

@Composable
fun RoleBadge(
    role: UserRole,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = if (role == UserRole.ADMIN) {
        Pair(Color(0xFFFEF3C7), Color(0xFFE59712))
    } else {
        Pair(Color(0xFFE0F2FE), Color(0xFF2563EB))
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bgColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = role.displayName.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
        }
    }
}

@Composable
fun QuickAccessCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CyberCard(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(accentColor.copy(alpha = 0.24f), accentColor.copy(alpha = 0.08f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1A3673),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF3E5894),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
