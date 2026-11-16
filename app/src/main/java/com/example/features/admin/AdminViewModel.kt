package com.example.features.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.AppConfig
import com.example.database.AdminAuditLogDoc
import com.example.database.AdminFirestoreRepository
import com.example.database.AnnouncementDoc
import com.example.database.AppConfigDoc
import com.example.database.AttendanceRecordDoc
import com.example.database.BroadcastNotificationDoc
import com.example.database.CompetitionConfigDoc
import com.example.database.GymConfigDoc
import com.example.database.LeaderboardHistoryDoc
import com.example.database.LeaderboardStatDoc
import com.example.database.MembershipRecordDoc
import com.example.database.PaymentRecordDoc
import com.example.database.PaymentRequestDoc
import com.example.database.PromotionalOfferDoc
import com.example.database.QrSecurityEventDoc
import com.example.database.ReferralRecordDoc
import com.example.database.UserProfileDoc
import com.example.database.toFriendlyFirestoreMessage
import com.example.integration.ExternalSyncPreview
import com.example.integration.StandardGymSyncBridge
import com.example.models.AdminDashboardModule
import com.example.models.AdminMemberSummary
import com.example.models.AdminOverviewMetrics
import com.example.models.AdminOverviewPeriod
import com.example.models.LeaderboardCategory
import com.example.models.LeaderboardEntry
import com.example.models.MemberActivitySegment
import com.example.utils.AdminAnalyticsEngine
import com.example.utils.ImageCompressionUtils
import com.example.utils.StreakAndLeaderboardEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class AdminRecentActivityItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val badgeLabel: String,
    val isAlert: Boolean
)

data class AdminUiState(
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val actionFeedback: String? = null,
    val isPasscodeVerified: Boolean = false,
    val passcodeError: String? = null,
    val selectedModule: AdminDashboardModule = AdminDashboardModule.GYM_OVERVIEW,
    val showQrManagement: Boolean = false,
    val selectedPeriod: AdminOverviewPeriod = AdminOverviewPeriod.SEVEN_DAYS,
    val selectedSegment: MemberActivitySegment = MemberActivitySegment.ALL,
    val searchQuery: String = "",
    val selectedCompCategory: LeaderboardCategory = LeaderboardCategory.ATTENDANCE_STREAK,
    val overviewMetrics: AdminOverviewMetrics = AdminOverviewMetrics(),
    val recentActivity: List<AdminRecentActivityItem> = emptyList(),
    val allMembers: List<AdminMemberSummary> = emptyList(),
    val filteredMembers: List<AdminMemberSummary> = emptyList(),
    val allAttendance: List<AttendanceRecordDoc> = emptyList(),
    val pendingPaymentRequests: List<PaymentRequestDoc> = emptyList(),
    val competitionConfigs: Map<LeaderboardCategory, CompetitionConfigDoc> = emptyMap(),
    val leaderboardRankings: List<LeaderboardEntry> = emptyList(),
    val leaderboardStats: List<LeaderboardStatDoc> = emptyList(),
    val leaderboardHistory: List<LeaderboardHistoryDoc> = emptyList(),
    val referrals: List<ReferralRecordDoc> = emptyList(),
    val announcements: List<AnnouncementDoc> = emptyList(),
    val promotionalOffers: List<PromotionalOfferDoc> = emptyList(),
    val broadcastNotifications: List<BroadcastNotificationDoc> = emptyList(),
    val qrSecurityEvents: List<QrSecurityEventDoc> = emptyList(),
    val auditLogs: List<AdminAuditLogDoc> = emptyList(),
    val gymConfig: GymConfigDoc = GymConfigDoc(
        id = "main_entrance",
        gymId = "main_entrance",
        gymName = AppConfig.GYM_NAME,
        staticEntranceCode = AppConfig.COMMON_ENTRANCE_QR_CODE,
        rotationWindowSeconds = AppConfig.QR_ROTATION_WINDOW_SECONDS,
        isQrEnabled = true,
        allowStaticFallback = true,
        qrVersion = 1L
    ),
    val appConfig: AppConfigDoc = AppConfigDoc(
        id = "current",
        latestVersion = AppConfig.INSTALLED_VERSION,
        releaseDate = "Current Release",
        releaseHighlights = listOf(
            "Phase 3 Admin Console & Analytics",
            "Live Entrance QR Terminal & Security Monitor",
            "Competition & Member Engagement Sync"
        )
    ),
    val exportedSyncPayload: String? = null,
    val syncPreview: ExternalSyncPreview? = null
)

