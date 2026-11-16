package com.example.features.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.AppConfig
import com.example.database.AppConfigDoc
import com.example.database.AttendanceRecordDoc
import com.example.database.LeaderboardStatDoc
import com.example.database.MemberFirestoreRepository
import com.example.database.MembershipRecordDoc
import com.example.database.PaymentRecordDoc
import com.example.database.ReferralRecordDoc
import com.example.database.UserProfileDoc
import com.example.database.toFriendlyFirestoreMessage
import com.example.models.AppUpdateInfo
import com.example.models.AttendanceRecord
import com.example.models.LeaderboardCategory
import com.example.models.MemberProfile
import com.example.models.MembershipOverview
import com.example.models.PersonalStats
import com.example.models.UpdateAvailabilityState
import com.example.models.UserRole
import com.example.utils.ImageCompressionUtils
import com.example.utils.MembershipCycleCalculator
import com.example.utils.StreakAndLeaderboardEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class MemberProfileUiState(
    val isLoading: Boolean = true,
    val member: MemberProfile? = null,
    val referralCode: String = "",
    val membership: MembershipOverview? = null,
    val attendanceHistory: List<AttendanceRecord> = emptyList(),
    val updateInfo: AppUpdateInfo = AppUpdateInfo(
        installedVersion = AppConfig.INSTALLED_VERSION,
        latestVersion = AppConfig.INSTALLED_VERSION,
        releaseDate = "Current Release",
        availabilityState = UpdateAvailabilityState.UP_TO_DATE,
        releaseHighlights = listOf(
            "Live Google Sign-In & Firestore synchronization",
            "Common entrance QR scanner with rotating anti-fraud tokens",
            "Nutrition servings tracker with Missing Data analytics"
        )
    ),
    val profileStatusMessage: String? = null,
    val updateActionMessage: String? = null,
    val isSavingProfile: Boolean = false
)

private data class ProfileAccountStreams(
    val profile: UserProfileDoc?,
    val membership: MembershipRecordDoc?,
    val payments: List<PaymentRecordDoc>,
    val attendance: List<AttendanceRecordDoc>
)

private data class ProfileClubStreams(
    val leaderboardStats: List<LeaderboardStatDoc>,
    val referrals: List<ReferralRecordDoc>,
    val remoteConfig: AppConfigDoc?,
    val checkingUpdate: Boolean
)

