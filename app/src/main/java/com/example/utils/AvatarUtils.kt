package com.example.utils

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

object AvatarUtils {
    private val avatarPalette = listOf(
        Color(0xFF2563EB),
        Color(0xFF0D9488),
        Color(0xFFD97706),
        Color(0xFF7C3AED),
        Color(0xFFDB2777),
        Color(0xFF059669),
        Color(0xFFEA580C),
        Color(0xFF0284C7)
    )

    fun extractInitials(fullName: String): String {
        val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> "AP"
            parts.size == 1 -> parts.first().take(2).uppercase()
            else -> "${parts.first().first()}${parts.last().first()}".uppercase()
        }
    }

    fun colorForName(fullName: String): Color {
        val index = abs(fullName.hashCode()) % avatarPalette.size
        return avatarPalette[index]
    }
}