private data class ClubMemberRecords(
    val users: List<UserProfileDoc>,
    val memberships: List<MembershipRecordDoc>,
    val attendance: List<AttendanceRecordDoc>,
    val paymentRequests: List<PaymentRequestDoc>,
    val payments: List<PaymentRecordDoc>
)

private data class ClubEngagementStreams(
    val announcements: List<AnnouncementDoc>,
    val competitionConfigs: List<CompetitionConfigDoc>,
    val promotionalOffers: List<PromotionalOfferDoc>,
    val broadcasts: List<BroadcastNotificationDoc>,
    val referrals: List<ReferralRecordDoc>
)

private data class ClubSystemStreams(
    val leaderboardStats: List<LeaderboardStatDoc>,
    val leaderboardHistory: List<LeaderboardHistoryDoc>,
    val qrEvents: List<QrSecurityEventDoc>,
    val auditLogs: List<AdminAuditLogDoc>,
    val gymConfig: GymConfigDoc?,
    val appConfig: AppConfigDoc?
)

private data class AdminControlsState(
    val isPasscodeVerified: Boolean,
    val passcodeError: String?,
    val selectedModule: AdminDashboardModule,
    val showQrManagement: Boolean,
    val selectedPeriod: AdminOverviewPeriod,
    val selectedSegment: MemberActivitySegment,
    val searchQuery: String,
    val selectedCompCategory: LeaderboardCategory,
    val isBusy: Boolean,
    val actionFeedback: String?,
    val exportedSyncPayload: String?,
    val syncPreview: ExternalSyncPreview?
)

