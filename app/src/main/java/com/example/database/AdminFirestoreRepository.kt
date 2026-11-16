package com.example.database

import android.content.Context
import com.example.R
import com.example.config.AppConfig
import com.example.utils.DateTimeUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/**
 * Cloud Firestore repository for Gymimpulse Phase 3 Admin Console.
 * All reads and writes enforce authenticated admin authorization via firestore.rules
 * and append immutable audit entries to `/admin_audit_logs`.
 */
class AdminFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private val shortDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

    private fun requireAdminIdentity(): Pair<String, String> {
        val user = auth.currentUser
            ?: throw IllegalStateException("Admin must be signed in with Google.")
        return user.uid to (user.email ?: "admin@gymimpulse.fit")
    }

    private fun <T> Flow<T>.handleAdminErrors(
        operationType: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val fsError = error.findFirestoreException()
        if (fsError == null && error is CancellationException) throw error
        val exceptionToLog = fsError ?: (error as? Exception)
        if (exceptionToLog != null) {
            handleFirestoreError(exceptionToLog, operationType, path) { auth }
        }
        throw error
    }

    /**
     * Checks live backend admin authorization: either verified admin registry doc `/admins/{uid}`
     * exists OR verified admin email token claim matches backend rules.
     */
    fun observeIsAdminAuthorized(uid: String): Flow<Boolean> = flow {
        val user = auth.currentUser
        if (user == null || user.uid != uid) {
            emit(false)
            return@flow
        }
        if (user.email.equals("hereyourmax@gmail.com", ignoreCase = true) && user.isEmailVerified) {
            emit(true)
            return@flow
        }
        emitAll(
            db.collection("admins").document(uid)
                .snapshots()
                .map { it.exists() }
                .catch { emit(false) }
        )
    }

    // --- Club-Wide Admin Observation Streams ---

    fun observeAllUsers(): Flow<List<UserProfileDoc>> = flow {
        requireAdminIdentity()
        val path = "users"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap -> snap.toObjects(UserProfileDoc::class.java, ServerTimestampBehavior.ESTIMATE) }
                .handleAdminErrors(OperationType.LIST, path)
        )
    }

    fun observeAllMemberships(): Flow<List<MembershipRecordDoc>> = flow {
        requireAdminIdentity()
        emitAll(
            db.collectionGroup("memberships")
                .snapshots()
                .map { snap -> snap.toObjects(MembershipRecordDoc::class.java, ServerTimestampBehavior.ESTIMATE) }
                .handleAdminErrors(OperationType.LIST, "collectionGroup/memberships")
        )
    }

    fun observeAllPaymentRequests(): Flow<List<PaymentRequestDoc>> = flow {
        requireAdminIdentity()
        emitAll(
            db.collectionGroup("payment_requests")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PaymentRequestDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleAdminErrors(OperationType.LIST, "collectionGroup/payment_requests")
        )
    }

    fun observeAllPayments(): Flow<List<PaymentRecordDoc>> = flow {
        requireAdminIdentity()
        emitAll(
            db.collectionGroup("payments")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PaymentRecordDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleAdminErrors(OperationType.LIST, "collectionGroup/payments")
        )
    }

    fun observeAllAttendance(): Flow<List<AttendanceRecordDoc>> = flow {
        requireAdminIdentity()
        emitAll(
            db.collectionGroup("attendance")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AttendanceRecordDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.dateKey }
                }
                .handleAdminErrors(OperationType.LIST, "collectionGroup/attendance")
        )
    }

    fun observeAllAnnouncements(): Flow<List<AnnouncementDoc>> = flow {
        requireAdminIdentity()
        val path = "announcements"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AnnouncementDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleAdminErrors(OperationType.LIST, path)
        )
    }

    fun observeCompetitionConfigs(): Flow<List<CompetitionConfigDoc>> = flow {
        requireAdminIdentity()
        val path = "competition_configs"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap -> snap.toObjects(CompetitionConfigDoc::class.java, ServerTimestampBehavior.ESTIMATE) }
                .handleAdminErrors(OperationType.LIST, path)
        )
    }

    fun observePromotionalOffers(): Flow<List<PromotionalOfferDoc>> = flow {
        requireAdminIdentity()
        val path = "promotional_offers"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PromotionalOfferDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleAdminErrors(OperationType.LIST, path)
        )
    }

    fun observeBroadcastNotifications(): Flow<List<BroadcastNotificationDoc>> = flow {
        requireAdminIdentity()
        val path = "notifications_broadcast"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(BroadcastNotificationDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleAdminErrors(OperationType.LIST, path)
        )
    }

    fun observeAllReferrals(): Flow<List<ReferralRecordDoc>> = flow {
        requireAdminIdentity()
        val path = "referrals"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ReferralRecordDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleAdminErrors(OperationType.LIST, path)
        )
    }

    fun observeQrSecurityEvents(): Flow<List<QrSecurityEventDoc>> = flow {
        requireAdminIdentity()
        val path = "qr_security_events"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(QrSecurityEventDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleAdminErrors(OperationType.LIST, path)
        )
    }

    fun observeAuditLogs(): Flow<List<AdminAuditLogDoc>> = flow {
        requireAdminIdentity()
        val path = "admin_audit_logs"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AdminAuditLogDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleAdminErrors(OperationType.LIST, path)
        )
    }

    fun observeLeaderboardStats(): Flow<List<LeaderboardStatDoc>> = flow {
        requireAdminIdentity()
        val path = "leaderboard_stats"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap -> snap.toObjects(LeaderboardStatDoc::class.java, ServerTimestampBehavior.ESTIMATE) }
                .handleAdminErrors(OperationType.LIST, path)
        )
    }

    fun observeLeaderboardHistory(): Flow<List<LeaderboardHistoryDoc>> = flow {
        requireAdminIdentity()
        val path = "leaderboard_history"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LeaderboardHistoryDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.monthKey }
                }
                .handleAdminErrors(OperationType.LIST, path)
        )
    }

    fun observeGymConfig(): Flow<GymConfigDoc?> = flow {
        requireAdminIdentity()
        val path = "gym_config/main_entrance"
        emitAll(
            db.collection("gym_config").document("main_entrance")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(GymConfigDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .handleAdminErrors(OperationType.GET, path)
        )
    }

    fun observeAppConfig(): Flow<AppConfigDoc?> = flow {
        requireAdminIdentity()
        val path = "app_config/current"
        emitAll(
            db.collection("app_config").document("current")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(AppConfigDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .handleAdminErrors(OperationType.GET, path)
        )
    }

    // --- Admin Audited Mutations ---

    /**
     * Confirms a payment (either verifying an existing payment_request OR recording a direct desk payment)
     * anchored to the exact payment date selected by the admin (e.g., Dec 4 -> Jan 4),
     * writes the official receipt in `users/{uid}/payments`, activates `users/{uid}/memberships/current`,
     * and logs an immutable entry in `admin_audit_logs`.
     */
    suspend fun confirmPaymentAndRenewMembership(
        memberUserId: String,
        memberName: String,
        existingMembership: MembershipRecordDoc?,
        pendingRequest: PaymentRequestDoc?,
        paymentYear: Int,
        paymentMonthOneBased: Int,
        paymentDayOfMonth: Int,
        amountUsd: Double,
        paymentMethod: String,
        referenceNumber: String,
        planName: String = existingMembership?.planName?.ifBlank { AppConfig.DEFAULT_PLAN_NAME }
            ?: AppConfig.DEFAULT_PLAN_NAME
    ) {
        val (adminUid, adminEmail) = requireAdminIdentity()
        val (cycleStart, cycleEnd) = DateTimeUtils.calculateCycleFromPaymentDate(
            year = paymentYear,
            monthOneBased = paymentMonthOneBased,
            dayOfMonth = paymentDayOfMonth
        )
        val cycleWindow = DateTimeUtils.formatCycleWindow(
            startYear = paymentYear,
            startMonthOneBased = paymentMonthOneBased,
            startDay = paymentDayOfMonth
        )
        val paymentId = "pay_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val formattedAmount = "$" + String.format(Locale.US, "%.2f", amountUsd)
        val cleanRef = referenceNumber.trim().ifBlank { "ADM-${paymentId.takeLast(6).uppercase()}" }

        val userRef = db.collection("users").document(memberUserId)
        val membershipRef = userRef.collection("memberships").document("current")
        val paymentRef = userRef.collection("payments").document(paymentId)

        val newMembership = MembershipRecordDoc(
            id = "current",
            userId = memberUserId,
            planName = planName,
            monthlyFeeUsd = amountUsd,
            membershipStartDate = existingMembership?.membershipStartDate?.ifBlank { cycleStart } ?: cycleStart,
            cycleStartDate = cycleStart,
            cycleEndDate = cycleEnd,
            nextRenewalDate = cycleEnd,
            status = "ACTIVE",
            autoRenewEnabled = existingMembership?.autoRenewEnabled ?: false
        )
        val paymentRecord = PaymentRecordDoc(
            id = paymentId,
            userId = memberUserId,
            paymentDate = cycleStart,
            cycleCovered = cycleWindow,
            amountFormatted = formattedAmount,
            method = paymentMethod,
            referenceNumber = cleanRef,
            isVerified = true
        )

        val batch = db.batch()
        batch.set(membershipRef, newMembership.toCreateMap())
        batch.set(paymentRef, paymentRecord.toCreateMap())
        if (pendingRequest != null && pendingRequest.id.isNotBlank()) {
            val reqRef = userRef.collection("payment_requests").document(pendingRequest.id)
            batch.set(reqRef, pendingRequest.toAdminResolutionMap(approved = true))
        }
        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, membershipRef.path) { auth }
            throw e
        }

        appendAuditLog(
            adminUid = adminUid,
            adminEmail = adminEmail,
            actionType = "CONFIRM_PAYMENT",
            targetId = memberUserId,
            summary = "Confirmed payment for $memberName ($cycleStart – $cycleEnd)",
            details = "Amount: $formattedAmount via $paymentMethod (Ref: $cleanRef)"
        )
    }

    suspend fun rejectPaymentRequest(
        memberUserId: String,
        memberName: String,
        request: PaymentRequestDoc,
        reason: String
    ) {
        val (adminUid, adminEmail) = requireAdminIdentity()
        val reqRef = db.collection("users").document(memberUserId)
            .collection("payment_requests").document(request.id)
        try {
            reqRef.set(request.toAdminResolutionMap(approved = false)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path) { auth }
            throw e
        }
        appendAuditLog(
            adminUid = adminUid,
            adminEmail = adminEmail,
            actionType = "REJECT_PAYMENT",
            targetId = memberUserId,
            summary = "Rejected payment request ${request.id} for $memberName",
            details = reason.take(300)
        )
    }

    suspend fun updateMembershipStatus(
        memberUserId: String,
        memberName: String,
        existingMembership: MembershipRecordDoc?,
        newStatus: String
    ) {
        val (adminUid, adminEmail) = requireAdminIdentity()
        val today = shortDateFormat.format(Calendar.getInstance().time)
        val membershipRef = db.collection("users").document(memberUserId)
            .collection("memberships").document("current")
        val updated = (existingMembership ?: MembershipRecordDoc(
            userId = memberUserId,
            planName = AppConfig.DEFAULT_PLAN_NAME,
            monthlyFeeUsd = AppConfig.DEFAULT_MONTHLY_FEE_USD,
            membershipStartDate = today,
            cycleStartDate = today,
            cycleEndDate = today,
            nextRenewalDate = today
        )).copy(userId = memberUserId, status = newStatus)

        try {
            membershipRef.set(updated.toCreateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, membershipRef.path) { auth }
            throw e
        }
        appendAuditLog(
            adminUid = adminUid,
            adminEmail = adminEmail,
            actionType = "UPDATE_MEMBERSHIP_STATUS",
            targetId = memberUserId,
            summary = "Changed $memberName membership status to $newStatus",
            details = null
        )
    }

    // --- Module 3: Competition Control & Referrals ---

    suspend fun saveCompetitionConfig(config: CompetitionConfigDoc) {
        val (adminUid, adminEmail) = requireAdminIdentity()
        val docId = config.id.ifBlank { config.categoryId.lowercase(Locale.US) }
        val docRef = db.collection("competition_configs").document(docId)
        try {
            docRef.set(config.toWriteMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path) { auth }
            throw e
        }
        appendAuditLog(
            adminUid = adminUid,
            adminEmail = adminEmail,
            actionType = "UPDATE_COMPETITION",
            targetId = docId,
            summary = "Updated competition config: ${config.title} (${config.periodLabel})",
            details = "Target=${config.targetValue}, Reward=${config.rewardTitle}"
        )
    }

    suspend fun publishMonthlyWinner(
        monthKey: String,
        categoryTitle: String,
        winnerUserId: String,
        winnerName: String,
        scoreLabel: String
    ) {
        val (adminUid, adminEmail) = requireAdminIdentity()
        val historyId = "win_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val histRef = db.collection("leaderboard_history").document(historyId)
        val lbRef = db.collection("leaderboard_stats").document(winnerUserId)

        try {
            histRef.set(
                LeaderboardHistoryDoc(
                    id = historyId,
                    monthKey = monthKey,
                    category = categoryTitle,
                    winnerName = winnerName,
                    scoreLabel = scoreLabel
                ).toWriteMap()
            ).await()
            val lbSnap = runCatching { lbRef.get().await() }.getOrNull()
            if (lbSnap != null && lbSnap.exists()) {
                val cur = lbSnap.toObject(LeaderboardStatDoc::class.java)
                if (cur != null) {
                    lbRef.set(cur.copy(isMonthlyWinner = true).toAdminWriteMap()).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, histRef.path) { auth }
            throw e
        }

        appendAuditLog(
            adminUid = adminUid,
            adminEmail = adminEmail,
            actionType = "PUBLISH_WINNER",
            targetId = winnerUserId,
            summary = "Published $monthKey winner ($categoryTitle): $winnerName",
            details = "Score: $scoreLabel"
        )
    }

    suspend fun adjustLeaderboardStats(
        stat: LeaderboardStatDoc,
        reason: String
    ) {
        val (adminUid, adminEmail) = requireAdminIdentity()
        val docRef = db.collection("leaderboard_stats").document(stat.userId)
        try {
            docRef.set(stat.toAdminWriteMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path) { auth }
            throw e
        }
        appendAuditLog(
            adminUid = adminUid,
            adminEmail = adminEmail,
            actionType = "MANUAL_SCORE_OVERRIDE",
            targetId = stat.userId,
            summary = "Adjusted competition score for ${stat.memberName}",
            details = "Streak=${stat.currentStreakDays}, Habits=${stat.healthyHabitsScore}, Reason=$reason"
        )
    }

    suspend fun resolveReferral(
        referral: ReferralRecordDoc,
        approved: Boolean,
        allUsers: List<UserProfileDoc>,
        allStats: List<LeaderboardStatDoc>
    ) {
        val (adminUid, adminEmail) = requireAdminIdentity()
        val referrer = allUsers.firstOrNull {
            it.referralCode.equals(referral.referrerCode, ignoreCase = true) ||
                it.userId == referral.referrerUserId
        }
        val refUid = referrer?.userId?.ifBlank { referrer.id } ?: referral.referrerUserId
        val docRef = db.collection("referrals").document(referral.id)

        try {
            docRef.set(referral.toAdminUpdateMap(approved = approved, resolvedReferrerUid = refUid)).await()
            if (approved && !refUid.isNullOrBlank()) {
                val lbRef = db.collection("leaderboard_stats").document(refUid)
                val existing = allStats.firstOrNull { it.userId == refUid }
                val updatedStat = (existing ?: LeaderboardStatDoc(
                    userId = refUid,
                    memberName = referrer?.fullName ?: "Club Member",
                    monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
                )).let {
                    it.copy(verifiedReferralsCount = it.verifiedReferralsCount + 1)
                }
                lbRef.set(updatedStat.toAdminWriteMap()).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path) { auth }
            throw e
        }

        appendAuditLog(
            adminUid = adminUid,
            adminEmail = adminEmail,
            actionType = if (approved) "VERIFY_REFERRAL" else "REJECT_REFERRAL",
            targetId = referral.id,
            summary = "${if (approved) "Verified" else "Rejected"} referral for ${referral.referredName}",
            details = "Code=${referral.referrerCode}, ReferrerUid=$refUid"
        )
    }

    // --- Module 4: Member Engagement (Announcements, Broadcast Notifications, Offers) ---

    suspend fun saveAnnouncement(announcement: AnnouncementDoc) {
        val (adminUid, adminEmail) = requireAdminIdentity()
        val docId = announcement.id.ifBlank {
            "ann_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        }
        val docRef = db.collection("announcements").document(docId)
        try {
            docRef.set(announcement.toWriteMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path) { auth }
            throw e
        }
        appendAuditLog(
            adminUid = adminUid,
            adminEmail = adminEmail,
            actionType = if (announcement.id.isBlank()) "CREATE_ANNOUNCEMENT" else "EDIT_ANNOUNCEMENT",
            targetId = docId,
            summary = "${if (announcement.isPublished) "Published" else "Saved draft"} announcement: ${announcement.title}",
            details = "Category=${announcement.category}, Important=${announcement.isImportant}"
        )
    }

    suspend fun deleteAnnouncement(announcement: AnnouncementDoc) {
        val (adminUid, adminEmail) = requireAdminIdentity()
        val docRef = db.collection("announcements").document(announcement.id)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path) { auth }
            throw e
        }
        appendAuditLog(
            adminUid = adminUid,
            adminEmail = adminEmail,
            actionType = "DELETE_ANNOUNCEMENT",
            targetId = announcement.id,
            summary = "Deleted announcement: ${announcement.title}",
            details = null
        )
    }

    suspend fun savePromotionalOffer(offer: PromotionalOfferDoc) {
        val (adminUid, adminEmail) = requireAdminIdentity()
        val docId = offer.id.ifBlank {
            "offer_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        }
        val docRef = db.collection("promotional_offers").document(docId)
        try {
            docRef.set(offer.toWriteMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path) { auth }
            throw e
        }
        appendAuditLog(
            adminUid = adminUid,
            adminEmail = adminEmail,
            actionType = "SAVE_PROMOTIONAL_OFFER",
            targetId = docId,
            summary = "Saved offer: ${offer.title} (${if (offer.isActive) "Active" else "Paused"})",
            details = "Audience=${offer.targetAudience}, Code=${offer.discountCode ?: "None"}"
        )
    }

    suspend fun deletePromotionalOffer(offer: PromotionalOfferDoc) {
        val (adminUid, adminEmail) = requireAdminIdentity()
        val docRef = db.collection("promotional_offers").document(offer.id)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path) { auth }
            throw e
        }
        appendAuditLog(
            adminUid = adminUid,
            adminEmail = adminEmail,
            actionType = "DELETE_OFFER",
            targetId = offer.id,
            summary = "Removed promotional offer: ${offer.title}",
            details = null
        )
    }

    suspend fun sendBroadcastNotification(
        title: String,
        message: String,
        type: String,
        audience: String,
        isUrgent: Boolean
    ) {
        val (adminUid, adminEmail) = requireAdminIdentity()
        val docId = "notif_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val docRef = db.collection("notifications_broadcast").document(docId)
        val payload = BroadcastNotificationDoc(
            id = docId,
            title = title.trim(),
            message = message.trim(),
            type = type,
            audience = audience,
            isUrgent = isUrgent,
            createdByAdminUid = adminUid
        ).toWriteMap()
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path) { auth }
            throw e
        }
        appendAuditLog(
            adminUid = adminUid,
            adminEmail = adminEmail,
            actionType = "SEND_BROADCAST",
            targetId = docId,
            summary = "Broadcast '$title' to $audience",
            details = "Type=$type, Urgent=$isUrgent"
        )
    }

    // --- Common QR Management & Remote App Config ---

    suspend fun updateGymQrConfig(config: GymConfigDoc, actionLabel: String) {
        val (adminUid, adminEmail) = requireAdminIdentity()
        val docRef = db.collection("gym_config").document("main_entrance")
        try {
            docRef.set(config.toWriteMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path) { auth }
            throw e
        }
        appendAuditLog(
            adminUid = adminUid,
            adminEmail = adminEmail,
            actionType = "UPDATE_QR_CONFIG",
            targetId = "main_entrance",
            summary = actionLabel,
            details = "Enabled=${config.isQrEnabled}, Window=${config.rotationWindowSeconds}s, Version=${config.qrVersion}"
        )
    }

    suspend fun markQrEventReviewed(event: QrSecurityEventDoc) {
        requireAdminIdentity()
        val docRef = db.collection("qr_security_events").document(event.id)
        try {
            docRef.set(event.toMarkReviewedMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path) { auth }
            throw e
        }
    }

    suspend fun updateAppVersionConfig(
        latestVersion: String,
        releaseDate: String,
        releaseHighlights: List<String>,
        githubRepo: String? = null,
        downloadUrl: String? = null
    ) {
        val (adminUid, adminEmail) = requireAdminIdentity()
        val docRef = db.collection("app_config").document("current")
        val doc = AppConfigDoc(
            id = "current",
            latestVersion = latestVersion.trim(),
            releaseDate = releaseDate.trim(),
            releaseHighlights = releaseHighlights.filter { it.isNotBlank() },
            githubRepo = githubRepo?.trim()?.takeIf { it.isNotBlank() },
            downloadUrl = downloadUrl?.trim()?.takeIf { it.isNotBlank() }
        )
        try {
            docRef.set(doc.toWriteMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path) { auth }
            throw e
        }
        appendAuditLog(
            adminUid = adminUid,
            adminEmail = adminEmail,
            actionType = "UPDATE_APP_CONFIG",
            targetId = "current",
            summary = "Updated remote app version metadata to v$latestVersion",
            details = null
        )
    }

    private suspend fun appendAuditLog(
        adminUid: String,
        adminEmail: String,
        actionType: String,
        targetId: String,
        summary: String,
        details: String?
    ) {
        val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val logRef = db.collection("admin_audit_logs").document(logId)
        val payload = AdminAuditLogDoc(
            id = logId,
            adminUserId = adminUid,
            adminEmail = adminEmail,
            actionType = actionType,
            targetId = targetId.ifBlank { "global" },
            summary = summary.take(200),
            details = details?.take(600)
        ).toCreateMap()
        runCatching { logRef.set(payload).await() }
    }
}
