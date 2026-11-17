package com.example.models

import com.example.database.AttendanceRecordDoc
import com.example.database.MembershipRecordDoc
import com.example.database.PaymentRecordDoc
import com.example.database.PaymentRequestDoc
import com.example.database.UserProfileDoc

enum class AdminDashboardModule(val title: String, val shortLabel: String) {
    GYM_OVERVIEW("Gym Overview", "Overview"),
    MEMBER_INSIGHTS("Member Insights", "Members"),
    COMPETITION_CONTROL("Competition Control", "Compete"),
    MEMBER_ENGAGEMENT("Member Engagement", "Engage")
}

enum class AdminOverviewPeriod(val label: String, val days: Int) {
    TODAY("Today", 1),
    SEVEN_DAYS("7 Days", 7),
    THIRTY_DAYS("30 Days", 30)
}

enum class MemberActivitySegment(val label: String) {
    ALL("All Members"),
    REGULAR("Regular"),
    INACTIVE("Inactive"),
    RECENTLY_JOINED("Recently Joined"),
    DECLINING("Declining"),
    STRONG_STREAK("Strong Streaks")
}

enum class BroadcastType(val label: String) {
    GENERAL("General Notice"),
    MEMBERSHIP_REMINDER("Membership Reminder"),
    COMPETITION("Competition Alert"),
    IMPORTANT_UPDATE("Important Update"),
    SPECIAL_OFFER("Special Offer")
}

enum class BroadcastAudience(val label: String) {
    ALL_MEMBERS("All Members"),
    ACTIVE_MEMBERS("Active Members"),
    EXPIRING_SOON("Expiring Soon"),
    OVERDUE("Overdue / Expired")
}

data class AdminAlertItem(
    val id: String,
    val title: String,
    val description: String,
    val severity: String, // CRITICAL, WARNING, INFO
    val targetModule: AdminDashboardModule
)

data class AdminMemberSummary(
    val userId: String,
    val profile: UserProfileDoc,
    val membership: MembershipRecordDoc?,
    val derivedStatus: MembershipStatus,
    val attendanceRecords: List<AttendanceRecordDoc>,
    val paymentRequests: List<PaymentRequestDoc>,
    val verifiedPayments: List<PaymentRecordDoc>,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val visitsLast7Days: Int,
    val visitsLast30Days: Int,
    val totalVisits: Int,
    val activitySegments: Set<MemberActivitySegment>,
    val lastCheckInDateKey: String?
)

data class DailyAttendanceTrendPoint(
    val dateKey: String,
    val label: String,
    val count: Int
)

data class AdminOverviewMetrics(
    val totalMembers: Int = 0,
    val activeMembers: Int = 0,
    val expiringSoonMembers: Int = 0,
    val expiredMembers: Int = 0,
    val overdueMembers: Int = 0,
    val todayAttendanceCount: Int = 0,
    val attendanceRatePercent: Int = 0,
    val periodAttendanceTotal: Int = 0,
    val pendingPaymentRequestsCount: Int = 0,
    val pendingReferralsCount: Int = 0,
    val unreviewedQrEventsCount: Int = 0,
    val attendanceTrend: List<DailyAttendanceTrendPoint> = emptyList(),
    val alerts: List<AdminAlertItem> = emptyList()
)

data class AdminModuleCardModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val isEnabled: Boolean = true
)
