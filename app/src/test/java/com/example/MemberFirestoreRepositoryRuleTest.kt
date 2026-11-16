package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.config.AppConfig
import com.example.database.MemberFirestoreRepository
import com.example.database.MemberSettingsDoc
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MemberFirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun uniqueSuffix(): String = UUID.randomUUID().toString().take(8)

    @Test
    fun registerMemberProfile_and_observeProfile_succeedsForOwner() = runBlocking {
        val aliceEmail = "alice_${uniqueSuffix()}@gympulse.fit"
        val uid = signInTestUser(aliceEmail)
        val repo = MemberFirestoreRepository(firestore, auth)

        val created = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.registerMemberProfile(
                fullName = "Alice Vance",
                phone = "+1 555 0110",
                email = aliceEmail,
                avatarData = null,
                fitnessGoal = "Lean Strength",
                referredByCode = null
            )
        }
        assertEquals(uid, created.userId)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(uid).first { it != null }
        }
        assertNotNull(observed)
        assertEquals("Alice Vance", observed?.fullName)
        assertEquals("MEMBER", observed?.role)
    }

    @Test
    fun recordDailyEntranceAttendance_succeedsOnce_andBlocksDuplicateSameDay() = runBlocking {
        val aliceEmail = "att_${uniqueSuffix()}@gympulse.fit"
        val uid = signInTestUser(aliceEmail)
        val repo = MemberFirestoreRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.registerMemberProfile("Alice Attendance", null, aliceEmail, null, null, null)
        }

        val dateKey = "2026-11-${10 + (kotlin.math.abs(uid.hashCode()) % 18)}"
        val record = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.recordDailyEntranceAttendance(
                dateKey = dateKey,
                gymCode = AppConfig.COMMON_ENTRANCE_QR_CODE,
                verificationToken = "WIN_12345_sig",
                zoneLabel = "Main Entrance QR",
                memberNameForLeaderboard = "Alice Attendance",
                avatarDataForLeaderboard = null
            )
        }
        assertEquals(dateKey, record.dateKey)

        // Second check-in on the exact same dateKey must fail
        var duplicateRejected = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repo.recordDailyEntranceAttendance(
                    dateKey = dateKey,
                    gymCode = AppConfig.COMMON_ENTRANCE_QR_CODE,
                    verificationToken = "WIN_12346_sig",
                    zoneLabel = "Main Entrance QR",
                    memberNameForLeaderboard = "Alice Attendance",
                    avatarDataForLeaderboard = null
                )
            }
        } catch (e: IllegalStateException) {
            duplicateRejected = true
        }
        assertTrue("Duplicate same-day attendance should be rejected", duplicateRejected)
    }

    @Test
    fun saveDailyFoodLog_and_memberSettings_succeedForOwner() = runBlocking {
        val aliceEmail = "food_${uniqueSuffix()}@gympulse.fit"
        val uid = signInTestUser(aliceEmail)
        val repo = MemberFirestoreRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.registerMemberProfile("Alice Fuel", null, aliceEmail, null, null, null)
            repo.saveDailyFoodLog(
                dateKey = "2026-11-16",
                dateLabel = "Nov 16, 2026",
                servingsByFood = mapOf("f_eggs" to 2, "f_chicken" to 1),
                allLogsMapForScore = emptyMap(),
                memberNameForLeaderboard = "Alice Fuel"
            )
            repo.saveMemberSettings(MemberSettingsDoc(userId = uid, attendanceReminders = true, leaderboardUpdates = true))
        }

        val logs = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeFoodLogs(uid).first { it.isNotEmpty() }
        }
        assertEquals(78L, logs.first().totalProteinGrams)
    }

    @Test
    fun submitPaymentRequest_createsPendingUnverifiedRequest_andBlocksDirectActiveMembership() = runBlocking {
        val aliceEmail = "pay_${uniqueSuffix()}@gympulse.fit"
        val uid = signInTestUser(aliceEmail)
        val repo = MemberFirestoreRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.registerMemberProfile("Alice Member", null, aliceEmail, null, null, null)
        }

        val reqId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitPaymentRequest(
                memberName = "Alice Member",
                memberCode = "GP-1234",
                amountFormatted = "$69.00 / month",
                paymentMethod = "Instant Bank / UPI",
                referenceNote = "UPI-99201"
            )
        }
        assertTrue(reqId.isNotBlank())

        val requests = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePaymentRequests(uid).first { it.isNotEmpty() }
        }
        assertEquals("PENDING_VERIFICATION", requests.first().status)
        assertEquals(false, requests.first().isVerified)

        // Direct client activation of membership must be rejected by rules
        var activationBlocked = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(uid)
                    .collection("memberships").document("current")
                    .update("status", "ACTIVE")
                    .await()
            }
        } catch (e: FirebaseFirestoreException) {
            activationBlocked = e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Ordinary member cannot activate own membership", activationBlocked)
    }

    @Test
    fun crossUserRead_and_unauthenticatedStream_failWithPermissionDenied() = runBlocking {
        val aliceEmail = "iso_a_${uniqueSuffix()}@gympulse.fit"
        val bobEmail = "iso_b_${uniqueSuffix()}@gympulse.fit"

        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = MemberFirestoreRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.registerMemberProfile("Alice Private", null, aliceEmail, null, null, null)
        }

        // Bob tries to read Alice's private user doc
        signInTestUser(bobEmail)
        var crossUserDenied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).get().await()
            }
        } catch (e: FirebaseFirestoreException) {
            crossUserDenied = e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Cross-user private profile read must be denied", crossUserDenied)

        // Signed-out caller observing protected stream fails with nested PERMISSION_DENIED
        auth.signOut()
        val unauthRepo = MemberFirestoreRepository(firestore, auth)
        var unauthDenied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                unauthRepo.observeUserProfile(aliceUid).first()
            }
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            unauthDenied = fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Unauthenticated listener must reject with PERMISSION_DENIED", unauthDenied)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
