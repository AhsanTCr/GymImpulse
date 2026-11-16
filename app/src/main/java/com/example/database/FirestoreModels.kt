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
)

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

    fun toOwnerUpdateMap(): Map<String, Any> = buildMap {
        put("memberName", memberName)
        if (!avatarData.isNullOrBlank()) put("avatarData", avatarData)
        put("currentStreakDays", currentStreakDays)
        put("longestStreakDays", longestStreakDays)
        put("monthlyVisits", monthlyVisits)
        put("weeklyTargetMetWeeks", weeklyTargetMetWeeks)
        put("healthyHabitsScore", healthyHabitsScore)
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
)

@IgnoreExtraProperties
data class AnnouncementDoc(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val dateLabel: String = "",
    val category: String = "EVENT",
    val isImportant: Boolean = false,
    val imageKey: String? = null,
    val ctaLabel: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class GymConfigDoc(
    @DocumentId val id: String = "",
    val gymId: String = "",
    val gymName: String = "",
    val staticEntranceCode: String = "",
    val rotationWindowSeconds: Long = 120L,
    val gymLatitude: Double? = null,
    val gymLongitude: Double? = null,
    val allowedRadiusMeters: Double? = null
)

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
}

@IgnoreExtraProperties
data class AppConfigDoc(
    @DocumentId val id: String = "",
    val latestVersion: String = "1.0.0",
    val releaseDate: String = "",
    val releaseHighlights: List<String> = emptyList()
)
