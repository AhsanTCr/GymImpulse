package com.example.features.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.AppConfig
import com.example.database.AnnouncementDoc
import com.example.database.AttendanceRecordDoc
import com.example.database.BroadcastNotificationDoc
import com.example.database.DailyFoodLogDoc
import com.example.database.LeaderboardStatDoc
import com.example.database.MemberFirestoreRepository
import com.example.database.MembershipRecordDoc
import com.example.database.PaymentRecordDoc
import com.example.database.PromotionalOfferDoc
import com.example.database.ReferralRecordDoc
import com.example.database.UserProfileDoc
import com.example.database.toFriendlyFirestoreMessage
import com.example.models.AnnouncementCategory
import com.example.models.GymAnnouncement
import com.example.models.LeaderboardCategory
import com.example.models.MemberProfile
import com.example.models.MembershipOverview
import com.example.models.MembershipStatus
import com.example.models.PersonalStats
import com.example.models.TodayAttendanceState
import com.example.models.UserRole
import com.example.utils.DateTimeUtils
import com.example.utils.MembershipCycleCalculator
import com.example.utils.NutritionAnalyticsEngine
import com.example.utils.StreakAndLeaderboardEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class HomeDashboardUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val greeting: String = DateTimeUtils.greetingForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)),
    val member: MemberProfile? = null,
    val membership: MembershipOverview? = null,
    val todayAttendance: TodayAttendanceState = TodayAttendanceState.NOT_CHECKED_IN,
    val proteinLoggedGrams: Int = 0,
    val proteinGoalGrams: Int = AppConfig.DEFAULT_PROTEIN_GOAL_GRAMS,
    val foodsLoggedCount: Int = 0,
    val totalFoodsCount: Int = NutritionAnalyticsEngine.catalogFoods.size,
    val latestAnnouncement: GymAnnouncement? = null,
    val latestBroadcast: BroadcastNotificationDoc? = null,
    val activeOffer: PromotionalOfferDoc? = null
)

private data class PrivateUserStreams(
    val profile: UserProfileDoc?,
    val membership: MembershipRecordDoc?,
    val payments: List<PaymentRecordDoc>,
    val attendance: List<AttendanceRecordDoc>
)

private data class ClubAndActivityStreams(
    val foodLogs: List<DailyFoodLogDoc>,
    val leaderboardStats: List<LeaderboardStatDoc>,
    val announcements: List<AnnouncementDoc>,
    val referrals: List<ReferralRecordDoc>
)

