package com.example.features.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.models.AdminDashboardModule
import com.example.ui.theme.LocalGymSemanticColors

@Composable
fun AdminPortalRootScreen(
    isBackendAdminAuthorized: Boolean,
    adminEmail: String,
    viewModel: AdminViewModel,
    onSwitchToMemberView: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (!isBackendAdminAuthorized || !state.isPasscodeVerified) {
        AdminPasscodeGate(
            isBackendAdminAuthorized = isBackendAdminAuthorized,
            adminEmail = adminEmail,
            passcodeError = state.passcodeError,
            onUnlockWithPasscode = viewModel::unlockWithPasscode,
            onSwitchToMemberView = onSwitchToMemberView,
            onLogout = onLogout,
            modifier = modifier
        )
        return
    }

    if (state.showQrManagement) {
        BackHandler { viewModel.openQrManagement(false) }
        AdminQrManagementScreen(
            state = state,
            onBack = { viewModel.openQrManagement(false) },
            onUpdateConfig = viewModel::updateGymQrConfig,
            onMarkEventReviewed = viewModel::markQrEventReviewed,
            modifier = modifier
        )
        return
    }

    AdminDashboardShell(
        state = state,
        viewModel = viewModel,
        onSwitchToMemberView = {
            viewModel.lockAdminGate()
            onSwitchToMemberView()
        },
        onLogout = {
            viewModel.lockAdminGate()
            onLogout()
        },
        modifier = modifier
    )
}

@Composable
private fun AdminDashboardShell(
    state: AdminUiState,
    viewModel: AdminViewModel,
    onSwitchToMemberView: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalGymSemanticColors.current
    val context = LocalContext.current

    BackHandler {
        if (state.selectedModule != AdminDashboardModule.GYM_OVERVIEW) {
            viewModel.selectModule(AdminDashboardModule.GYM_OVERVIEW)
        } else {
            onSwitchToMemberView()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GYMIMPULSE ADMIN",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.accentVolt
                            )
                            Text(
                                text = state.selectedModule.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                onClick = { viewModel.openQrManagement(true) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("admin_topbar_qr_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCode2,
                                        contentDescription = "Entrance QR Terminal",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "QR",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            IconButton(
                                onClick = onSwitchToMemberView,
                                modifier = Modifier.testTag("admin_topbar_member_view_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PersonOutline,
                                    contentDescription = "Switch to Member View",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = onLogout,
                                modifier = Modifier.testTag("admin_topbar_logout_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = "Sign Out",
                                    tint = semantic.alertCoral
                                )
                            }
                        }
                    }

                    if (!state.actionFeedback.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = MaterialTheme.shapes.small,
                            color = semantic.infoCyanContainer,
                            border = BorderStroke(1.dp, semantic.infoCyan.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = state.actionFeedback,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = semantic.infoCyan,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = viewModel::clearActionFeedback,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Dismiss feedback",
                                        tint = semantic.infoCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                AdminDashboardModule.entries.forEach { module ->
                    val selected = state.selectedModule == module
                    val (selectedIcon, unselectedIcon) = when (module) {
                        AdminDashboardModule.GYM_OVERVIEW -> Icons.Rounded.Dashboard to Icons.Outlined.Dashboard
                        AdminDashboardModule.MEMBER_INSIGHTS -> Icons.Rounded.Group to Icons.Outlined.Group
                        AdminDashboardModule.COMPETITION_CONTROL -> Icons.Rounded.EmojiEvents to Icons.Outlined.EmojiEvents
                        AdminDashboardModule.MEMBER_ENGAGEMENT -> Icons.Rounded.Campaign to Icons.Outlined.Campaign
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.selectModule(module) },
                        modifier = Modifier.testTag("admin_nav_${module.name.lowercase()}"),
                        icon = {
                            Icon(
                                imageVector = if (selected) selectedIcon else unselectedIcon,
                                contentDescription = module.title
                            )
                        },
                        label = {
                            Text(
                                text = module.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Syncing club analytics from Firestore...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                when (state.selectedModule) {
                    AdminDashboardModule.GYM_OVERVIEW -> AdminOverviewScreen(
                        state = state,
                        onSelectPeriod = viewModel::selectOverviewPeriod,
                        onNavigateToModule = viewModel::selectModule,
                        onOpenQrManagement = { viewModel.openQrManagement(true) },
                        onUpdateAppVersion = viewModel::updateAppVersionConfig,
                        onExportJson = viewModel::exportSyncBundleJson,
                        onExportMembersCsv = viewModel::exportMembersCsv,
                        onExportAttendanceCsv = viewModel::exportAttendanceCsv,
                        onPreviewInboundSync = viewModel::previewInboundSyncPayload,
                        onClearSyncDialog = viewModel::clearSyncDialogState
                    )

                    AdminDashboardModule.MEMBER_INSIGHTS -> AdminMemberInsightsScreen(
                        state = state,
                        onSelectSegment = viewModel::selectSegment,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onConfirmPayment = viewModel::confirmMemberPayment,
                        onRejectRequest = viewModel::rejectPaymentRequest,
                        onUpdateMembershipStatus = viewModel::updateMembershipStatus
                    )

                    AdminDashboardModule.COMPETITION_CONTROL -> AdminCompetitionControlScreen(
                        state = state,
                        onSelectCategory = viewModel::selectCompetitionCategory,
                        onSaveCompetitionConfig = viewModel::saveCompetitionConfig,
                        onPublishMonthlyWinner = viewModel::publishMonthlyWinner,
                        onAdjustMemberScore = viewModel::adjustMemberScore,
                        onResolveReferral = viewModel::resolveReferral
                    )

                    AdminDashboardModule.MEMBER_ENGAGEMENT -> AdminEngagementScreen(
                        state = state,
                        onSaveAnnouncement = { id, title, desc, dateLabel, cat, important, published, sched, imgKey, pickedUri, existingImgData, cta ->
                            viewModel.saveAnnouncementWithOptionalPhoto(
                                context = context,
                                existingId = id,
                                title = title,
                                description = desc,
                                dateLabel = dateLabel,
                                category = cat,
                                isImportant = important,
                                isPublished = published,
                                scheduledAtLabel = sched,
                                presetImageKey = imgKey,
                                pickedImageUri = pickedUri,
                                existingImageData = existingImgData,
                                ctaLabel = cta
                            )
                        },
                        onDeleteAnnouncement = viewModel::deleteAnnouncement,
                        onSendBroadcast = viewModel::sendBroadcastNotification,
                        onSaveOffer = viewModel::savePromotionalOffer,
                        onDeleteOffer = viewModel::deletePromotionalOffer
                    )
                }
            }
        }
    }
}
