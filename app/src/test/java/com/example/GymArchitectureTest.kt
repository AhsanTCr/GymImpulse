package com.example

import com.example.config.AppConfig
import com.example.database.AttendanceRecordDoc
import com.example.database.DailyFoodLogDoc
import com.example.database.LeaderboardStatDoc
import com.example.database.MembershipRecordDoc
import com.example.integration.StandardGymSyncBridge
import com.example.models.AdminOverviewPeriod
import com.example.models.DataSourceType
import com.example.models.LeaderboardCategory
import com.example.models.MemberActivitySegment
import com.example.models.MembershipStatus
import com.example.models.NutritionChartPeriod
import com.example.services.SampleGymData
import com.example.utils.AdminAnalyticsEngine
import com.example.utils.AvatarUtils
import com.example.utils.DateTimeUtils
import com.example.utils.GitHubReleaseChecker
import com.example.utils.MembershipCycleCalculator
import com.example.utils.NutritionAnalyticsEngine
import com.example.utils.QrAntiFraudVerifier
import com.example.utils.QrMatrixGenerator
import com.example.utils.QrVerificationResult
import com.example.utils.StreakAndLeaderboardEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GymArchitectureTest {

    @Test
    fun `membership cycle anchors directly to payment date`() {
        val (start, end) = DateTimeUtils.calculateCycleFromPaymentDate(2026, 12, 4)
        assertEquals("Dec 4, 2026", start)
        assertEquals("Jan 4, 2027", end)
    }

    @Test
    fun `membership cycle calculator derives active expiring and overdue states`() {
        val fmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val now = Calendar.getInstance()
        val futureEnd = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 15) }
        val soonEnd = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 2) }
        val oldEnd = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }

        assertEquals(
            MembershipStatus.ACTIVE,
            MembershipCycleCalculator.parseStatus("ACTIVE", fmt.format(futureEnd.time), now.timeInMillis)
        )
        assertEquals(
            MembershipStatus.EXPIRING_SOON,
            MembershipCycleCalculator.parseStatus("ACTIVE", fmt.format(soonEnd.time), now.timeInMillis)
        )
        assertEquals(
            MembershipStatus.PAYMENT_OVERDUE,
            MembershipCycleCalculator.parseStatus("ACTIVE", fmt.format(oldEnd.time), now.timeInMillis)
        )
        val overview = MembershipCycleCalculator.toOverview(
            MembershipRecordDoc(status = "ACTIVE", cycleStartDate = fmt.format(now.time), cycleEndDate = fmt.format(futureEnd.time)),
            emptyList(),
            now.timeInMillis
        )
        assertEquals(MembershipStatus.ACTIVE, overview.status)
    }

    @Test
    fun `nutrition missing data rules estimate 1 and 2 missing days and mark 3 plus as not tracked`() {
        val keys = listOf(
            "2026-11-01", // logged 120g
            "2026-11-02", // 1 missing day -> ESTIMATED (140g avg of 120 & 160)
            "2026-11-03", // logged 160g
            "2026-11-04", // 2 consecutive missing -> ESTIMATED
            "2026-11-05", // 2 consecutive missing -> ESTIMATED
            "2026-11-06", // logged 140g
            "2026-11-07", // 3 consecutive missing -> MISSING (Not tracked)
            "2026-11-08", // 3 consecutive missing -> MISSING (Not tracked)
            "2026-11-09"  // 3 consecutive missing -> MISSING (Not tracked)
        )
        val logs = mapOf(
            "2026-11-01" to DailyFoodLogDoc(dateKey = "2026-11-01", totalProteinGrams = 120, foodsCount = 5),
            "2026-11-03" to DailyFoodLogDoc(dateKey = "2026-11-03", totalProteinGrams = 160, foodsCount = 7),
            "2026-11-06" to DailyFoodLogDoc(dateKey = "2026-11-06", totalProteinGrams = 140, foodsCount = 6)
        )

        val resolved = NutritionAnalyticsEngine.resolveDailySeriesWithMissingRules(keys, logs)
        assertEquals(DataSourceType.LOGGED, resolved[0].third)
        assertEquals(120, resolved[0].second)

        // Single missing day estimated using suitable average (140g)
        assertEquals(DataSourceType.ESTIMATED, resolved[1].third)
        assertEquals(140, resolved[1].second)

        // Two consecutive missing days estimated using suitable average
        assertEquals(DataSourceType.ESTIMATED, resolved[3].third)
        assertEquals(DataSourceType.ESTIMATED, resolved[4].third)

        // Three consecutive missing days NEVER estimated -> MISSING
        assertEquals(DataSourceType.MISSING, resolved[6].third)
        assertEquals(0, resolved[6].second)
        assertEquals(DataSourceType.MISSING, resolved[7].third)
        assertEquals(DataSourceType.MISSING, resolved[8].third)
    }

    @Test
    fun `nutrition totals support multiple servings per food`() {
        val (protein, calories, foodsCount) = NutritionAnalyticsEngine.calculateTotals(
            mapOf("f_eggs" to 2, "f_chicken" to 1)
        )
        assertEquals(2, foodsCount)
        assertEquals(18 * 2 + 42, protein)
        assertEquals(210 * 2 + 265, calories)
    }

    @Test
    fun `qr anti fraud verifier validates live rotating code and blocks stale screenshots and duplicates`() {
        val now = 1_763_320_000_000L
        val validPayload = QrAntiFraudVerifier.generateRotatingEntrancePayload(nowMillis = now)
        val ok = QrAntiFraudVerifier.verifyCheckIn(
            rawPayload = validPayload,
            alreadyCheckedInToday = false,
            nowMillis = now
        )
        assertTrue(ok is QrVerificationResult.Valid)

        // Duplicate same-day scan fails
        val dup = QrAntiFraudVerifier.verifyCheckIn(
            rawPayload = validPayload,
            alreadyCheckedInToday = true,
            nowMillis = now
        )
        assertTrue(dup is QrVerificationResult.Invalid)

        // Expired screenshot (10 minutes old) fails
        val stalePayload = QrAntiFraudVerifier.generateRotatingEntrancePayload(nowMillis = now - 600_000L)
        val stale = QrAntiFraudVerifier.verifyCheckIn(
            rawPayload = stalePayload,
            alreadyCheckedInToday = false,
            nowMillis = now
        )
        assertTrue(stale is QrVerificationResult.Invalid)

        // Out of range location fails
        val outOfRange = QrAntiFraudVerifier.verifyCheckIn(
            rawPayload = validPayload,
            alreadyCheckedInToday = false,
            nowMillis = now,
            memberLat = 37.85,
            memberLng = -122.42,
            gymLat = AppConfig.DEFAULT_GYM_LATITUDE,
            gymLng = AppConfig.DEFAULT_GYM_LONGITUDE,
            allowedRadiusMeters = 500.0
        )
        assertTrue(outOfRange is QrVerificationResult.Invalid)
    }

    @Test
    fun `streak engine computes consecutive streak longest streak and weekly consistency`() {
        val today = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 16, 10, 0, 0)
        }
        val records = listOf(
            AttendanceRecordDoc(dateKey = "2026-11-16"),
            AttendanceRecordDoc(dateKey = "2026-11-15"),
            AttendanceRecordDoc(dateKey = "2026-11-14"),
            AttendanceRecordDoc(dateKey = "2026-11-12"),
            AttendanceRecordDoc(dateKey = "2026-11-11"),
            AttendanceRecordDoc(dateKey = "2026-11-10")
        )
        val metrics = StreakAndLeaderboardEngine.computeAttendanceMetrics(records, today)
        assertEquals(3, metrics.currentStreakDays)
        assertEquals(3, metrics.longestStreakDays)
        assertEquals(6, metrics.monthlyVisits)
        assertEquals(1, metrics.weeklyTargetMetWeeks)
    }

    @Test
    fun `leaderboard engine ranks members accurately across all four categories`() {
        val stats = listOf(
            LeaderboardStatDoc(userId = "u1", memberName = "Jordan", currentStreakDays = 10, healthyHabitsScore = 75, weeklyTargetMetWeeks = 4, verifiedReferralsCount = 1),
            LeaderboardStatDoc(userId = "u2", memberName = "Elena", currentStreakDays = 6, healthyHabitsScore = 96, weeklyTargetMetWeeks = 7, verifiedReferralsCount = 5)
        )
        val streakRank = StreakAndLeaderboardEngine.rankEntries(stats, LeaderboardCategory.ATTENDANCE_STREAK, "u1")
        assertEquals("u1", streakRank.first().memberId)
        assertTrue(streakRank.first().isCurrentUser)

        val referralRank = StreakAndLeaderboardEngine.rankEntries(stats, LeaderboardCategory.REFERRAL_CHAMPION, "u1")
        assertEquals("u2", referralRank.first().memberId)
    }

    @Test
    fun `all four leaderboard categories return ranked entries`() {
        LeaderboardCategory.entries.forEach { category ->
            val entries = SampleGymData.getLeaderboardEntries(category)
            assertTrue("Category $category should have podium + list", entries.size >= 6)
            assertEquals(1, entries.first().rank)
        }
    }

    @Test
    fun `avatar initials extraction handles full and single names`() {
        assertEquals("JV", AvatarUtils.extractInitials("Jordan Vance"))
        assertEquals("AP", AvatarUtils.extractInitials(""))
    }

    @Test
    fun `admin passcode SHA-256 verification accepts exact passcode and rejects wrong input`() {
        assertTrue(AdminAnalyticsEngine.verifyAdminPasscode("waliandsons326842"))
        assertFalse(AdminAnalyticsEngine.verifyAdminPasscode("wrongpasscode"))
        assertFalse(AdminAnalyticsEngine.verifyAdminPasscode(""))
    }

    @Test
    fun `qr matrix generator builds square ISO module grid for entrance code`() {
        val payload = QrAntiFraudVerifier.generateRotatingEntrancePayload()
        val matrix = QrMatrixGenerator.encode(payload)
        assertTrue(matrix.size >= 21)
        assertEquals(matrix.size, matrix.first().size)
    }

    @Test
    fun `external gym sync bridge exports and deduplicates CSV and JSON bundles`() {
        val bridge = StandardGymSyncBridge()
        val csv = """
            memberCode,fullName
            GP-101,Jordan
            GP-101,Jordan
            GP-102,Elena
        """.trimIndent()
        val preview = bridge.previewInboundPayload(csv)
        assertEquals("CSV Roster", preview.formatDetected)
        assertEquals(3, preview.recordCount)
        assertEquals(2, preview.deduplicatedIdsCount)
    }

    @Test
    fun `github release checker parses latest APK asset and compares versions`() {
        val sampleJson = """
            {
              "tag_name": "v1.2.0",
              "published_at": "2026-11-16T10:30:00Z",
              "html_url": "https://github.com/gymimpulse/app/releases/tag/v1.2.0",
              "body": "- Added Admin Portal\n- Improved QR Speed",
              "assets": [
                { "browser_download_url": "https://github.com/gymimpulse/app/releases/download/v1.2.0/Gymimpulse-v1.2.0.apk" }
              ]
            }
        """.trimIndent()
        val parsed = GitHubReleaseChecker.parseReleaseJson(sampleJson)!!
        assertEquals("1.2.0", parsed.version)
        assertEquals("2026-11-16", parsed.releaseDate)
        assertTrue(parsed.downloadUrl!!.endsWith(".apk"))
        assertEquals(2, parsed.highlights.size)
        assertTrue(GitHubReleaseChecker.isNewerVersion("1.2.0", "1.0.0"))
        assertFalse(GitHubReleaseChecker.isNewerVersion("1.0.0", "1.2.0"))
    }
}