class HomeDashboardViewModel(
    private val repository: MemberFirestoreRepository,
    private val currentUserId: String
) : ViewModel() {

    private val privateStreams = combine(
        repository.observeUserProfile(currentUserId),
        repository.observeMembership(currentUserId),
        repository.observePaymentHistory(currentUserId),
        repository.observeAttendanceRecords(currentUserId)
    ) { profile, membership, payments, attendance ->
        PrivateUserStreams(profile, membership, payments, attendance)
    }

    private val clubStreams = combine(
        repository.observeFoodLogs(currentUserId),
        repository.observeLeaderboardStats().catch { emit(emptyList()) },
        repository.observeAnnouncements().catch { emit(emptyList()) },
        repository.observeMyReferrals(currentUserId).catch { emit(emptyList()) }
    ) { foodLogs, leaderboardStats, announcements, referrals ->
        ClubAndActivityStreams(foodLogs, leaderboardStats, announcements, referrals)
    }

    private val engagementStreams = combine(
        repository.observePromotionalOffers().catch { emit(emptyList()) },
        repository.observeBroadcastNotifications().catch { emit(emptyList()) }
    ) { offers, broadcasts ->
        offers to broadcasts
    }

    val uiState: StateFlow<HomeDashboardUiState> = combine(
        privateStreams,
        clubStreams,
        engagementStreams
    ) { priv, club, engagement ->
        val profileDoc = priv.profile
        if (profileDoc == null) {
            return@combine HomeDashboardUiState(isLoading = true)
        }

        val cal = Calendar.getInstance()
        val todayKey = repository.todayDateKey(cal)
        val attendanceMetrics = StreakAndLeaderboardEngine.computeAttendanceMetrics(priv.attendance, cal)

        val ranked = StreakAndLeaderboardEngine.rankEntries(
            stats = club.leaderboardStats,
            category = LeaderboardCategory.ATTENDANCE_STREAK,
            currentUserId = currentUserId
        )
        val myRank = ranked.firstOrNull { it.memberId == currentUserId }?.rank ?: 0
        val lbVerifiedRefs = club.leaderboardStats
            .firstOrNull { it.userId == currentUserId }
            ?.verifiedReferralsCount?.toInt() ?: 0
        val verifiedReferrals = maxOf(lbVerifiedRefs, club.referrals.count { it.isVerified })

        val member = MemberProfile(
            id = profileDoc.userId.ifBlank { currentUserId },
            memberCode = profileDoc.memberCode.ifBlank { "GP-MEMBER" },
            fullName = profileDoc.fullName.ifBlank { "Club Member" },
            phone = profileDoc.phone.orEmpty().ifBlank { "Not added" },
            email = profileDoc.email,
            avatarUri = profileDoc.avatarData,
            role = if (profileDoc.role.equals("ADMIN", ignoreCase = true)) UserRole.ADMIN else UserRole.MEMBER,
            joinedDate = profileDoc.joinedDate,
            fitnessGoal = profileDoc.fitnessGoal ?: "Strength & ${AppConfig.DEFAULT_PROTEIN_GOAL_GRAMS}g Daily Protein",
            stats = PersonalStats(
                currentStreakDays = attendanceMetrics.currentStreakDays,
                longestStreakDays = attendanceMetrics.longestStreakDays,
                totalWorkouts = attendanceMetrics.totalWorkouts,
                monthlyWorkouts = attendanceMetrics.monthlyVisits,
                weeklyTargetMetWeeks = attendanceMetrics.weeklyTargetMetWeeks,
                referralsCount = verifiedReferrals,
                leaderboardRank = myRank,
                totalGymMembers = club.leaderboardStats.size.coerceAtLeast(1)
            )
        )

        val membershipOverview = MembershipCycleCalculator.toOverview(
            record = priv.membership,
            payments = priv.payments,
            nowMillis = cal.timeInMillis
        )

        val checkedInToday = priv.attendance.any { it.dateKey == todayKey }
        val todayStatus = if (checkedInToday) TodayAttendanceState.CHECKED_IN else TodayAttendanceState.NOT_CHECKED_IN

        val todayLog = club.foodLogs.firstOrNull { it.dateKey == todayKey }
        val proteinLogged = todayLog?.totalProteinGrams?.toInt() ?: 0
        val foodsLoggedCount = todayLog?.foodsCount?.toInt() ?: 0

        val latestAnn = club.announcements.firstOrNull()?.toDomainModel()
        val matchingOffer = engagement.first.firstOrNull {
            matchesMemberAudience(it.targetAudience, membershipOverview.status)
        }
        val matchingBroadcast = engagement.second.firstOrNull {
            matchesMemberAudience(it.audience, membershipOverview.status)
        }

        HomeDashboardUiState(
            isLoading = false,
            errorMessage = null,
            greeting = DateTimeUtils.greetingForHour(cal.get(Calendar.HOUR_OF_DAY)),
            member = member,
            membership = membershipOverview,
            todayAttendance = todayStatus,
            proteinLoggedGrams = proteinLogged,
            proteinGoalGrams = AppConfig.DEFAULT_PROTEIN_GOAL_GRAMS,
            foodsLoggedCount = foodsLoggedCount,
            totalFoodsCount = NutritionAnalyticsEngine.catalogFoods.size,
            latestAnnouncement = latestAnn,
            latestBroadcast = matchingBroadcast,
            activeOffer = matchingOffer
        )
    }.catch { error ->
        emit(
            HomeDashboardUiState(
                isLoading = false,
                errorMessage = error.toFriendlyFirestoreMessage("Unable to load dashboard data.")
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HomeDashboardUiState(isLoading = true)
    )
}

internal fun AnnouncementDoc.toDomainModel(): GymAnnouncement {
    val parsedCategory = when (category.uppercase()) {
        "IMPORTANT" -> AnnouncementCategory.IMPORTANT
        "COMPETITION" -> AnnouncementCategory.COMPETITION
        else -> AnnouncementCategory.EVENT
    }
    val bannerRes = when (imageKey?.lowercase()) {
        "competition" -> com.example.R.drawable.img_gym_event_competition
        "hero" -> com.example.R.drawable.img_gym_hero_banner
        else -> null
    }
    return GymAnnouncement(
        id = id.ifBlank { title.hashCode().toString() },
        title = title,
        description = description,
        dateLabel = dateLabel,
        category = parsedCategory,
        isImportant = isImportant,
        isPublished = isPublished,
        scheduledAtLabel = scheduledAtLabel,
        bannerDrawableRes = bannerRes,
        bannerImageData = imageData,
        ctaLabel = ctaLabel
    )
}

private fun matchesMemberAudience(audience: String, status: MembershipStatus): Boolean {
    return when (audience.uppercase()) {
        "ACTIVE_MEMBERS" -> status == MembershipStatus.ACTIVE || status == MembershipStatus.EXPIRING_SOON
        "EXPIRING_SOON" -> status == MembershipStatus.EXPIRING_SOON
        "OVERDUE" -> status == MembershipStatus.PAYMENT_OVERDUE || status == MembershipStatus.EXPIRED
        else -> true
    }
}
