package com.example.database

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val memberCode: String = "",
    val fullName: String = "",
    val phone: String? = null,
    val email: String = "",
    val avatarData: String? = null,
    val role: String = "MEMBER",
    val joinedDate: String = "",
    val fitnessGoal: String? = null,
    val referralCode: String = "",
    val referredByCode: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("memberCode", memberCode)
        put("fullName", fullName)
        if (!phone.isNullOrBlank()) put("phone", phone)
        put("email", email)
        if (!avatarData.isNullOrBlank()) put("avatarData", avatarData)
        put("role", "MEMBER")
        put("joinedDate", joinedDate)
        if (!fitnessGoal.isNullOrBlank()) put("fitnessGoal", fitnessGoal)
        put("referralCode", referralCode)
        if (!referredByCode.isNullOrBlank()) put("referredByCode", referredByCode)
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }

    fun toOwnerUpdateMap(
        newFullName: String,
        newPhone: String?,
        newAvatarData: String?,
        newFitnessGoal: String?
    ): Map<String, Any> = buildMap {
        put("fullName", newFullName)
        if (newPhone != null) put("phone", newPhone)
        if (newAvatarData != null) put("avatarData", newAvatarData)
        if (newFitnessGoal != null) put("fitnessGoal", newFitnessGoal)
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class MembershipRecordDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val planName: String = "",
    val monthlyFeeUsd: Double = 69.0,
    val membershipStartDate: String = "",
    val cycleStartDate: String = "",
    val cycleEndDate: String = "",
    val nextRenewalDate: String = "",
    val status: String = "EXPIRED",
    val autoRenewEnabled: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "planName" to planName,
        "monthlyFeeUsd" to monthlyFeeUsd,
        "membershipStartDate" to membershipStartDate,
        "cycleStartDate" to cycleStartDate,
        "cycleEndDate" to cycleEndDate,
        "nextRenewalDate" to nextRenewalDate,
        "status" to status,
        "autoRenewEnabled" to autoRenewEnabled,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class PaymentRequestDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val memberName: String = "",
    val memberCode: String = "",
    val amountFormatted: String = "",
    val paymentMethod: String = "",
    val referenceNote: String? = null,
    val paymentDateLabel: String = "",
    val status: String = "PENDING_VERIFICATION",
    val isVerified: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("memberName", memberName)
        put("memberCode", memberCode)
        put("amountFormatted", amountFormatted)
        put("paymentMethod", paymentMethod)
        if (!referenceNote.isNullOrBlank()) put("referenceNote", referenceNote)
        put("paymentDateLabel", paymentDateLabel)
        put("status", "PENDING_VERIFICATION")
        put("isVerified", false)
        put("createdAt", FieldValue.serverTimestamp())
    }

    fun toAdminResolutionMap(approved: Boolean): Map<String, Any> = buildMap {
        put("userId", userId)
        put("memberName", memberName)
        put("memberCode", memberCode)
        put("amountFormatted", amountFormatted)
        put("paymentMethod", paymentMethod)
        if (!referenceNote.isNullOrBlank()) put("referenceNote", referenceNote)
        put("paymentDateLabel", paymentDateLabel)
        put("status", if (approved) "VERIFIED" else "REJECTED")
        put("isVerified", approved)
        put("createdAt", createdAt ?: FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class PaymentRecordDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val paymentDate: String = "",
    val cycleCovered: String = "",
    val amountFormatted: String = "",
    val method: String = "",
    val referenceNumber: String = "",
    val isVerified: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "paymentDate" to paymentDate,
        "cycleCovered" to cycleCovered,
        "amountFormatted" to amountFormatted,
        "method" to method,
        "referenceNumber" to referenceNumber,
        "isVerified" to isVerified,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class AttendanceRecordDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val dateLabel: String = "",
    val checkInTime: String = "",
    val zoneLabel: String = "",
    val gymCode: String = "",
    val verificationToken: String = "",
    val verifiedViaCommonQr: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "dateKey" to dateKey,
        "dateLabel" to dateLabel,
        "checkInTime" to checkInTime,
        "zoneLabel" to zoneLabel,
        "gymCode" to gymCode,
        "verificationToken" to verificationToken,
        "verifiedViaCommonQr" to true,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class DailyFoodLogDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val dateLabel: String = "",
    val takenFoodIds: List<String> = emptyList(),
    val servingsByFood: Map<String, Long> = emptyMap(),
    val totalProteinGrams: Long = 0L,
    val totalCalories: Long = 0L,
    val foodsCount: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "dateKey" to dateKey,
        "dateLabel" to dateLabel,
        "takenFoodIds" to takenFoodIds,
        "servingsByFood" to servingsByFood,
        "totalProteinGrams" to totalProteinGrams,
        "totalCalories" to totalCalories,
        "foodsCount" to foodsCount,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class MemberSettingsDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val attendanceReminders: Boolean = true,
    val nutritionGoalReminders: Boolean = true,
    val membershipExpiryAlerts: Boolean = true,
    val gymAnnouncements: Boolean = true,
    val leaderboardUpdates: Boolean = false,
    val morningFuelEnabled: Boolean = true,
    val postWorkoutProteinEnabled: Boolean = true,
    val eveningCheckInEnabled: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "attendanceReminders" to attendanceReminders,
        "nutritionGoalReminders" to nutritionGoalReminders,
        "membershipExpiryAlerts" to membershipExpiryAlerts,
        "gymAnnouncements" to gymAnnouncements,
        "leaderboardUpdates" to leaderboardUpdates,
        "morningFuelEnabled" to morningFuelEnabled,
        "postWorkoutProteinEnabled" to postWorkoutProteinEnabled,
        "eveningCheckInEnabled" to eveningCheckInEnabled,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class LeaderboardStatDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val memberName: String = "",
    val avatarData: String? = null,
    val currentStreakDays: Long = 0L,
    val longestStreakDays: Long = 0L,
    val monthlyVisits: Long = 0L,
    val weeklyTargetMetWeeks: Long = 0L,
    val healthyHabitsScore: Long = 0L,
    val verifiedReferralsCount: Long = 0L,
    val isMonthlyWinner: Boolean = false,
    val monthKey: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("memberName", memberName)
        if (!avatarData.isNullOrBlank()) put("avatarData", avatarData)
        put("currentStreakDays", currentStreakDays)
        put("longestStreakDays", longestStreakDays)
        put("monthlyVisits", monthlyVisits)
        put("weeklyTargetMetWeeks", weeklyTargetMetWeeks)
        put("healthyHabitsScore", healthyHabitsScore)
        put("verifiedReferralsCount", 0L)
        put("isMonthlyWinner", false)
        put("monthKey", monthKey)
        put("updatedAt", FieldValue.serverTimestamp())
    }

    fun toAdminWriteMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("memberName", memberName)
        if (!avatarData.isNullOrBlank()) put("avatarData", avatarData)
        put("currentStreakDays", currentStreakDays)
        put("longestStreakDays", longestStreakDays)
        put("monthlyVisits", monthlyVisits)
        put("weeklyTargetMetWeeks", weeklyTargetMetWeeks)
        put("healthyHabitsScore", healthyHabitsScore)
        put("verifiedReferralsCount", verifiedReferralsCount)
        put("isMonthlyWinner", isMonthlyWinner)
        put("monthKey", monthKey)
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class LeaderboardHistoryDoc(
    @DocumentId val id: String = "",
    val monthKey: String = "",
    val category: String = "",
    val winnerName: String = "",
    val scoreLabel: String = ""
) {
    fun toWriteMap(): Map<String, Any> = mapOf(
        "monthKey" to monthKey,
        "category" to category,
        "winnerName" to winnerName,
        "scoreLabel" to scoreLabel
    )
}

@IgnoreExtraProperties
data class AnnouncementDoc(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val dateLabel: String = "",
    val category: String = "EVENT",
    val isImportant: Boolean = false,
    val isPublished: Boolean = true,
    val scheduledAtLabel: String? = null,
    val imageKey: String? = null,
    val imageData: String? = null,
    val ctaLabel: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> = buildMap {
        put("title", title)
        put("description", description)
        put("dateLabel", dateLabel)
        put("category", category)
        put("isImportant", isImportant)
        put("isPublished", isPublished)
        if (!scheduledAtLabel.isNullOrBlank()) put("scheduledAtLabel", scheduledAtLabel)
        if (!imageKey.isNullOrBlank()) put("imageKey", imageKey)
        if (!imageData.isNullOrBlank()) put("imageData", imageData)
        if (!ctaLabel.isNullOrBlank()) put("ctaLabel", ctaLabel)
        put("createdAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class GymConfigDoc(
    @DocumentId val id: String = "",
    val gymId: String = "",
    val gymName: String = "",
    val staticEntranceCode: String = "",
    val rotationWindowSeconds: Long = 120L,
    val gymLatitude: Double? = null,
    val gymLongitude: Double? = null,
    val allowedRadiusMeters: Double? = null,
    val isQrEnabled: Boolean = true,
    val allowStaticFallback: Boolean = true,
    val qrVersion: Long = 1L,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> = buildMap {
        put("gymId", gymId)
        put("gymName", gymName)
        put("staticEntranceCode", staticEntranceCode)
        put("rotationWindowSeconds", rotationWindowSeconds)
        if (gymLatitude != null) put("gymLatitude", gymLatitude)
        if (gymLongitude != null) put("gymLongitude", gymLongitude)
        if (allowedRadiusMeters != null) put("allowedRadiusMeters", allowedRadiusMeters)
        put("isQrEnabled", isQrEnabled)
        put("allowStaticFallback", allowStaticFallback)
        put("qrVersion", qrVersion)
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class ReferralRecordDoc(
    @DocumentId val id: String = "",
    val referrerUserId: String? = null,
    val referrerCode: String = "",
    val referredUserId: String = "",
    val referredName: String = "",
    val status: String = "PENDING_VERIFICATION",
    val isVerified: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        if (!referrerUserId.isNullOrBlank()) put("referrerUserId", referrerUserId)
        put("referrerCode", referrerCode)
        put("referredUserId", referredUserId)
        put("referredName", referredName)
        put("status", "PENDING_VERIFICATION")
        put("isVerified", false)
        put("createdAt", FieldValue.serverTimestamp())
    }

    fun toAdminUpdateMap(approved: Boolean, resolvedReferrerUid: String?): Map<String, Any> = buildMap {
        val refUid = resolvedReferrerUid ?: referrerUserId
        if (!refUid.isNullOrBlank()) put("referrerUserId", refUid)
        put("referrerCode", referrerCode)
        put("referredUserId", referredUserId)
        put("referredName", referredName)
        put("status", if (approved) "VERIFIED" else "REJECTED")
        put("isVerified", approved)
        put("createdAt", createdAt ?: FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class AppConfigDoc(
    @DocumentId val id: String = "",
    val latestVersion: String = "1.0.0",
    val releaseDate: String = "",
    val releaseHighlights: List<String> = emptyList(),
    val githubRepo: String? = null,
    val downloadUrl: String? = null
) {
    fun toWriteMap(): Map<String, Any> = buildMap {
        put("latestVersion", latestVersion)
        put("releaseDate", releaseDate)
        put("releaseHighlights", releaseHighlights)
        if (!githubRepo.isNullOrBlank()) put("githubRepo", githubRepo)
        if (!downloadUrl.isNullOrBlank()) put("downloadUrl", downloadUrl)
    }
}

@IgnoreExtraProperties
data class CompetitionConfigDoc(
    @DocumentId val id: String = "",
    val categoryId: String = "",
    val title: String = "",
    val periodLabel: String = "",
    val targetDescription: String = "",
    val targetValue: Long = 0L,
    val rewardTitle: String = "",
    val rewardDescription: String? = null,
    val isActive: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> = buildMap {
        put("categoryId", categoryId)
        put("title", title)
        put("periodLabel", periodLabel)
        put("targetDescription", targetDescription)
        put("targetValue", targetValue)
        put("rewardTitle", rewardTitle)
        if (!rewardDescription.isNullOrBlank()) put("rewardDescription", rewardDescription)
        put("isActive", isActive)
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class PromotionalOfferDoc(
    @DocumentId val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val badgeText: String = "SPECIAL OFFER",
    val discountCode: String? = null,
    val validUntilLabel: String = "",
    val targetAudience: String = "ALL_MEMBERS",
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> = buildMap {
        put("title", title)
        put("subtitle", subtitle)
        put("badgeText", badgeText)
        if (!discountCode.isNullOrBlank()) put("discountCode", discountCode)
        put("validUntilLabel", validUntilLabel)
        put("targetAudience", targetAudience)
        put("isActive", isActive)
        put("createdAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class BroadcastNotificationDoc(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "GENERAL",
    val audience: String = "ALL_MEMBERS",
    val isUrgent: Boolean = false,
    val createdByAdminUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> = mapOf(
        "title" to title,
        "message" to message,
        "type" to type,
        "audience" to audience,
        "isUrgent" to isUrgent,
        "createdByAdminUid" to createdByAdminUid,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class QrSecurityEventDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val memberName: String = "",
    val memberCode: String? = null,
    val eventType: String = "INVALID_CODE",
    val reason: String = "",
    val rawCodePrefix: String? = null,
    val distanceMeters: Double? = null,
    val reviewed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("memberName", memberName)
        if (!memberCode.isNullOrBlank()) put("memberCode", memberCode)
        put("eventType", eventType)
        put("reason", reason)
        if (!rawCodePrefix.isNullOrBlank()) put("rawCodePrefix", rawCodePrefix)
        if (distanceMeters != null) put("distanceMeters", distanceMeters)
        put("reviewed", false)
        put("createdAt", FieldValue.serverTimestamp())
    }

    fun toMarkReviewedMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("memberName", memberName)
        if (!memberCode.isNullOrBlank()) put("memberCode", memberCode)
        put("eventType", eventType)
        put("reason", reason)
        if (!rawCodePrefix.isNullOrBlank()) put("rawCodePrefix", rawCodePrefix)
        if (distanceMeters != null) put("distanceMeters", distanceMeters)
        put("reviewed", true)
        put("createdAt", createdAt ?: FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class AdminAuditLogDoc(
    @DocumentId val id: String = "",
    val adminUserId: String = "",
    val adminEmail: String = "",
    val actionType: String = "",
    val targetId: String = "",
    val summary: String = "",
    val details: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("adminUserId", adminUserId)
        put("adminEmail", adminEmail)
        put("actionType", actionType)
        put("targetId", targetId)
        put("summary", summary)
        if (!details.isNullOrBlank()) put("details", details)
        put("createdAt", FieldValue.serverTimestamp())
    }
}