class ProfileViewModel(
    private val repository: MemberFirestoreRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _remoteAppConfig = MutableStateFlow<AppConfigDoc?>(null)
    private val _checkingUpdate = MutableStateFlow(false)
    private val _updateActionMessage = MutableStateFlow<String?>(null)
    private val _profileStatusMessage = MutableStateFlow<String?>(null)
    private val _isSavingProfile = MutableStateFlow(false)

    init {
        refreshRemoteAppConfig()
    }

    private fun refreshRemoteAppConfig() {
        viewModelScope.launch {
            _remoteAppConfig.value = runCatching { repository.getAppConfig() }.getOrNull()
        }
    }

    private val accountStreams = combine(
        repository.observeUserProfile(currentUserId),
        repository.observeMembership(currentUserId),
        repository.observePaymentHistory(currentUserId),
        repository.observeAttendanceRecords(currentUserId)
    ) { profile, membership, payments, attendance ->
        ProfileAccountStreams(profile, membership, payments, attendance)
    }

    private val clubStreams = combine(
        repository.observeLeaderboardStats().catch { emit(emptyList()) },
        repository.observeMyReferrals(currentUserId).catch { emit(emptyList()) },
        _remoteAppConfig,
        _checkingUpdate
    ) { lbStats, referrals, remoteConfig, checking ->
        ProfileClubStreams(lbStats, referrals, remoteConfig, checking)
    }

    private val feedbackStreams = combine(
        _profileStatusMessage,
        _updateActionMessage,
        _isSavingProfile
    ) { profMsg, updMsg, saving ->
        Triple(profMsg, updMsg, saving)
    }

    val uiState: StateFlow<MemberProfileUiState> = combine(
        accountStreams,
        clubStreams,
        feedbackStreams
    ) { acct, club, feedback ->
        val profileDoc = acct.profile
        val cal = Calendar.getInstance()
        val metrics = StreakAndLeaderboardEngine.computeAttendanceMetrics(acct.attendance, cal)
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

        val member = profileDoc?.let {
            MemberProfile(
                id = it.userId.ifBlank { currentUserId },
                memberCode = it.memberCode.ifBlank { "GP-MEMBER" },
                fullName = it.fullName.ifBlank { "Club Member" },
                phone = it.phone.orEmpty().ifBlank { "Not added" },
                email = it.email,
                avatarUri = it.avatarData,
                role = if (it.role.equals("ADMIN", ignoreCase = true)) UserRole.ADMIN else UserRole.MEMBER,
                joinedDate = it.joinedDate,
                fitnessGoal = it.fitnessGoal ?: "Strength & ${AppConfig.DEFAULT_PROTEIN_GOAL_GRAMS}g Daily Protein",
                stats = PersonalStats(
                    currentStreakDays = metrics.currentStreakDays,
                    longestStreakDays = metrics.longestStreakDays,
                    totalWorkouts = metrics.totalWorkouts,
                    monthlyWorkouts = metrics.monthlyVisits,
                    weeklyTargetMetWeeks = metrics.weeklyTargetMetWeeks,
                    referralsCount = verifiedReferrals,
                    leaderboardRank = myRank,
                    totalGymMembers = club.leaderboardStats.size.coerceAtLeast(1)
                )
            )
        }

        val membershipOverview = MembershipCycleCalculator.toOverview(
            record = acct.membership,
            payments = acct.payments,
            nowMillis = cal.timeInMillis
        )

        val history = acct.attendance.map { rec ->
            AttendanceRecord(
                id = rec.id.ifBlank { rec.dateKey },
                dateLabel = rec.dateLabel.ifBlank { rec.dateKey },
                checkInTime = rec.checkInTime,
                zoneLabel = rec.zoneLabel.ifBlank { "Main Entrance Gate" },
                verifiedViaCommonQr = rec.verifiedViaCommonQr
            )
        }

        val remote = club.remoteConfig
        val latestVersion = remote?.latestVersion?.takeIf { it.isNotBlank() } ?: AppConfig.INSTALLED_VERSION
        val updateState = when {
            club.checkingUpdate -> UpdateAvailabilityState.CHECKING
            isNewerVersion(latestVersion, AppConfig.INSTALLED_VERSION) -> UpdateAvailabilityState.UPDATE_AVAILABLE
            else -> UpdateAvailabilityState.UP_TO_DATE
        }
        val highlights = remote?.releaseHighlights?.takeIf { it.isNotEmpty() } ?: listOf(
            "Real-time Firestore club sync & Google Sign-In",
            "Common entrance QR verification with anti-fraud checks",
            "Daily healthy food servings tracker & 7-day insights"
        )

        MemberProfileUiState(
            isLoading = profileDoc == null,
            member = member,
            referralCode = profileDoc?.referralCode.orEmpty(),
            membership = membershipOverview,
            attendanceHistory = history,
            updateInfo = AppUpdateInfo(
                installedVersion = AppConfig.INSTALLED_VERSION,
                latestVersion = latestVersion,
                releaseDate = remote?.releaseDate?.ifBlank { "Current Release" } ?: "Current Release",
                availabilityState = updateState,
                releaseHighlights = highlights
            ),
            profileStatusMessage = feedback.first,
            updateActionMessage = feedback.second,
            isSavingProfile = feedback.third
        )
    }.catch { error ->
        emit(
            MemberProfileUiState(
                isLoading = false,
                profileStatusMessage = error.toFriendlyFirestoreMessage("Unable to load profile.")
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = MemberProfileUiState(isLoading = true)
    )

    fun updateProfilePhoto(context: Context, uri: String?) {
        val currentMember = uiState.value.member ?: return
        if (uri.isNullOrBlank()) return
        _isSavingProfile.value = true
        _profileStatusMessage.value = null

        viewModelScope.launch {
            try {
                val compressed = ImageCompressionUtils.compressUriToDataUri(context, uri)
                if (compressed == null) {
                    _isSavingProfile.value = false
                    _profileStatusMessage.value = "Could not process selected photo."
                    return@launch
                }
                repository.updateMemberProfile(
                    fullName = currentMember.fullName,
                    phone = currentMember.phone.takeIf { it != "Not added" },
                    avatarData = compressed,
                    fitnessGoal = currentMember.fitnessGoal
                )
                _isSavingProfile.value = false
                _profileStatusMessage.value = "Profile picture updated."
            } catch (e: Exception) {
                _isSavingProfile.value = false
                _profileStatusMessage.value = e.toFriendlyFirestoreMessage("Could not save profile picture.")
            }
        }
    }

    fun updateProfileDetails(fullName: String, phone: String, fitnessGoal: String) {
        val currentMember = uiState.value.member ?: return
        val trimmedName = fullName.trim()
        if (trimmedName.length < 2) {
            _profileStatusMessage.value = "Name must be at least 2 characters."
            return
        }
        _isSavingProfile.value = true
        _profileStatusMessage.value = null

        viewModelScope.launch {
            try {
                repository.updateMemberProfile(
                    fullName = trimmedName,
                    phone = phone.trim().takeIf { it.isNotBlank() && it != "Not added" },
                    avatarData = currentMember.avatarUri,
                    fitnessGoal = fitnessGoal.trim().takeIf { it.isNotBlank() }
                )
                _isSavingProfile.value = false
                _profileStatusMessage.value = "Profile details saved."
            } catch (e: Exception) {
                _isSavingProfile.value = false
                _profileStatusMessage.value = e.toFriendlyFirestoreMessage("Failed to update profile.")
            }
        }
    }

    fun clearProfileMessage() {
        _profileStatusMessage.value = null
    }

    fun checkForAppUpdates() {
        if (_checkingUpdate.value) return
        _checkingUpdate.value = true
        _updateActionMessage.value = null
        viewModelScope.launch {
            try {
                val config = repository.getAppConfig()
                _remoteAppConfig.value = config
                _checkingUpdate.value = false
                val latest = config?.latestVersion?.takeIf { it.isNotBlank() } ?: AppConfig.INSTALLED_VERSION
                _updateActionMessage.value = if (isNewerVersion(latest, AppConfig.INSTALLED_VERSION)) {
                    "New GymPulse release v$latest is available."
                } else {
                    "You are running the latest version (v${AppConfig.INSTALLED_VERSION})."
                }
            } catch (e: Exception) {
                _checkingUpdate.value = false
                _updateActionMessage.value = e.toFriendlyFirestoreMessage("Unable to check app updates right now.")
            }
        }
    }

    private fun isNewerVersion(remote: String, installed: String): Boolean {
        val rParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val iParts = installed.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(rParts.size, iParts.size)
        for (idx in 0 until maxLen) {
            val r = rParts.getOrElse(idx) { 0 }
            val i = iParts.getOrElse(idx) { 0 }
            if (r > i) return true
            if (r < i) return false
        }
        return false
    }
}
