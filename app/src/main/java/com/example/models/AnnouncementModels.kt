package com.example.models

enum class AnnouncementCategory(val label: String) {
    ALL("All"),
    IMPORTANT("Important"),
    EVENT("Events"),
    COMPETITION("Competitions")
}

data class GymAnnouncement(
    val id: String,
    val title: String,
    val description: String,
    val dateLabel: String,
    val category: AnnouncementCategory,
    val isImportant: Boolean,
    val isPublished: Boolean = true,
    val scheduledAtLabel: String? = null,
    val bannerDrawableRes: Int? = null,
    val bannerImageData: String? = null,
    val ctaLabel: String? = null
)
