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
    val bannerDrawableRes: Int? = null,
    val ctaLabel: String? = null
)
