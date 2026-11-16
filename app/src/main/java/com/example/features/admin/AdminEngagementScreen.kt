package com.example.features.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.dp
import com.example.database.AnnouncementDoc
import com.example.database.PromotionalOfferDoc
import com.example.models.BroadcastAudience
import com.example.models.BroadcastType
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalGymSemanticColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AdminEngagementScreen(
    state: AdminUiState,
    onSaveAnnouncement: (
        id: String,
        title: String,
        description: String,
        dateLabel: String,
        category: String,
        isImportant: Boolean,
        isPublished: Boolean,
        scheduledAtLabel: String?,
        presetImageKey: String?,
        pickedImageUri: String?,
        existingImageData: String?,
        ctaLabel: String?
    ) -> Unit,
    onDeleteAnnouncement: (AnnouncementDoc) -> Unit,
    onSendBroadcast: (title: String, message: String, type: String, audience: String, isUrgent: Boolean) -> Unit,
    onSaveOffer: (PromotionalOfferDoc) -> Unit,
    onDeleteOffer: (PromotionalOfferDoc) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalGymSemanticColors.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Announcements, 1=Broadcasts, 2=Offers
    var editingAnnouncement by remember { mutableStateOf<AnnouncementDoc?>(null) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    var editingOffer by remember { mutableStateOf<PromotionalOfferDoc?>(null) }
    var showOfferDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag("engage_tab_announcements"),
                    text = { Text("Posts (${state.announcements.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("engage_tab_broadcasts"),
                    text = { Text("Notify (${state.broadcastNotifications.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.testTag("engage_tab_offers"),
                    text = { Text("Offers (${state.promotionalOffers.size})") }
                )
            }

            when (selectedTab) {
                0 -> AnnouncementsAdminTab(
                    announcements = state.announcements,
                    onNew = {
                        editingAnnouncement = null
                        showAnnouncementDialog = true
                    },
                    onEdit = {
                        editingAnnouncement = it
                        showAnnouncementDialog = true
                    },
                    onDelete = onDeleteAnnouncement
                )

                1 -> BroadcastAdminTab(
                    broadcasts = state.broadcastNotifications,
                    onSendBroadcast = onSendBroadcast
                )

                2 -> OffersAdminTab(
                    offers = state.promotionalOffers,
                    onNew = {
                        editingOffer = null
                        showOfferDialog = true
                    },
                    onEdit = {
                        editingOffer = it
                        showOfferDialog = true
                    },
                    onToggleActive = { offer ->
                        onSaveOffer(offer.copy(isActive = !offer.isActive))
                    },
                    onDelete = onDeleteOffer
                )
            }
        }
    }

    if (showAnnouncementDialog) {
        AnnouncementEditorDialog(
            initial = editingAnnouncement,
            onDismiss = { showAnnouncementDialog = false },
            onSave = { id, title, desc, dateLabel, cat, important, published, sched, imgKey, pickedUri, existingData, cta ->
                onSaveAnnouncement(id, title, desc, dateLabel, cat, important, published, sched, imgKey, pickedUri, existingData, cta)
                showAnnouncementDialog = false
            }
        )
    }

    if (showOfferDialog) {
        PromotionalOfferDialog(
            initial = editingOffer,
            onDismiss = { showOfferDialog = false },
            onSave = { offer ->
                onSaveOffer(offer)
                showOfferDialog = false
            }
        )
    }
}

@Composable
private fun AnnouncementsAdminTab(
    announcements: List<AnnouncementDoc>,
    onNew: () -> Unit,
    onEdit: (AnnouncementDoc) -> Unit,
    onDelete: (AnnouncementDoc) -> Unit
) {
    val semantic = LocalGymSemanticColors.current
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = onNew,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("engage_create_announcement_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.size(6.dp))
                Text("Create Announcement")
            }
        }
        if (announcements.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Rounded.Campaign,
                    title = "No Announcements Yet",
                    subtitle = "Create events, facility notices, or competition announcements synced to all members."
                )
            }
        } else {
            items(announcements, key = { it.id }) { ann ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (ann.isPublished) semantic.activeGreenContainer else semantic.warningAmberContainer
                                ) {
                                    Text(
                                        text = if (ann.isPublished) "PUBLISHED" else "SCHEDULED / DRAFT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (ann.isPublished) semantic.activeGreen else semantic.warningAmber,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                if (ann.isImportant) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = semantic.alertCoralContainer
                                    ) {
                                        Text(
                                            text = "IMPORTANT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = semantic.alertCoral,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            Row {
                                IconButton(onClick = { onEdit(ann) }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit announcement")
                                }
                                IconButton(onClick = { onDelete(ann) }) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Delete announcement",
                                        tint = semantic.alertCoral
                                    )
                                }
                            }
                        }
                        Text(ann.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = ann.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${ann.category} • ${ann.dateLabel}" +
                                (ann.scheduledAtLabel?.let { " • Sched: $it" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BroadcastAdminTab(
    broadcasts: List<com.example.database.BroadcastNotificationDoc>,
    onSendBroadcast: (title: String, message: String, type: String, audience: String, isUrgent: Boolean) -> Unit
) {
    val semantic = LocalGymSemanticColors.current
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(BroadcastType.GENERAL) }
    var selectedAudience by remember { mutableStateOf(BroadcastAudience.ALL_MEMBERS) }
    var isUrgent by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                    Text(
                        text = "Dispatch Club Notification",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Notification Title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_title_input")
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message Body") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_message_input")
                    )
                    Text("Notification Type", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BroadcastType.entries.forEach { t ->
                            FilterChip(
                                selected = selectedType == t,
                                onClick = { selectedType = t },
                                label = { Text(t.label) }
                            )
                        }
                    }
                    Text("Target Audience", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BroadcastAudience.entries.forEach { aud ->
                            FilterChip(
                                selected = selectedAudience == aud,
                                onClick = { selectedAudience = aud },
                                label = { Text(aud.label) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mark as Urgent Club Alert", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = isUrgent, onCheckedChange = { isUrgent = it })
                    }
                    Button(
                        onClick = {
                            onSendBroadcast(title, message, selectedType.name, selectedAudience.name, isUrgent)
                            if (title.isNotBlank() && message.isNotBlank()) {
                                title = ""
                                message = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_send_button")
                    ) {
                        Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Send Broadcast to Members")
                    }
                }
            }
        }

        item {
            SectionHeader(title = "Broadcast History (${broadcasts.size})")
        }

        if (broadcasts.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "No Broadcasts Sent",
                    subtitle = "Dispatched notifications appear on targeted members' dashboards."
                )
            }
        } else {
            items(broadcasts, key = { it.id }) { item ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (item.isUrgent) semantic.alertCoral.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.type.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (item.isUrgent) semantic.alertCoral else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.audience.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(item.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = item.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OffersAdminTab(
    offers: List<PromotionalOfferDoc>,
    onNew: () -> Unit,
    onEdit: (PromotionalOfferDoc) -> Unit,
    onToggleActive: (PromotionalOfferDoc) -> Unit,
    onDelete: (PromotionalOfferDoc) -> Unit
) {
    val semantic = LocalGymSemanticColors.current
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = onNew,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("engage_create_offer_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.size(6.dp))
                Text("Create Promotional Offer")
            }
        }

        if (offers.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Rounded.LocalOffer,
                    title = "No Promotional Offers",
                    subtitle = "Publish renewal discounts, referral bonuses, or seasonal packages to members."
                )
            }
        } else {
            items(offers, key = { it.id }) { offer ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (offer.isActive) semantic.activeGreenContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${offer.badgeText} • ${if (offer.isActive) "ACTIVE" else "PAUSED"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (offer.isActive) semantic.activeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = offer.isActive,
                                    onCheckedChange = { onToggleActive(offer) }
                                )
                                IconButton(onClick = { onEdit(offer) }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit offer")
                                }
                                IconButton(onClick = { onDelete(offer) }) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Delete offer",
                                        tint = semantic.alertCoral
                                    )
                                }
                            }
                        }
                        Text(offer.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = offer.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Audience: ${offer.targetAudience} • Valid: ${offer.validUntilLabel}" +
                                (offer.discountCode?.let { " • Code: $it" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementEditorDialog(
    initial: AnnouncementDoc?,
    onDismiss: () -> Unit,
    onSave: (
        id: String,
        title: String,
        description: String,
        dateLabel: String,
        category: String,
        isImportant: Boolean,
        isPublished: Boolean,
        scheduledAtLabel: String?,
        presetImageKey: String?,
        pickedImageUri: String?,
        existingImageData: String?,
        ctaLabel: String?
    ) -> Unit
) {
    val defaultDate = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Calendar.getInstance().time)
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var dateLabel by remember { mutableStateOf(initial?.dateLabel ?: defaultDate) }
    var category by remember { mutableStateOf(initial?.category ?: "EVENT") }
    var isImportant by remember { mutableStateOf(initial?.isImportant ?: false) }
    var isPublished by remember { mutableStateOf(initial?.isPublished ?: true) }
    var schedLabel by remember { mutableStateOf(initial?.scheduledAtLabel ?: "") }
    var presetKey by remember { mutableStateOf(initial?.imageKey ?: "hero") }
    var pickedUri by remember { mutableStateOf<String?>(null) }
    var cta by remember { mutableStateOf(initial?.ctaLabel ?: "") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) pickedUri = uri.toString()
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Announcement" else "Edit Announcement") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Headline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Details") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateLabel,
                    onValueChange = { dateLabel = it },
                    label = { Text("Display Date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("IMPORTANT", "EVENT", "COMPETITION").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Important Badge", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isImportant, onCheckedChange = { isImportant = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Publish Live Now", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isPublished, onCheckedChange = { isPublished = it })
                }
                if (!isPublished) {
                    OutlinedTextField(
                        value = schedLabel,
                        onValueChange = { schedLabel = it },
                        label = { Text("Scheduled Release Date/Time") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedButton(
                    onClick = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        when {
                            pickedUri != null -> "Photo Selected (Ready to Compress)"
                            initial?.imageData != null -> "Replace Uploaded Photo"
                            else -> "Upload Banner Image (Optional)"
                        }
                    )
                }
                if (pickedUri == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("hero" to "Club Hero", "competition" to "Event Banner", "" to "None").forEach { (k, lbl) ->
                            FilterChip(
                                selected = presetKey == k,
                                onClick = { presetKey = k },
                                label = { Text(lbl) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = cta,
                    onValueChange = { cta = it },
                    label = { Text("CTA Label (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial?.id ?: "",
                        title,
                        desc,
                        dateLabel,
                        category,
                        isImportant,
                        isPublished,
                        schedLabel.takeIf { !isPublished },
                        presetKey.ifBlank { null },
                        pickedUri,
                        initial?.imageData,
                        cta.ifBlank { null }
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PromotionalOfferDialog(
    initial: PromotionalOfferDoc?,
    onDismiss: () -> Unit,
    onSave: (PromotionalOfferDoc) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var subtitle by remember { mutableStateOf(initial?.subtitle ?: "") }
    var badge by remember { mutableStateOf(initial?.badgeText ?: "SPECIAL OFFER") }
    var code by remember { mutableStateOf(initial?.discountCode ?: "") }
    var validUntil by remember { mutableStateOf(initial?.validUntilLabel ?: "End of Month") }
    var audience by remember { mutableStateOf(initial?.targetAudience ?: BroadcastAudience.ALL_MEMBERS.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Promotional Offer" else "Edit Promotional Offer") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 410.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Offer Headline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Offer Details") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = badge,
                    onValueChange = { badge = it },
                    label = { Text("Badge Text (e.g. 20% OFF)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Promo Code (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = validUntil,
                    onValueChange = { validUntil = it },
                    label = { Text("Valid Until") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BroadcastAudience.entries.forEach { aud ->
                        FilterChip(
                            selected = audience == aud.name,
                            onClick = { audience = aud.name },
                            label = { Text(aud.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        PromotionalOfferDoc(
                            id = initial?.id ?: "",
                            title = title.trim(),
                            subtitle = subtitle.trim(),
                            badgeText = badge.trim().ifBlank { "OFFER" },
                            discountCode = code.trim().takeIf { it.isNotBlank() },
                            validUntilLabel = validUntil.trim().ifBlank { "Limited Time" },
                            targetAudience = audience,
                            isActive = initial?.isActive ?: true
                        )
                    )
                }
            ) { Text("Save Offer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
