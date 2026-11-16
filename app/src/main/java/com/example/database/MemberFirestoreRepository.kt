package com.example.database

import android.content.Context
import com.example.R
import com.example.config.AppConfig
import com.example.utils.NutritionAnalyticsEngine
import com.example.utils.StreakAndLeaderboardEngine
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
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
 * Cloud Firestore repository for GymPulse member features.
 * Always receives a FirebaseFirestore instance configured with R.string.firestore_database_id.
 */
class MemberFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val shortDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
    private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.handleSnapshotErrors(
        operationType: OperationType,
        path: String,
        scopedUid: String?
    ): Flow<T> = catch { error ->
        val fsError = error.findFirestoreException()
        if (fsError == null && error is CancellationException) throw error
        val exceptionToLog = fsError ?: (error as? Exception)
        if (exceptionToLog != null && (scopedUid == null || auth.currentUser?.uid == scopedUid)) {
            handleFirestoreError(exceptionToLog, operationType, path) { auth }
        }
        throw error
    }

    // --- 1. Member Profile ---

    fun observeUserProfile(userId: String): Flow<UserProfileDoc?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfileDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .handleSnapshotErrors(OperationType.GET, path, userId)
    }

    suspend fun getUserProfile(userId: String): UserProfileDoc? {
        val docRef = db.collection("users").document(userId)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(UserProfileDoc::class.java, ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path) { auth }
            throw e
        }
    }

    suspend fun registerMemberProfile(
        fullName: String,
        phone: String?,
        email: String,
        avatarData: String?,
        fitnessGoal: String?,
        referredByCode: String?
    ): UserProfileDoc {
        val uid = requireUserId()
        val now = Calendar.getInstance()
        val joinedDate = shortDateFormat.format(now.time)
        val numericSuffix = (1000 + kotlin.math.abs(uid.hashCode()) % 9000)
        val memberCode = "GP-$numericSuffix"
        val referralCode = "GP-REF-$numericSuffix"
        val cleanGoal = fitnessGoal?.trim()?.takeIf { it.isNotBlank() }
            ?: "Strength & ${AppConfig.DEFAULT_PROTEIN_GOAL_GRAMS}g Daily Protein"

        val profile = UserProfileDoc(
            id = uid,
            userId = uid,
            memberCode = memberCode,
            fullName = fullName.trim().ifBlank { "Club Member" },
            phone = phone?.trim()?.takeIf { it.isNotBlank() },
            email = email.trim(),
            avatarData = avatarData,
            role = "MEMBER",
            joinedDate = joinedDate,
            fitnessGoal = cleanGoal,
            referralCode = referralCode,
            referredByCode = referredByCode?.trim()?.takeIf { it.isNotBlank() }
        )

        val userRef = db.collection("users").document(uid)
        try {
            userRef.set(profile.toCreateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, userRef.path) { auth }
            throw e
        }

        // Initialize starter EXPIRED membership record (only admin activates paid cycles)
        val membershipRef = userRef.collection("memberships").document("current")
        val existingMembership = runCatching { membershipRef.get().await() }.getOrNull()
        if (existingMembership == null || !existingMembership.exists()) {
            val starterMembership = MembershipRecordDoc(
                userId = uid,
                planName = AppConfig.DEFAULT_PLAN_NAME,
                monthlyFeeUsd = AppConfig.DEFAULT_MONTHLY_FEE_USD,
                membershipStartDate = joinedDate,
                cycleStartDate = joinedDate,
                cycleEndDate = joinedDate,
                nextRenewalDate = joinedDate,
                status = "EXPIRED",
                autoRenewEnabled = false
            )
            try {
                membershipRef.set(starterMembership.toCreateMap()).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, membershipRef.path) { auth }
            }
        }

        // Initialize default notification preferences
        val settingsRef = userRef.collection("settings").document("notifications")
        val existingSettings = runCatching { settingsRef.get().await() }.getOrNull()
        if (existingSettings == null || !existingSettings.exists()) {
            val defaultSettings = MemberSettingsDoc(userId = uid)
            try {
                settingsRef.set(defaultSettings.toWriteMap()).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, settingsRef.path) { auth }
            }
        }

        // Initialize non-PII public leaderboard record
        val lbRef = db.collection("leaderboard_stats").document(uid)
        val existingLb = runCatching { lbRef.get().await() }.getOrNull()
        if (existingLb == null || !existingLb.exists()) {
            val starterLb = LeaderboardStatDoc(
                userId = uid,
                memberName = profile.fullName,
                avatarData = profile.avatarData,
                monthKey = monthKeyFormat.format(now.time)
            )
            try {
                lbRef.set(starterLb.toCreateMap()).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, lbRef.path) { auth }
            }
        }

        // Record pending referral if user entered a friend's referral code
        if (!profile.referredByCode.isNullOrBlank()) {
            val referralRef = db.collection("referrals").document("ref_$uid")
            val existingRef = runCatching { referralRef.get().await() }.getOrNull()
            if (existingRef == null || !existingRef.exists()) {
                val referralDoc = ReferralRecordDoc(
                    referrerCode = profile.referredByCode,
                    referredUserId = uid,
                    referredName = profile.fullName
                )
                try {
                    referralRef.set(referralDoc.toCreateMap()).await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.CREATE, referralRef.path) { auth }
                }
            }
        }

        return profile
    }

    suspend fun updateMemberProfile(
        fullName: String,
        phone: String?,
        avatarData: String?,
        fitnessGoal: String?
    ) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val updateMap = UserProfileDoc(
            userId = uid,
            fullName = fullName.trim().ifBlank { "Club Member" },
            phone = phone?.trim()?.takeIf { it.isNotBlank() },
            avatarData = avatarData,
            fitnessGoal = fitnessGoal?.trim()?.takeIf { it.isNotBlank() }
        ).toOwnerUpdateMap(
            newFullName = fullName.trim().ifBlank { "Club Member" },
            newPhone = phone?.trim()?.takeIf { it.isNotBlank() },
            newAvatarData = avatarData,
            newFitnessGoal = fitnessGoal?.trim()?.takeIf { it.isNotBlank() }
        )
        try {
            docRef.update(updateMap).await()
            // Sync non-PII display name / avatar to leaderboard_stats if it exists
            val lbRef = db.collection("leaderboard_stats").document(uid)
            val lbSnap = runCatching { lbRef.get().await() }.getOrNull()
            if (lbSnap != null && lbSnap.exists()) {
                val lbUpdates = buildMap<String, Any> {
                    put("memberName", fullName.trim().ifBlank { "Club Member" })
                    if (!avatarData.isNullOrBlank()) put("avatarData", avatarData)
                    put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp())
                }
                lbRef.update(lbUpdates).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path) { auth }
            throw e
        }
    }

    // --- 2. Membership, Payment Requests & Verified Payments ---

    fun observeMembership(userId: String): Flow<MembershipRecordDoc?> {
        val path = "users/$userId/memberships/current"
        return db.collection("users").document(userId)
            .collection("memberships").document("current")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(MembershipRecordDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .handleSnapshotErrors(OperationType.GET, path, userId)
    }

    fun observePaymentHistory(userId: String): Flow<List<PaymentRecordDoc>> {
        val path = "users/$userId/payments"
        return db.collection("users").document(userId)
            .collection("payments")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PaymentRecordDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .handleSnapshotErrors(OperationType.LIST, path, userId)
    }

    fun observePaymentRequests(userId: String): Flow<List<PaymentRequestDoc>> {
        val path = "users/$userId/payment_requests"
        return db.collection("users").document(userId)
            .collection("payment_requests")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PaymentRequestDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .handleSnapshotErrors(OperationType.LIST, path, userId)
    }

    suspend fun submitPaymentRequest(
        memberName: String,
        memberCode: String,
        amountFormatted: String,
        paymentMethod: String,
        referenceNote: String?
    ): String {
        val uid = requireUserId()
        val requestId = "req_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("users").document(uid)
            .collection("payment_requests").document(requestId)
        val payload = PaymentRequestDoc(
            userId = uid,
            memberName = memberName,
            memberCode = memberCode,
            amountFormatted = amountFormatted,
            paymentMethod = paymentMethod,
            referenceNote = referenceNote?.trim()?.takeIf { it.isNotBlank() },
            paymentDateLabel = shortDateFormat.format(Calendar.getInstance().time),
            status = "PENDING_VERIFICATION",
            isVerified = false
        ).toCreateMap()

        try {
            docRef.set(payload).await()
            return requestId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path) { auth }
            throw e
        }
    }

    // --- 3. Common Entrance QR Attendance ---

    fun observeAttendanceRecords(userId: String): Flow<List<AttendanceRecordDoc>> {
        val path = "users/$userId/attendance"
        return db.collection("users").document(userId)
            .collection("attendance")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AttendanceRecordDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.dateKey }
            }
            .handleSnapshotErrors(OperationType.LIST, path, userId)
    }

    suspend fun recordDailyEntranceAttendance(
        dateKey: String,
        gymCode: String,
        verificationToken: String,
        zoneLabel: String,
        memberNameForLeaderboard: String,
        avatarDataForLeaderboard: String?
    ): AttendanceRecordDoc {
        val uid = requireUserId()
        val now = Calendar.getInstance()
        val dateLabel = shortDateFormat.format(now.time)
        val checkInTime = timeFormat.format(now.time)

        val attCollection = db.collection("users").document(uid).collection("attendance")
        val docRef = attCollection.document(dateKey)

        // Pre-check to fail cleanly before write if doc already exists for today
        val existingSnap = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path) { auth }
            throw e
        }

        if (existingSnap.exists()) {
            throw IllegalStateException("You have already checked in today ($dateKey).")
        }

        val record = AttendanceRecordDoc(
            id = dateKey,
            userId = uid,
            dateKey = dateKey,
            dateLabel = dateLabel,
            checkInTime = checkInTime,
            zoneLabel = zoneLabel,
            gymCode = gymCode,
            verificationToken = verificationToken,
            verifiedViaCommonQr = true
        )

        try {
            docRef.set(record.toCreateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path) { auth }
            throw e
        }

        // Recompute metrics from real attendance docs and sync non-PII leaderboard_stats/{uid}
        val allRecords = runCatching {
            attCollection.get().await().toObjects(AttendanceRecordDoc::class.java, ServerTimestampBehavior.ESTIMATE)
        }.getOrElse { listOf(record) }

        val metrics = StreakAndLeaderboardEngine.computeAttendanceMetrics(allRecords, now)
        syncAttendanceToLeaderboard(
            uid = uid,
            memberName = memberNameForLeaderboard,
            avatarData = avatarDataForLeaderboard,
            currentStreakDays = metrics.currentStreakDays,
            longestStreakDays = metrics.longestStreakDays,
            monthlyVisits = metrics.monthlyVisits,
            weeklyTargetMetWeeks = metrics.weeklyTargetMetWeeks,
            monthKey = monthKeyFormat.format(now.time)
        )

        return record
    }

    private suspend fun syncAttendanceToLeaderboard(
        uid: String,
        memberName: String,
        avatarData: String?,
        currentStreakDays: Int,
        longestStreakDays: Int,
        monthlyVisits: Int,
        weeklyTargetMetWeeks: Int,
        monthKey: String
    ) {
        val lbRef = db.collection("leaderboard_stats").document(uid)
        val snap = runCatching { lbRef.get().await() }.getOrNull()
        if (snap != null && snap.exists()) {
            val existing = snap.toObject(LeaderboardStatDoc::class.java, ServerTimestampBehavior.ESTIMATE)
            // Respect rule cap: at most +1 step per update if streak jumped cleanly
            val safeStreak = if (existing != null) {
                currentStreakDays.coerceAtMost(existing.currentStreakDays.toInt() + 1)
            } else currentStreakDays
            val safeMonthly = if (existing != null) {
                monthlyVisits.coerceAtMost(existing.monthlyVisits.toInt() + 1)
            } else monthlyVisits
            val safeLongest = maxOf(longestStreakDays, existing?.longestStreakDays?.toInt() ?: 0)
            val updates = buildMap<String, Any> {
                put("memberName", memberName.ifBlank { existing?.memberName ?: "Club Member" })
                if (!avatarData.isNullOrBlank()) put("avatarData", avatarData)
                put("currentStreakDays", safeStreak.toLong())
                put("longestStreakDays", safeLongest.toLong())
                put("monthlyVisits", safeMonthly.toLong())
                put("weeklyTargetMetWeeks", weeklyTargetMetWeeks.toLong())
                put("healthyHabitsScore", existing?.healthyHabitsScore ?: 0L)
                put("monthKey", monthKey)
                put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp())
            }
            try {
                lbRef.update(updates).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, lbRef.path) { auth }
            }
        } else {
            val doc = LeaderboardStatDoc(
                userId = uid,
                memberName = memberName.ifBlank { "Club Member" },
                avatarData = avatarData,
                currentStreakDays = currentStreakDays.toLong(),
                longestStreakDays = longestStreakDays.toLong(),
                monthlyVisits = monthlyVisits.toLong(),
                weeklyTargetMetWeeks = weeklyTargetMetWeeks.toLong(),
                healthyHabitsScore = 0L,
                verifiedReferralsCount = 0L,
                isMonthlyWinner = false,
                monthKey = monthKey
            )
            try {
                lbRef.set(doc.toCreateMap()).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, lbRef.path) { auth }
            }
        }
    }

    // --- 4. Daily Food & Nutrition Logs ---

    fun observeFoodLogs(userId: String): Flow<List<DailyFoodLogDoc>> {
        val path = "users/$userId/food_logs"
        return db.collection("users").document(userId)
            .collection("food_logs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DailyFoodLogDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.dateKey }
            }
            .handleSnapshotErrors(OperationType.LIST, path, userId)
    }

    suspend fun saveDailyFoodLog(
        dateKey: String,
        dateLabel: String,
        servingsByFood: Map<String, Int>,
        allLogsMapForScore: Map<String, DailyFoodLogDoc>,
        memberNameForLeaderboard: String
    ) {
        val uid = requireUserId()
        val (protein, calories, foodsCount) = NutritionAnalyticsEngine.calculateTotals(servingsByFood)
        val takenIds = servingsByFood.filterValues { it > 0 }.keys.sorted()
        val servingsLongMap = servingsByFood.filterValues { it > 0 }.mapValues { it.value.toLong() }

        val doc = DailyFoodLogDoc(
            id = dateKey,
            userId = uid,
            dateKey = dateKey,
            dateLabel = dateLabel,
            takenFoodIds = takenIds,
            servingsByFood = servingsLongMap,
            totalProteinGrams = protein.toLong(),
            totalCalories = calories.toLong(),
            foodsCount = foodsCount.toLong()
        )

        val docRef = db.collection("users").document(uid)
            .collection("food_logs").document(dateKey)

        try {
            docRef.set(doc.toWriteMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path) { auth }
            throw e
        }

        // Sync updated healthy habits score to public leaderboard
        val updatedMap = allLogsMapForScore.toMutableMap().apply { put(dateKey, doc) }
        val newHabitsScore = NutritionAnalyticsEngine.calculateHealthyHabitsScore(updatedMap)
        val lbRef = db.collection("leaderboard_stats").document(uid)
        val lbSnap = runCatching { lbRef.get().await() }.getOrNull()
        if (lbSnap != null && lbSnap.exists()) {
            try {
                lbRef.update(
                    mapOf(
                        "healthyHabitsScore" to newHabitsScore.toLong(),
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, lbRef.path) { auth }
            }
        } else {
            val newLb = LeaderboardStatDoc(
                userId = uid,
                memberName = memberNameForLeaderboard.ifBlank { "Club Member" },
                healthyHabitsScore = newHabitsScore.toLong(),
                monthKey = monthKeyFormat.format(Calendar.getInstance().time)
            )
            try {
                lbRef.set(newLb.toCreateMap()).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, lbRef.path) { auth }
            }
        }
    }

    // --- 5. Member Notification & Reminder Settings ---

    fun observeMemberSettings(userId: String): Flow<MemberSettingsDoc?> {
        val path = "users/$userId/settings/notifications"
        return db.collection("users").document(userId)
            .collection("settings").document("notifications")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(MemberSettingsDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .handleSnapshotErrors(OperationType.GET, path, userId)
    }

    suspend fun saveMemberSettings(settings: MemberSettingsDoc) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("settings").document("notifications")
        val normalized = settings.copy(userId = uid)
        try {
            docRef.set(normalized.toWriteMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path) { auth }
            throw e
        }
    }

    // --- 6. Public Club Leaderboards, Announcements, Gym Config & App Config ---

    fun observeLeaderboardStats(): Flow<List<LeaderboardStatDoc>> {
        val path = "leaderboard_stats"
        return flow {
            requireUserId()
            emitAll(
                db.collection(path)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(LeaderboardStatDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                    }
                    .handleSnapshotErrors(OperationType.LIST, path, null)
            )
        }
    }

    fun observeLeaderboardHistory(): Flow<List<LeaderboardHistoryDoc>> {
        val path = "leaderboard_history"
        return flow {
            requireUserId()
            emitAll(
                db.collection(path)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(LeaderboardHistoryDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                            .sortedByDescending { it.monthKey }
                    }
                    .handleSnapshotErrors(OperationType.LIST, path, null)
            )
        }
    }

    fun observeAnnouncements(): Flow<List<AnnouncementDoc>> {
        val path = "announcements"
        return flow {
            requireUserId()
            emitAll(
                db.collection(path)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(AnnouncementDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                            .filter { it.isPublished }
                            .sortedByDescending { it.createdAt?.seconds ?: 0L }
                    }
                    .handleSnapshotErrors(OperationType.LIST, path, null)
            )
        }
    }

    fun observeCompetitionConfigs(): Flow<List<CompetitionConfigDoc>> {
        val path = "competition_configs"
        return flow {
            requireUserId()
            emitAll(
                db.collection(path)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(CompetitionConfigDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                    }
                    .handleSnapshotErrors(OperationType.LIST, path, null)
            )
        }
    }

    fun observePromotionalOffers(): Flow<List<PromotionalOfferDoc>> {
        val path = "promotional_offers"
        return flow {
            requireUserId()
            emitAll(
                db.collection(path)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(PromotionalOfferDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                            .filter { it.isActive }
                            .sortedByDescending { it.createdAt?.seconds ?: 0L }
                    }
                    .handleSnapshotErrors(OperationType.LIST, path, null)
            )
        }
    }

    fun observeBroadcastNotifications(): Flow<List<BroadcastNotificationDoc>> {
        val path = "notifications_broadcast"
        return flow {
            requireUserId()
            emitAll(
                db.collection(path)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(BroadcastNotificationDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                            .sortedByDescending { it.createdAt?.seconds ?: 0L }
                    }
                    .handleSnapshotErrors(OperationType.LIST, path, null)
            )
        }
    }

    fun observeGymConfig(): Flow<GymConfigDoc?> {
        val path = "gym_config/main_entrance"
        return flow {
            requireUserId()
            emitAll(
                db.collection("gym_config").document("main_entrance")
                    .snapshots()
                    .map { snap ->
                        if (snap.exists()) snap.toObject(GymConfigDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                        else null
                    }
                    .handleSnapshotErrors(OperationType.GET, path, null)
            )
        }
    }

    fun observeAppConfig(): Flow<AppConfigDoc?> {
        val path = "app_config/current"
        return flow {
            requireUserId()
            emitAll(
                db.collection("app_config").document("current")
                    .snapshots()
                    .map { snap ->
                        if (snap.exists()) snap.toObject(AppConfigDoc::class.java, ServerTimestampBehavior.ESTIMATE)
                        else null
                    }
                    .handleSnapshotErrors(OperationType.GET, path, null)
            )
        }
    }

    suspend fun logQrSecurityEvent(
        memberName: String,
        memberCode: String?,
        eventType: String,
        reason: String,
        rawCodePrefix: String?,
        distanceMeters: Double?
    ) {
        val uid = auth.currentUser?.uid ?: return
        val eventId = "qre_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("qr_security_events").document(eventId)
        val payload = QrSecurityEventDoc(
            id = eventId,
            userId = uid,
            memberName = memberName.ifBlank { "Member" }.take(80),
            memberCode = memberCode?.take(32),
            eventType = eventType,
            reason = reason.take(260),
            rawCodePrefix = rawCodePrefix?.take(64),
            distanceMeters = distanceMeters,
            reviewed = false
        ).toCreateMap()
        runCatching { docRef.set(payload).await() }
    }

    fun observeMyReferrals(userId: String): Flow<List<ReferralRecordDoc>> {
        val path = "referrals"
        return db.collection(path)
            .whereEqualTo("referrerUserId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ReferralRecordDoc::class.java, ServerTimestampBehavior.ESTIMATE)
            }
            .handleSnapshotErrors(OperationType.LIST, path, userId)
    }

    suspend fun getGymConfig(): GymConfigDoc? {
        requireUserId()
        val docRef = db.collection("gym_config").document("main_entrance")
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) snap.toObject(GymConfigDoc::class.java) else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Non-fatal fallback to AppConfig defaults when admin hasn't provisioned gym_config doc yet
            if ((e as? FirebaseFirestoreException)?.code != FirebaseFirestoreException.Code.NOT_FOUND) {
                handleFirestoreError(e, OperationType.GET, docRef.path) { auth }
            }
            null
        }
    }

    suspend fun getAppConfig(): AppConfigDoc? {
        requireUserId()
        val docRef = db.collection("app_config").document("current")
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) snap.toObject(AppConfigDoc::class.java) else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if ((e as? FirebaseFirestoreException)?.code != FirebaseFirestoreException.Code.NOT_FOUND) {
                handleFirestoreError(e, OperationType.GET, docRef.path) { auth }
            }
            null
        }
    }

    /**
     * Convenience date key helper for YYYY-MM-DD formatting.
     */
    fun todayDateKey(calendar: Calendar = Calendar.getInstance()): String =
        dateKeyFormat.format(calendar.time)

    fun todayDateLabel(calendar: Calendar = Calendar.getInstance()): String =
        shortDateFormat.format(calendar.time)
}
