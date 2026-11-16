package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.config.AppConfig
import com.example.database.AdminFirestoreRepository
import com.example.database.AnnouncementDoc
import com.example.database.CompetitionConfigDoc
import com.example.database.GymConfigDoc
import com.example.database.MemberFirestoreRepository
import com.example.database.PromotionalOfferDoc
import com.example.models.BroadcastAudience
import com.example.models.BroadcastType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class AdminFirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun uniqueSuffix(): String = UUID.randomUUID().toString().take(8)

    /**
     * Marks the signed-in Auth Emulator user's email as verified (mimicking Google Sign-In)
     * and refreshes the ID token so `request.auth.token.email_verified == true`.
     */
    private suspend fun verifyEmulatorUserEmailAndRefreshToken() = withContext(Dispatchers.IO) {
        val user = checkNotNull(auth.currentUser) { "Must be signed in" }
        val url = URL("http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/projects/demo-no-project/accounts:update")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer owner")
            doOutput = true
        }
        val body = """{"localId":"${user.uid}","emailVerified":true}"""
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val responseCode = conn.responseCode
        check(responseCode in 200..299) { "Emulator email verification failed: HTTP $responseCode" }
        user.reload().await()
        user.getIdToken(true).await()
        check(user.isEmailVerified) { "Expected user.isEmailVerified == true after Auth Emulator admin update" }
    }

    @Test
    fun verifiedAdmin_canConfirmPaymentAnchoredToPaymentDate_andUpdateAuditLogs() = runBlocking {
        // 1. Register a normal member & submit a pending payment request
        val memberEmail = "mem_${uniqueSuffix()}@gymimpulse.fit"
        val memberUid = signInTestUser(memberEmail)
        val memberRepo = MemberFirestoreRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            memberRepo.registerMemberProfile(
                fullName = "Taylor Brooks",
                phone = "+1 555 0199",
                email = memberEmail,
                avatarData = null,
                fitnessGoal = "Consistency & Core",
                referredByCode = null
            )
            memberRepo.submitPaymentRequest(
                memberName = "Taylor Brooks",
                memberCode = "GP-1900",
                amountFormatted = "$69.00",
                paymentMethod = "Bank Transfer",
                referenceNote = "UPI-DEC04"
            )
        }
        val pendingRequests = withTimeout(FLOW_TIMEOUT_MS) {
            memberRepo.observePaymentRequests(memberUid).first { it.isNotEmpty() }
        }
        val requestDoc = pendingRequests.first()

        // 2. Sign in as verified club admin
        signInTestUser("hereyourmax@gmail.com")
        verifyEmulatorUserEmailAndRefreshToken()

        val adminRepo = AdminFirestoreRepository(firestore, auth)
        val isAuthorized = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeIsAdminAuthorized(auth.currentUser!!.uid).first()
        }
        assertTrue("Verified admin email must be authorized", isAuthorized)

        // 3. Confirm payment anchored to Dec 4, 2026 -> Jan 4, 2027
        withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.confirmPaymentAndRenewMembership(
                memberUserId = memberUid,
                memberName = "Taylor Brooks",
                existingMembership = null,
                pendingRequest = requestDoc,
                paymentYear = 2026,
                paymentMonthOneBased = 12,
                paymentDayOfMonth = 4,
                amountUsd = 69.0,
                paymentMethod = "Bank Transfer",
                referenceNumber = "UPI-DEC04"
            )
        }

        // Verify membership activated with Dec 4, 2026 -> Jan 4, 2027 cycle and audit logged
        val updatedMemberships = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAllMemberships().first { list ->
                list.any { it.userId == memberUid && it.status == "ACTIVE" }
            }
        }
        val activeMem = updatedMemberships.first { it.userId == memberUid }
        assertEquals("Dec 4, 2026", activeMem.cycleStartDate)
        assertEquals("Jan 4, 2027", activeMem.cycleEndDate)

        val auditLogs = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAuditLogs().first { logs ->
                logs.any { it.actionType == "CONFIRM_PAYMENT" && it.targetId == memberUid }
            }
        }
        assertTrue(auditLogs.any { it.targetId == memberUid })
    }

    @Test
    fun verifiedAdmin_canPublishCompetitionsOffersAnnouncementsBroadcastsAndQrConfig() = runBlocking {
        signInTestUser("hereyourmax@gmail.com")
        verifyEmulatorUserEmailAndRefreshToken()

        val adminRepo = AdminFirestoreRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.saveCompetitionConfig(
                CompetitionConfigDoc(
                    id = "attendance_streak",
                    categoryId = "ATTENDANCE_STREAK",
                    title = "Attendance Streak",
                    periodLabel = "Dec 2026",
                    targetDescription = "Build consecutive check-ins",
                    targetValue = 21,
                    rewardTitle = "1 Month Free + Gymimpulse Hoodie",
                    isActive = true
                )
            )
            adminRepo.savePromotionalOffer(
                PromotionalOfferDoc(
                    title = "Year-End Pro Discount",
                    subtitle = "Save 15% on 3-month renewals verified at front desk.",
                    badgeText = "15% OFF",
                    discountCode = "IMPULSE15",
                    validUntilLabel = "Dec 31, 2026",
                    targetAudience = BroadcastAudience.ALL_MEMBERS.name,
                    isActive = true
                )
            )
            adminRepo.saveAnnouncement(
                AnnouncementDoc(
                    title = "Winter Lift Meet",
                    description = "Join the Saturday bench & deadlift showcase.",
                    dateLabel = "Dec 12, 2026",
                    category = "COMPETITION",
                    isImportant = true,
                    isPublished = true
                )
            )
            adminRepo.sendBroadcastNotification(
                title = "Holiday Hours Alert",
                message = "Open 6 AM - 6 PM this holiday.",
                type = BroadcastType.IMPORTANT_UPDATE.name,
                audience = BroadcastAudience.ALL_MEMBERS.name,
                isUrgent = true
            )
            adminRepo.updateGymQrConfig(
                config = GymConfigDoc(
                    id = "main_entrance",
                    gymId = "main_entrance",
                    gymName = AppConfig.GYM_NAME,
                    staticEntranceCode = AppConfig.COMMON_ENTRANCE_QR_CODE,
                    rotationWindowSeconds = 90L,
                    isQrEnabled = true,
                    allowStaticFallback = true,
                    qrVersion = 2L
                ),
                actionLabel = "Rotated QR window to 90s"
            )
        }

        // Now sign in as a regular member and verify sync + strict write isolation
        val regularEmail = "reg_${uniqueSuffix()}@gymimpulse.fit"
        val regularUid = signInTestUser(regularEmail)
        val memberRepo = MemberFirestoreRepository(firestore, auth)

        val isRegularAuthorized = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeIsAdminAuthorized(regularUid).first()
        }
        assertFalse("Regular member must NEVER pass admin authorization", isRegularAuthorized)

        val offers = withTimeout(FLOW_TIMEOUT_MS) {
            memberRepo.observePromotionalOffers().first { it.isNotEmpty() }
        }
        assertTrue(offers.any { it.discountCode == "IMPULSE15" })

        val broadcasts = withTimeout(FLOW_TIMEOUT_MS) {
            memberRepo.observeBroadcastNotifications().first { it.isNotEmpty() }
        }
        assertTrue(broadcasts.any { it.title == "Holiday Hours Alert" })

        // Regular member must be denied when attempting admin-only list/write calls
        var adminReadDenied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                adminRepo.observeAllUsers().first()
            }
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            adminReadDenied = fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Normal member reading all /users must receive PERMISSION_DENIED", adminReadDenied)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
