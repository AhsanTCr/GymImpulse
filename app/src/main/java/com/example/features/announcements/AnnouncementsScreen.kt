package com.example.features.announcements

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.models.AnnouncementCategory
import com.example.models.GymAnnouncement
import com.example.ui.components.ApexSubScreenTopBar
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.LocalGymSemanticColors
import com.example.utils.ImageCompressionUtils

@Composable
fun AnnouncementsScreen(
    viewModel: AnnouncementsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedAnnouncement by remember { mutableStateOf<GymAnnouncement?>(null) }

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
                title = "Club Announcements",
                subtitle = "Events, competitions & notices",
                onBackClick = onNavigateBack
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnnouncementCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = state.selectedFilter == cat,
                        onClick = { viewModel.selectFilter(cat) },
                        modifier = Modifier.testTag("announcement_filter_${cat.name.lowercase()}"),
                        label = { Text(cat.label) }
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (!state.errorMessage.isNullOrBlank()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Rounded.Campaign,
                            title = "Announcements Unavailable",
                            subtitle = state.errorMessage.orEmpty()
                        )
                    }
                } else if (state.filteredAnnouncements.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Rounded.Campaign,
                            title = "No Announcements Here",
                            subtitle = "New club competitions, events, and important gym notices will appear here."
                        )
                    }
                } else {
                    items(state.filteredAnnouncements, key = { it.id }) { item ->
                        AnnouncementCard(
                            announcement = item,
                            onClick = { selectedAnnouncement = item }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    selectedAnnouncement?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedAnnouncement = null },
            title = { Text(item.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnnouncementBannerImage(
                        bannerDrawableRes = item.bannerDrawableRes,
                        bannerImageData = item.bannerImageData,
                        height = 130.dp,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    )
                    Text(
                        text = "${item.category.label} • ${item.dateLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAnnouncement = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun AnnouncementCard(
    announcement: GymAnnouncement,
    onClick: () -> Unit
) {
    val semantic = LocalGymSemanticColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("announcement_card_${announcement.id}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (announcement.isImportant) 1.5.dp else 1.dp,
            color = if (announcement.isImportant) semantic.alertCoral.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        onClick = onClick
    ) {
        Column {
            AnnouncementBannerImage(
                bannerDrawableRes = announcement.bannerDrawableRes,
                bannerImageData = announcement.bannerImageData,
                height = 156.dp
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (announcement.isImportant) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = semantic.alertCoralContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PriorityHigh,
                                        contentDescription = "Important",
                                        tint = semantic.alertCoral,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "IMPORTANT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = semantic.alertCoral
                                    )
                                }
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (announcement.category == AnnouncementCategory.COMPETITION) {
                                    Icon(
                                        imageVector = Icons.Rounded.EmojiEvents,
                                        contentDescription = null,
                                        tint = semantic.warningAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = announcement.category.label.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = announcement.dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = announcement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!announcement.ctaLabel.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = announcement.ctaLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementBannerImage(
    bannerDrawableRes: Int?,
    bannerImageData: String?,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val decoded = remember(bannerImageData) {
        bannerImageData?.let { ImageCompressionUtils.decodeBase64DataUri(it)?.asImageBitmap() }
    }
    when {
        decoded != null -> {
            Image(
                bitmap = decoded,
                contentDescription = stringResource(R.string.cd_announcement_image),
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .fillMaxWidth()
                    .height(height)
            )
        }
        bannerDrawableRes != null -> {
            Image(
                painter = painterResource(id = bannerDrawableRes),
                contentDescription = stringResource(R.string.cd_announcement_image),
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .fillMaxWidth()
                    .height(height)
            )
        }
    }
}