class AdminViewModel(
    private val adminRepo: AdminFirestoreRepository,
    private val adminUid: String
) : ViewModel() {

    private val syncBridge = StandardGymSyncBridge()

    private val _isPasscodeVerified = MutableStateFlow(false)
    private val _passcodeError = MutableStateFlow<String?>(null)
    private val _selectedModule = MutableStateFlow(AdminDashboardModule.GYM_OVERVIEW)
    private val _showQrManagement = MutableStateFlow(false)
    private val _selectedPeriod = MutableStateFlow(AdminOverviewPeriod.SEVEN_DAYS)
    private val _selectedSegment = MutableStateFlow(MemberActivitySegment.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCompCategory = MutableStateFlow(LeaderboardCategory.ATTENDANCE_STREAK)
    private val _isBusy = MutableStateFlow(false)
    private val _actionFeedback = MutableStateFlow<String?>(null)
    private val _exportedSyncPayload = MutableStateFlow<String?>(null)
    private val _syncPreview = MutableStateFlow<ExternalSyncPreview?>(null)

    private val clubRecordsFlow = combine(
        adminRepo.observeAllUsers(),
        adminRepo.observeAllMemberships().catch { emit(emptyList()) },
        adminRepo.observeAllAttendance().catch { emit(emptyList()) },
        adminRepo.observeAllPaymentRequests().catch { emit(emptyList()) },
        adminRepo.observeAllPayments().catch { emit(emptyList()) }
    ) { users, memberships, attendance, requests, payments ->
        ClubMemberRecords(users, memberships, attendance, requests, payments)
    }

    private val engagementFlow = combine(
        adminRepo.observeAllAnnouncements().catch { emit(emptyList()) },
        adminRepo.observeCompetitionConfigs().catch { emit(emptyList()) },
        adminRepo.observePromotionalOffers().catch { emit(emptyList()) },
        adminRepo.observeBroadcastNotifications().catch { emit(emptyList()) },
        adminRepo.observeAllReferrals().catch { emit(emptyList()) }
    ) { announcements, compConfigs, offers, broadcasts, referrals ->
        ClubEngagementStreams(announcements, compConfigs, offers, broadcasts, referrals)
    }

    private val systemBaseFlow = combine(
        adminRepo.observeLeaderboardStats().catch { emit(emptyList()) },
        adminRepo.observeLeaderboardHistory().catch { emit(emptyList()) },
        adminRepo.observeQrSecurityEvents().catch { emit(emptyList()) },
        adminRepo.observeAuditLogs().catch { emit(emptyList()) }
    ) { lbStats, lbHist, qrEvents, logs ->
        Quadruple(lbStats, lbHist, qrEvents, logs)
    }

    private val systemFlow = combine(
        systemBaseFlow,
        adminRepo.observeGymConfig().catch { emit(null) },
        adminRepo.observeAppConfig().catch { emit(null) }
    ) { base, gymConfig, appConfig ->
        ClubSystemStreams(
            leaderboardStats = base.first,
            leaderboardHistory = base.second,
            qrEvents = base.third,
            auditLogs = base.fourth,
            gymConfig = gymConfig,
            appConfig = appConfig
        )
    }

    private val navControlsFlow = combine(
        _isPasscodeVerified,
        _passcodeError,
        _selectedModule,
        _showQrManagement,
        _selectedPeriod
    ) { verified, passErr, mod, showQr, period ->
        Quint(verified, passErr, mod, showQr, period)
    }

    private val filterControlsFlow = combine(
        _selectedSegment,
        _searchQuery,
        _selectedCompCategory,
        _isBusy,
        _actionFeedback
    ) { seg, query, compCat, busy, feedback ->
        Quint(seg, query, compCat, busy, feedback)
    }

    private val controlsFlow = combine(
        navControlsFlow,
        filterControlsFlow,
        _exportedSyncPayload,
        _syncPreview
    ) { nav, filt, exportPayload, syncPrev ->
        AdminControlsState(
            isPasscodeVerified = nav.first,
            passcodeError = nav.second,
            selectedModule = nav.third,
            showQrManagement = nav.fourth,
            selectedPeriod = nav.fifth,
            selectedSegment = filt.first,
            searchQuery = filt.second,
            selectedCompCategory = filt.third,
            isBusy = filt.fourth,
            actionFeedback = filt.fifth,
            exportedSyncPayload = exportPayload,
            syncPreview = syncPrev
        )
    }

    val uiState: StateFlow<AdminUiState> = combine(
        clubRecordsFlow,
        engagementFlow,
        systemFlow,
        controlsFlow
    ) { records, engagement, system, ctrl ->
        val summaries = AdminAnalyticsEngine.buildMemberSummaries(
            users = records.users,
            memberships = records.memberships,
            attendanceRecords = records.attendance,
            paymentRequests = records.paymentRequests,
            verifiedPayments = records.payments
        )
        val usersByUid = summaries.associateBy { it.userId }
        val query = ctrl.searchQuery.trim().lowercase(Locale.US)
        val filtered = summaries.filter { summary ->
            val matchesSegment = ctrl.selectedSegment == MemberActivitySegment.ALL ||
                ctrl.selectedSegment in summary.activitySegments
            val matchesSearch = query.isEmpty() ||
                summary.profile.fullName.lowercase(Locale.US).contains(query) ||
                summary.profile.memberCode.lowercase(Locale.US).contains(query) ||
                summary.profile.email.lowercase(Locale.US).contains(query)
            matchesSegment && matchesSearch
        }

        val overviewMetrics = AdminAnalyticsEngine.computeOverviewMetrics(
            summaries = summaries,
            attendanceRecords = records.attendance,
            paymentRequests = records.paymentRequests,
            referrals = engagement.referrals,
            qrSecurityEvents = system.qrEvents,
            period = ctrl.selectedPeriod
        )

        val recentAttItems = records.attendance.take(6).map { att ->
            val memberName = usersByUid[att.userId]?.profile?.fullName ?: "Club Member"
            AdminRecentActivityItem(
                id = "att_${att.userId}_${att.dateKey}",
                title = "$memberName checked in",
                subtitle = "${att.dateLabel} at ${att.checkInTime} • ${att.zoneLabel}",
                badgeLabel = "CHECK-IN",
                isAlert = false
            )
        }
        val recentPayItems = records.paymentRequests.take(5).map { req ->
            AdminRecentActivityItem(
                id = "req_${req.id}",
                title = "${req.memberName} (${req.amountFormatted})",
                subtitle = "${req.paymentMethod} • ${req.paymentDateLabel}",
                badgeLabel = req.status.replace("_", " "),
                isAlert = req.status == "PENDING_VERIFICATION"
            )
        }
        val recentActivity = (recentPayItems + recentAttItems).take(8)

        val configByCategory = buildDefaultCompetitionMap(engagement.competitionConfigs)
        val rankings = StreakAndLeaderboardEngine.rankEntries(
            stats = system.leaderboardStats,
            category = ctrl.selectedCompCategory,
            currentUserId = adminUid
        )

        AdminUiState(
            isLoading = false,
            isBusy = ctrl.isBusy,
            errorMessage = null,
            actionFeedback = ctrl.actionFeedback,
            isPasscodeVerified = ctrl.isPasscodeVerified,
            passcodeError = ctrl.passcodeError,
            selectedModule = ctrl.selectedModule,
            showQrManagement = ctrl.showQrManagement,
            selectedPeriod = ctrl.selectedPeriod,
            selectedSegment = ctrl.selectedSegment,
            searchQuery = ctrl.searchQuery,
            selectedCompCategory = ctrl.selectedCompCategory,
            overviewMetrics = overviewMetrics,
            recentActivity = recentActivity,
            allMembers = summaries,
            filteredMembers = filtered,
            allAttendance = records.attendance,
            pendingPaymentRequests = records.paymentRequests.filter {
                !it.isVerified && it.status == "PENDING_VERIFICATION"
            },
            competitionConfigs = configByCategory,
            leaderboardRankings = rankings,
            leaderboardStats = system.leaderboardStats,
            leaderboardHistory = system.leaderboardHistory,
            referrals = engagement.referrals,
            announcements = engagement.announcements,
            promotionalOffers = engagement.promotionalOffers,
            broadcastNotifications = engagement.broadcasts,
            qrSecurityEvents = system.qrEvents,
            auditLogs = system.auditLogs,
            gymConfig = system.gymConfig ?: AdminUiState().gymConfig,
            appConfig = system.appConfig ?: AdminUiState().appConfig,
            exportedSyncPayload = ctrl.exportedSyncPayload,
            syncPreview = ctrl.syncPreview
        )
    }.catch { error ->
        emit(
            AdminUiState(
                isLoading = false,
                isPasscodeVerified = _isPasscodeVerified.value,
                errorMessage = error.toFriendlyFirestoreMessage("Admin authorization or database sync failed.")
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AdminUiState(isLoading = true)
    )

    // --- Passcode & Navigation Controls ---

    fun unlockWithPasscode(passcode: String) {
        if (AdminAnalyticsEngine.verifyAdminPasscode(passcode)) {
            _isPasscodeVerified.value = true
            _passcodeError.value = null
        } else {
            _passcodeError.value = "Incorrect admin passcode. Verify credentials and retry."
        }
    }

    fun lockAdminGate() {
        _isPasscodeVerified.value = false
        _passcodeError.value = null
    }

    fun selectModule(module: AdminDashboardModule) {
        _selectedModule.value = module
        _showQrManagement.value = false
    }

    fun openQrManagement(open: Boolean) {
        _showQrManagement.value = open
    }

    fun selectOverviewPeriod(period: AdminOverviewPeriod) {
        _selectedPeriod.value = period
    }

    fun selectSegment(segment: MemberActivitySegment) {
        _selectedSegment.value = segment
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCompetitionCategory(category: LeaderboardCategory) {
        _selectedCompCategory.value = category
    }

    fun clearActionFeedback() {
        _actionFeedback.value = null
    }

    // --- Module 2: Membership & Payment Mutations ---

    fun confirmMemberPayment(
        member: AdminMemberSummary,
        pendingRequest: PaymentRequestDoc?,
        year: Int,
        monthOneBased: Int,
        dayOfMonth: Int,
        amountUsd: Double,
        paymentMethod: String,
        referenceNumber: String
    ) {
        runAdminAction(successMessage = "Payment confirmed and cycle activated for ${member.profile.fullName}.") {
            adminRepo.confirmPaymentAndRenewMembership(
                memberUserId = member.userId,
                memberName = member.profile.fullName,
                existingMembership = member.membership,
                pendingRequest = pendingRequest,
                paymentYear = year,
                paymentMonthOneBased = monthOneBased,
                paymentDayOfMonth = dayOfMonth,
                amountUsd = amountUsd,
                paymentMethod = paymentMethod,
                referenceNumber = referenceNumber
            )
        }
    }

    fun rejectPaymentRequest(member: AdminMemberSummary, request: PaymentRequestDoc, reason: String) {
        runAdminAction(successMessage = "Rejected payment request for ${member.profile.fullName}.") {
            adminRepo.rejectPaymentRequest(
                memberUserId = member.userId,
                memberName = member.profile.fullName,
                request = request,
                reason = reason.ifBlank { "Receipt could not be matched at front desk." }
            )
        }
    }

    fun updateMembershipStatus(member: AdminMemberSummary, newStatus: String) {
        runAdminAction(successMessage = "${member.profile.fullName} membership marked $newStatus.") {
            adminRepo.updateMembershipStatus(
                memberUserId = member.userId,
                memberName = member.profile.fullName,
                existingMembership = member.membership,
                newStatus = newStatus
            )
        }
    }

    // --- Module 3: Competition Control & Referrals ---

    fun saveCompetitionConfig(config: CompetitionConfigDoc) {
        runAdminAction(successMessage = "Saved ${config.title} competition settings.") {
            adminRepo.saveCompetitionConfig(config)
        }
    }

    fun publishMonthlyWinner(
        monthKey: String,
        categoryTitle: String,
        winnerUserId: String,
        winnerName: String,
        scoreLabel: String
    ) {
        runAdminAction(successMessage = "Published $winnerName as $monthKey winner.") {
            adminRepo.publishMonthlyWinner(monthKey, categoryTitle, winnerUserId, winnerName, scoreLabel)
        }
    }

    fun adjustMemberScore(
        userId: String,
        memberName: String,
        streakDays: Long,
        monthlyVisits: Long,
        habitsScore: Long,
        referralsCount: Long,
        reason: String
    ) {
        val currentStat = uiState.value.leaderboardStats.firstOrNull { it.userId == userId }
            ?: LeaderboardStatDoc(
                userId = userId,
                memberName = memberName,
                monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
            )
        val updated = currentStat.copy(
            currentStreakDays = streakDays.coerceAtLeast(0L),
            longestStreakDays = maxOf(currentStat.longestStreakDays, streakDays.coerceAtLeast(0L)),
            monthlyVisits = monthlyVisits.coerceAtLeast(0L),
            healthyHabitsScore = habitsScore.coerceIn(0L, 100L),
            verifiedReferralsCount = referralsCount.coerceAtLeast(0L)
        )
        runAdminAction(successMessage = "Adjusted leaderboard score for $memberName (audit logged).") {
            adminRepo.adjustLeaderboardStats(updated, reason.ifBlank { "Admin adjustment" })
        }
    }

    fun resolveReferral(referral: ReferralRecordDoc, approved: Boolean) {
        val state = uiState.value
        val users = state.allMembers.map { it.profile }
        runAdminAction(
            successMessage = if (approved) "Verified referral for ${referral.referredName}."
            else "Rejected referral for ${referral.referredName}."
        ) {
            adminRepo.resolveReferral(
                referral = referral,
                approved = approved,
                allUsers = users,
                allStats = state.leaderboardStats
            )
        }
    }

    // --- Module 4: Announcements, Broadcasts & Promotional Offers ---

    fun saveAnnouncementWithOptionalPhoto(
        context: Context,
        existingId: String,
        title: String,
        description: String,
        dateLabel: String,
        category: String,
        isImportant: Boolean,
        isPublished: Boolean,
        scheduledAtLabel: String?,
        presetImageKey: String?,
        pickedImageUri: String?,
        existingImageData: String?,
        ctaLabel: String?
    ) {
        if (title.trim().length < 2 || description.trim().length < 2) {
            _actionFeedback.value = "Title and description must each be at least 2 characters."
            return
        }
        _isBusy.value = true
        viewModelScope.launch {
            try {
                val resolvedImageData = when {
                    !pickedImageUri.isNullOrBlank() ->
                        ImageCompressionUtils.compressUriToDataUri(context, pickedImageUri)
                    else -> existingImageData
                }
                val doc = AnnouncementDoc(
                    id = existingId,
                    title = title.trim(),
                    description = description.trim(),
                    dateLabel = dateLabel.trim().ifBlank {
                        SimpleDateFormat("MMM d, yyyy", Locale.US).format(Calendar.getInstance().time)
                    },
                    category = category,
                    isImportant = isImportant,
                    isPublished = isPublished,
                    scheduledAtLabel = scheduledAtLabel?.trim()?.takeIf { it.isNotBlank() },
                    imageKey = if (resolvedImageData != null) null else presetImageKey?.takeIf { it.isNotBlank() },
                    imageData = resolvedImageData,
                    ctaLabel = ctaLabel?.trim()?.takeIf { it.isNotBlank() }
                )
                adminRepo.saveAnnouncement(doc)
                _actionFeedback.value = if (isPublished) "Announcement published to members." else "Announcement saved as draft/scheduled."
            } catch (e: Exception) {
                _actionFeedback.value = e.toFriendlyFirestoreMessage("Failed to save announcement.")
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun deleteAnnouncement(announcement: AnnouncementDoc) {
        runAdminAction(successMessage = "Deleted announcement '${announcement.title}'.") {
            adminRepo.deleteAnnouncement(announcement)
        }
    }

    fun sendBroadcastNotification(
        title: String,
        message: String,
        type: String,
        audience: String,
        isUrgent: Boolean
    ) {
        if (title.trim().length < 2 || message.trim().length < 2) {
            _actionFeedback.value = "Enter a broadcast title and message."
            return
        }
        runAdminAction(successMessage = "Broadcast notification dispatched to $audience.") {
            adminRepo.sendBroadcastNotification(title, message, type, audience, isUrgent)
        }
    }

    fun savePromotionalOffer(offer: PromotionalOfferDoc) {
        if (offer.title.trim().length < 2 || offer.subtitle.trim().length < 2) {
            _actionFeedback.value = "Offer title and description are required."
            return
        }
        runAdminAction(successMessage = "Saved promotional offer '${offer.title}'.") {
            adminRepo.savePromotionalOffer(offer)
        }
    }

    fun deletePromotionalOffer(offer: PromotionalOfferDoc) {
        runAdminAction(successMessage = "Removed offer '${offer.title}'.") {
            adminRepo.deletePromotionalOffer(offer)
        }
    }

    // --- Common Entrance QR Config, App Version & External Sync ---

    fun updateGymQrConfig(
        isQrEnabled: Boolean,
        allowStaticFallback: Boolean,
        rotationWindowSeconds: Long,
        allowedRadiusMeters: Double?
    ) {
        val current = uiState.value.gymConfig
        val updated = current.copy(
            id = "main_entrance",
            gymId = "main_entrance",
            gymName = AppConfig.GYM_NAME,
            staticEntranceCode = current.staticEntranceCode.ifBlank { AppConfig.COMMON_ENTRANCE_QR_CODE },
            rotationWindowSeconds = rotationWindowSeconds.coerceIn(30L, 600L),
            isQrEnabled = isQrEnabled,
            allowStaticFallback = allowStaticFallback,
            allowedRadiusMeters = allowedRadiusMeters,
            gymLatitude = if (allowedRadiusMeters != null) current.gymLatitude ?: AppConfig.DEFAULT_GYM_LATITUDE else current.gymLatitude,
            gymLongitude = if (allowedRadiusMeters != null) current.gymLongitude ?: AppConfig.DEFAULT_GYM_LONGITUDE else current.gymLongitude,
            qrVersion = current.qrVersion + 1L
        )
        runAdminAction(successMessage = "Updated Common Entrance QR configuration (v${updated.qrVersion}).") {
            adminRepo.updateGymQrConfig(
                config = updated,
                actionLabel = "QR Config updated: enabled=$isQrEnabled, window=${updated.rotationWindowSeconds}s"
            )
        }
    }

    fun markQrEventReviewed(event: QrSecurityEventDoc) {
        runAdminAction(successMessage = "Marked QR security event reviewed.") {
            adminRepo.markQrEventReviewed(event)
        }
    }

    fun updateAppVersionConfig(
        latestVersion: String,
        releaseDate: String,
        highlightsRaw: String,
        githubRepo: String = "",
        downloadUrl: String = ""
    ) {
        val highlights = highlightsRaw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (latestVersion.trim().isBlank()) {
            _actionFeedback.value = "Version string cannot be empty."
            return
        }
        runAdminAction(successMessage = "Published remote app release v${latestVersion.trim()}.") {
            adminRepo.updateAppVersionConfig(
                latestVersion = latestVersion.trim(),
                releaseDate = releaseDate.trim(),
                releaseHighlights = highlights,
                githubRepo = githubRepo,
                downloadUrl = downloadUrl
            )
        }
    }

    fun exportSyncBundleJson() {
        val s = uiState.value
        val payments = s.allMembers.flatMap { it.verifiedPayments }
        _exportedSyncPayload.value = syncBridge.buildJsonSyncBundle(s.allMembers, s.allAttendance, payments)
        _actionFeedback.value = "Generated deduplicated JSON sync bundle (${s.allMembers.size} members)."
    }

    fun exportMembersCsv() {
        val s = uiState.value
        _exportedSyncPayload.value = syncBridge.buildMemberRosterCsv(s.allMembers)
        _actionFeedback.value = "Generated Member Roster CSV (${s.allMembers.size} rows)."
    }

    fun exportAttendanceCsv() {
        val s = uiState.value
        val map = s.allMembers.associateBy { it.userId }
        _exportedSyncPayload.value = syncBridge.buildAttendanceCsv(s.allAttendance, map)
        _actionFeedback.value = "Generated Attendance Logs CSV."
    }

    fun previewInboundSyncPayload(payload: String) {
        _syncPreview.value = syncBridge.previewInboundPayload(payload)
    }

    fun clearSyncDialogState() {
        _exportedSyncPayload.value = null
        _syncPreview.value = null
    }

    private fun runAdminAction(successMessage: String, block: suspend () -> Unit) {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                block()
                _actionFeedback.value = successMessage
            } catch (e: Exception) {
                _actionFeedback.value = e.toFriendlyFirestoreMessage("Admin operation failed.")
            } finally {
                _isBusy.value = false
            }
        }
    }

    private fun buildDefaultCompetitionMap(
        docs: List<CompetitionConfigDoc>
    ): Map<LeaderboardCategory, CompetitionConfigDoc> {
        val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.US).format(Calendar.getInstance().time)
        val byCat = docs.associateBy { it.categoryId.uppercase(Locale.US) }
        return LeaderboardCategory.entries.associateWith { cat ->
            byCat[cat.name] ?: when (cat) {
                LeaderboardCategory.ATTENDANCE_STREAK -> CompetitionConfigDoc(
                    id = cat.name.lowercase(Locale.US),
                    categoryId = cat.name,
                    title = cat.title,
                    periodLabel = monthLabel,
                    targetDescription = "Longest consecutive daily entrance check-in streak",
                    targetValue = 21L,
                    rewardTitle = "1 Month Free Pro + Gymimpulse Hoodie",
                    isActive = true
                )
                LeaderboardCategory.HEALTHY_HABITS_RACE -> CompetitionConfigDoc(
                    id = cat.name.lowercase(Locale.US),
                    categoryId = cat.name,
                    title = cat.title,
                    periodLabel = monthLabel,
                    targetDescription = "Hit 85%+ daily protein and whole-food targets",
                    targetValue = 85L,
                    rewardTitle = "Custom Supplement & Shaker Kit",
                    isActive = true
                )
                LeaderboardCategory.CONSISTENCY_CHAMPION -> CompetitionConfigDoc(
                    id = cat.name.lowercase(Locale.US),
                    categoryId = cat.name,
                    title = cat.title,
                    periodLabel = monthLabel,
                    targetDescription = "Complete 3+ verified visits every week of the month",
                    targetValue = 4L,
                    rewardTitle = "4 Personal Training Recovery Sessions",
                    isActive = true
                )
                LeaderboardCategory.REFERRAL_CHAMPION -> CompetitionConfigDoc(
                    id = cat.name.lowercase(Locale.US),
                    categoryId = cat.name,
                    title = cat.title,
                    periodLabel = monthLabel,
                    targetDescription = "Refer verified joining members with your personal code",
                    targetValue = 3L,
                    rewardTitle = "$50 Club Credit per Verified Referral",
                    isActive = true
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
private data class Quint<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
