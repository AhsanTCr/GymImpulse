package com.example.models

/**
 * Reserved Phase 3 Admin module models.
 * Admin dashboards, member verification, and payment approvals will expand here in Phase 3.
 */
data class AdminModuleCardModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val targetPhase: String = "Phase 3"
)
