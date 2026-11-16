package com.example.utils

import com.example.config.AppConfig
import com.example.database.AttendanceRecordDoc
import com.example.database.MembershipRecordDoc
import com.example.database.PaymentRecordDoc
import com.example.database.PaymentRequestDoc
import com.example.database.QrSecurityEventDoc
import com.example.database.ReferralRecordDoc
import com.example.database.UserProfileDoc
import com.example.models.AdminAlertItem
import com.example.models.AdminDashboardModule
import com.example.models.AdminMemberSummary
import com.example.models.AdminOverviewMetrics
import com.example.models.AdminOverviewPeriod
import com.example.models.DailyAttendanceTrendPoint
import com.example.models.MemberActivitySegment
import com.example.models.MembershipStatus
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Pure domain analytics engine for Gymimpulse Admin Portal.
 * Computes club KPIs, attendance trends, member engagement segments, and passcode hashes.
 */
object AdminAnalyticsEngine {

    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val shortDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
    private val trendDayFormat = SimpleDateFormat("MMM d", Locale.US)

    fun sha256Hex(raw: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verifyAdminPasscode(passcode: String): Boolean {
        val trimmed = passcode.trim()
        if (trimmed.isEmpty()) return false
        return sha256Hex(trimmed) == AppConfig.ADMIN_PASSCODE_SHA256
    }

    fun buildMemberSummaries(
        users: List<UserProfileDoc>,
        memberships: List<MembershipRecordDoc>,
        attendanceRecords: List<AttendanceRecordDoc>,
        paymentRequests: List<PaymentRequestDoc>,
        verifiedPayments: List<PaymentRecordDoc>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<AdminMemberSummary> {
        val membershipByUser = memberships.associateBy { it.userId.ifBlank { "" } }
        val attendanceByUser = attendanceRecords.groupBy { it.userId }
        val requestsByUser = paymentRequests.groupBy { it.userId }
        val paymentsByUser = verifiedPayments.groupBy { it.userId }

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val cutoff7DaysKey = dateKeyOffset(cal, -6)
        val cutoff14DaysKey = dateKeyOffset(cal, -13)
        val cutoff28DaysKey = dateKeyOffset(cal, -27)
        val cutoff30DaysKey = dateKeyOffset(cal, -29)

        return users
            .distinctBy { it.userId.ifBlank { it.id } }
            .map { user ->
                val uid = user.userId.ifBlank { user.id }
                val membership = membershipByUser[uid]
                val derivedStatus = if (membership != null) {
                    MembershipCycleCalculator.parseStatus(membership.status, membership.cycleEndDate, nowMillis)
                } else {
                    MembershipStatus.EXPIRED
                }

                val userAtt = (attendanceByUser[uid] ?: emptyList()).sortedByDescending { it.dateKey }
                val metrics = StreakAndLeaderboardEngine.computeAttendanceMetrics(userAtt, cal)

                val uniqueDates = userAtt.map { it.dateKey }.distinct()
                val visits7d = uniqueDates.count { it >= cutoff7DaysKey }
                val visits30d = uniqueDates.count { it >= cutoff30DaysKey }
                val recent14Visits = uniqueDates.count { it >= cutoff14DaysKey }
                val prior14Visits = uniqueDates.count { it >= cutoff28DaysKey && it < cutoff14DaysKey }

                val joinedRecently = runCatching {
                    val joinedMillis = shortDateFormat.parse(user.joinedDate)?.time ?: return@runCatching false
                    val daysSinceJoin = TimeUnit.MILLISECONDS.toDays(nowMillis - joinedMillis)
                    daysSinceJoin in 0..21
                }.getOrDefault(false)

                val segments = buildSet {
                    add(MemberActivitySegment.ALL)
                    if (visits7d >= 3 || visits30d >= 8) add(MemberActivitySegment.REGULAR)
                    if (recent14Visits == 0) add(MemberActivitySegment.INACTIVE)
                    if (joinedRecently) add(MemberActivitySegment.RECENTLY_JOINED)
                    if (prior14Visits >= 2 && recent14Visits * 2 <= prior14Visits) add(MemberActivitySegment.DECLINING)
                    if (metrics.currentStreakDays >= 5) add(MemberActivitySegment.STRONG_STREAK)
                }

                AdminMemberSummary(
                    userId = uid,
                    profile = user,
                    membership = membership,
                    derivedStatus = derivedStatus,
                    attendanceRecords = userAtt,
                    paymentRequests = (requestsByUser[uid] ?: emptyList())
                        .sortedByDescending { it.createdAt?.seconds ?: 0L },
                    verifiedPayments = (paymentsByUser[uid] ?: emptyList())
                        .sortedByDescending { it.createdAt?.seconds ?: 0L },
                    currentStreakDays = metrics.currentStreakDays,
                    longestStreakDays = metrics.longestStreakDays,
                    visitsLast7Days = visits7d,
                    visitsLast30Days = visits30d,
                    totalVisits = uniqueDates.size,
                    activitySegments = segments,
                    lastCheckInDateKey = uniqueDates.firstOrNull()
                )
            }
            .sortedWith(
                compareByDescending<AdminMemberSummary> {
                    it.paymentRequests.any { req -> req.status == "PENDING_VERIFICATION" }
                }.thenByDescending { it.visitsLast7Days }
                    .thenBy { it.profile.fullName.lowercase() }
            )
    }

    fun computeOverviewMetrics(
        summaries: List<AdminMemberSummary>,
        attendanceRecords: List<AttendanceRecordDoc>,
        paymentRequests: List<PaymentRequestDoc>,
        referrals: List<ReferralRecordDoc>,
        qrSecurityEvents: List<QrSecurityEventDoc>,
        period: AdminOverviewPeriod,
        nowMillis: Long = System.currentTimeMillis()
    ): AdminOverviewMetrics {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val todayKey = dateKeyFormat.format(cal.time)
        val cutoffKey = dateKeyOffset(cal, -(period.days - 1))

        val total = summaries.size
        val active = summaries.count { it.derivedStatus == MembershipStatus.ACTIVE }
        val expiring = summaries.count { it.derivedStatus == MembershipStatus.EXPIRING_SOON }
        val expired = summaries.count { it.derivedStatus == MembershipStatus.EXPIRED }
        val overdue = summaries.count { it.derivedStatus == MembershipStatus.PAYMENT_OVERDUE }

        val todayAttendance = attendanceRecords
            .filter { it.dateKey == todayKey }
            .distinctBy { it.userId }
            .size

        val denominator = total.coerceAtLeast(1)
        val attendanceRate = ((todayAttendance * 100f) / denominator).toInt().coerceIn(0, 100)

        val periodTotal = attendanceRecords
            .filter { it.dateKey >= cutoffKey && it.dateKey <= todayKey }
            .distinctBy { "${it.userId}_${it.dateKey}" }
            .size

        val pendingPayments = paymentRequests.count { !it.isVerified && it.status == "PENDING_VERIFICATION" }
        val pendingReferrals = referrals.count { !it.isVerified && it.status == "PENDING_VERIFICATION" }
        val unreviewedQrEvents = qrSecurityEvents.count { !it.reviewed }

        // Build daily attendance bar trend
        val daysToPlot = when (period) {
            AdminOverviewPeriod.TODAY -> 7
            AdminOverviewPeriod.SEVEN_DAYS -> 7
            AdminOverviewPeriod.THIRTY_DAYS -> 10
        }
        val countsByDate = attendanceRecords
            .distinctBy { "${it.userId}_${it.dateKey}" }
            .groupingBy { it.dateKey }
            .eachCount()

        val trend = (daysToPlot - 1 downTo 0).map { offset ->
            val dayCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val key = dateKeyFormat.format(dayCal.time)
            val label = if (offset == 0) "Today" else trendDayFormat.format(dayCal.time)
            DailyAttendanceTrendPoint(
                dateKey = key,
                label = label,
                count = countsByDate[key] ?: 0
            )
        }

        val alerts = buildList {
            if (pendingPayments > 0) {
                add(
                    AdminAlertItem(
                        id = "alert_pending_payments",
                        title = "$pendingPayments Payment Request(s) Awaiting Verification",
                        description = "Review bank transfers or desk receipts and confirm actual payment dates.",
                        severity = "CRITICAL",
                        targetModule = AdminDashboardModule.MEMBER_INSIGHTS
                    )
                )
            }
            if (overdue > 0) {
                add(
                    AdminAlertItem(
                        id = "alert_overdue",
                        title = "$overdue Payment Overdue Membership(s)",
                        description = "Follow up on overdue accounts before restricting entrance QR check-ins.",
                        severity = "CRITICAL",
                        targetModule = AdminDashboardModule.MEMBER_INSIGHTS
                    )
                )
            }
            if (expiring > 0) {
                add(
                    AdminAlertItem(
                        id = "alert_expiring",
                        title = "$expiring Membership(s) Expiring Within 5 Days",
                        description = "Send a targeted renewal reminder or offer from Member Engagement.",
                        severity = "WARNING",
                        targetModule = AdminDashboardModule.MEMBER_ENGAGEMENT
                    )
                )
            }
            if (unreviewedQrEvents > 0) {
                add(
                    AdminAlertItem(
                        id = "alert_qr_events",
                        title = "$unreviewedQrEvents Suspicious / Blocked QR Scan Alert(s)",
                        description = "Inspect expired tokens, duplicate attempts, or out-of-range entrance scans.",
                        severity = "WARNING",
                        targetModule = AdminDashboardModule.GYM_OVERVIEW
                    )
                )
            }
            if (pendingReferrals > 0) {
                add(
                    AdminAlertItem(
                        id = "alert_referrals",
                        title = "$pendingReferrals Referral Claim(s) Pending Verification",
                        description = "Verify referred new members in Competition Control to update Referral Champion ranks.",
                        severity = "INFO",
                        targetModule = AdminDashboardModule.COMPETITION_CONTROL
                    )
                )
            }
        }

        return AdminOverviewMetrics(
            totalMembers = total,
            activeMembers = active,
            expiringSoonMembers = expiring,
            expiredMembers = expired,
            overdueMembers = overdue,
            todayAttendanceCount = todayAttendance,
            attendanceRatePercent = attendanceRate,
            periodAttendanceTotal = periodTotal,
            pendingPaymentRequestsCount = pendingPayments,
            pendingReferralsCount = pendingReferrals,
            unreviewedQrEventsCount = unreviewedQrEvents,
            attendanceTrend = trend,
            alerts = alerts
        )
    }

    private fun dateKeyOffset(base: Calendar, offsetDays: Int): String {
        val copy = (base.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offsetDays) }
        return dateKeyFormat.format(copy.time)
    }
}
