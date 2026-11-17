package com.example.services

import com.example.R
import com.example.config.AppConfig
import com.example.models.AdminModuleCardModel
import com.example.models.AnnouncementCategory
import com.example.models.AppUpdateInfo
import com.example.models.AttendanceRecord
import com.example.models.DataSourceType
import com.example.models.FoodCategory
import com.example.models.FoodLogHistoryItem
import com.example.models.GymAnnouncement
import com.example.models.HealthyFoodItem
import com.example.models.LeaderboardCategory
import com.example.models.LeaderboardEntry
import com.example.models.MemberProfile
import com.example.models.MembershipCycle
import com.example.models.MembershipOverview
import com.example.models.MembershipStatus
import com.example.models.NotificationPreferences
import com.example.models.NutritionChartPeriod
import com.example.models.NutritionChartPoint
import com.example.models.NutritionReminderSettings
import com.example.models.PaymentMethod
import com.example.models.PaymentRecord
import com.example.models.PersonalStats
import com.example.models.UpdateAvailabilityState
import com.example.models.UserRole

/**
 * Baseline fallback dataset tailored for a commercial 200+ member gym.
 */
object SampleGymData {

    val currentMember = MemberProfile(
        id = "mem_1042",
        memberCode = "APX-0142",
        fullName = "Jordan Vance",
        phone = "+1 (555) 284-9104",
        email = "jordan.vance@apexpulse.fit",
        avatarUri = null,
        role = UserRole.MEMBER,
        joinedDate = "June 4, 2025",
        fitnessGoal = "Lean Strength & 140g Daily Protein",
        stats = PersonalStats(
            currentStreakDays = 14,
            longestStreakDays = 26,
            totalWorkouts = 118,
            monthlyWorkouts = 13,
            weeklyTargetMetWeeks = 7,
            referralsCount = 3,
            leaderboardRank = 4,
            totalGymMembers = 214
        )
    )

    // Cycle follows actual payment date (Nov 4 -> Dec 4)
    val membershipOverview = MembershipOverview(
        planName = "Apex All-Access Pro",
        monthlyFeeFormatted = "$69.00 / month",
        membershipStartDate = "June 4, 2025",
        currentCycle = MembershipCycle(
            cycleLabel = "Nov 4 – Dec 4, 2026",
            startDate = "Nov 4, 2026",
            endDate = "Dec 4, 2026",
            daysRemaining = 18,
            totalDaysInCycle = 30
        ),
        nextRenewalDate = "Dec 4, 2026",
        status = MembershipStatus.ACTIVE,
        autoRenewEnabled = true,
        paymentHistory = listOf(
            PaymentRecord(
                id = "pay_06",
                paymentDate = "Nov 4, 2026",
                cycleCovered = "Nov 4 – Dec 4, 2026",
                amountFormatted = "$69.00",
                method = PaymentMethod.CARD,
                referenceNumber = "TXN-894120",
                isVerified = true
            ),
            PaymentRecord(
                id = "pay_05",
                paymentDate = "Oct 4, 2026",
                cycleCovered = "Oct 4 – Nov 4, 2026",
                amountFormatted = "$69.00",
                method = PaymentMethod.CARD,
                referenceNumber = "TXN-851904",
                isVerified = true
            ),
            PaymentRecord(
                id = "pay_04",
                paymentDate = "Sep 4, 2026",
                cycleCovered = "Sep 4 – Oct 4, 2026",
                amountFormatted = "$69.00",
                method = PaymentMethod.UPI_INSTANT,
                referenceNumber = "TXN-812033",
                isVerified = true
            ),
            PaymentRecord(
                id = "pay_03",
                paymentDate = "Aug 4, 2026",
                cycleCovered = "Aug 4 – Sep 4, 2026",
                amountFormatted = "$69.00",
                method = PaymentMethod.CARD,
                referenceNumber = "TXN-775491",
                isVerified = true
            ),
            PaymentRecord(
                id = "pay_02",
                paymentDate = "Jul 4, 2026",
                cycleCovered = "Jul 4 – Aug 4, 2026",
                amountFormatted = "$69.00",
                method = PaymentMethod.FRONT_DESK,
                referenceNumber = "TXN-739218",
                isVerified = true
            )
        )
    )

    val recentAttendance = listOf(
        AttendanceRecord("att_1", "Nov 15, 2026", "06:42 AM", "Main Entrance QR", true),
        AttendanceRecord("att_2", "Nov 14, 2026", "07:05 AM", "Main Entrance QR", true),
        AttendanceRecord("att_3", "Nov 13, 2026", "06:18 PM", "Main Entrance QR", true),
        AttendanceRecord("att_4", "Nov 12, 2026", "06:50 AM", "Main Entrance QR", true),
        AttendanceRecord("att_5", "Nov 11, 2026", "07:12 AM", "Main Entrance QR", true),
        AttendanceRecord("att_6", "Nov 10, 2026", "05:55 PM", "Main Entrance QR", true),
        AttendanceRecord("att_7", "Nov 9, 2026", "08:20 AM", "Main Entrance QR", true)
    )

    val healthyFoods = listOf(
        HealthyFoodItem("f_eggs", "Eggs", "3 whole boiled / scrambled", 18, 210, FoodCategory.PROTEIN, "eggs", true, "07:45 AM", DataSourceType.LOGGED),
        HealthyFoodItem("f_chicken", "Chicken Breast", "180g grilled fillet", 42, 265, FoodCategory.PROTEIN, "chicken", true, "01:15 PM", DataSourceType.LOGGED),
        HealthyFoodItem("f_milk", "Milk", "300ml low-fat fortified", 11, 130, FoodCategory.DAIRY, "milk", true, "08:10 AM", DataSourceType.LOGGED),
        HealthyFoodItem("f_yogurt", "Greek Yogurt", "200g plain high-protein", 20, 145, FoodCategory.DAIRY, "yogurt", false, null, DataSourceType.MISSING),
        HealthyFoodItem("f_fish", "Salmon / Fish", "160g baked fillet", 34, 290, FoodCategory.PROTEIN, "fish", false, null, DataSourceType.MISSING),
        HealthyFoodItem("f_almonds", "Almonds", "30g raw roasted handful", 6, 175, FoodCategory.WHOLE_GRAIN, "almonds", true, "10:30 AM", DataSourceType.LOGGED),
        HealthyFoodItem("f_oats", "Rolled Oats", "60g overnight oats bowl", 8, 225, FoodCategory.WHOLE_GRAIN, "oats", true, "07:50 AM", DataSourceType.LOGGED),
        HealthyFoodItem("f_fruits", "Fresh Fruits", "Banana & mixed berries", 2, 120, FoodCategory.PRODUCE, "fruits", false, null, DataSourceType.MISSING),
        HealthyFoodItem("f_veg", "Green Vegetables", "Broccoli, spinach & peppers", 5, 85, FoodCategory.PRODUCE, "vegetables", true, "01:20 PM", DataSourceType.LOGGED),
        HealthyFoodItem("f_whey", "Whey / Plant Shake", "1 scoop post-workout", 24, 120, FoodCategory.PROTEIN, "shake", false, null, DataSourceType.MISSING)
    )

    fun getNutritionChart(period: NutritionChartPeriod): List<NutritionChartPoint> = when (period) {
        NutritionChartPeriod.DAILY -> listOf(
            NutritionChartPoint("6AM", 0, 140, DataSourceType.MISSING),
            NutritionChartPoint("8AM", 37, 140, DataSourceType.LOGGED),
            NutritionChartPoint("11AM", 43, 140, DataSourceType.LOGGED),
            NutritionChartPoint("2PM", 90, 140, DataSourceType.LOGGED),
            NutritionChartPoint("6PM", 114, 140, DataSourceType.ESTIMATED),
            NutritionChartPoint("9PM", 134, 140, DataSourceType.ESTIMATED)
        )
        NutritionChartPeriod.WEEKLY -> listOf(
            NutritionChartPoint("Mon", 142, 140, DataSourceType.LOGGED),
            NutritionChartPoint("Tue", 136, 140, DataSourceType.LOGGED),
            NutritionChartPoint("Wed", 118, 140, DataSourceType.ESTIMATED),
            NutritionChartPoint("Thu", 148, 140, DataSourceType.LOGGED),
            NutritionChartPoint("Fri", 0, 140, DataSourceType.MISSING),
            NutritionChartPoint("Sat", 144, 140, DataSourceType.LOGGED),
            NutritionChartPoint("Today", 90, 140, DataSourceType.LOGGED)
        )
        NutritionChartPeriod.MONTHLY -> listOf(
            NutritionChartPoint("Wk 1", 128, 140, DataSourceType.LOGGED),
            NutritionChartPoint("Wk 2", 139, 140, DataSourceType.LOGGED),
            NutritionChartPoint("Wk 3", 112, 140, DataSourceType.ESTIMATED),
            NutritionChartPoint("Wk 4", 134, 140, DataSourceType.LOGGED)
        )
    }

    val foodLogHistory = listOf(
        FoodLogHistoryItem("h_1", "Yesterday • Nov 15", 8, 10, 144, 140, DataSourceType.LOGGED),
        FoodLogHistoryItem("h_2", "Nov 14", 7, 10, 136, 140, DataSourceType.LOGGED),
        FoodLogHistoryItem("h_3", "Nov 13", 5, 10, 118, 140, DataSourceType.ESTIMATED),
        FoodLogHistoryItem("h_4", "Nov 12", 0, 10, 0, 140, DataSourceType.MISSING),
        FoodLogHistoryItem("h_5", "Nov 11", 9, 10, 148, 140, DataSourceType.LOGGED)
    )

    val defaultReminderSettings = NutritionReminderSettings()
    val defaultNotificationPreferences = NotificationPreferences()

    fun getLeaderboardEntries(category: LeaderboardCategory): List<LeaderboardEntry> = when (category) {
        LeaderboardCategory.ATTENDANCE_STREAK -> listOf(
            LeaderboardEntry(1, "m_1", "Marcus Sterling", null, 34, "34d streak", "Best: 48d • Monthly Winner", isMonthlyWinner = true),
            LeaderboardEntry(2, "m_2", "Elena Rostova", null, 29, "29d streak", "Best: 31d • 22 visits/mo"),
            LeaderboardEntry(3, "m_3", "Devon Brooks", null, 21, "21d streak", "Best: 25d • 19 visits/mo"),
            LeaderboardEntry(4, "mem_1042", "Jordan Vance", null, 14, "14d streak", "Best: 26d • You", isCurrentUser = true),
            LeaderboardEntry(5, "m_5", "Priya Nair", null, 12, "12d streak", "Best: 19d • 15 visits/mo"),
            LeaderboardEntry(6, "m_6", "Lucas Thorne", null, 10, "10d streak", "Best: 22d • 14 visits/mo"),
            LeaderboardEntry(7, "m_7", "Amina Hassan", null, 9, "9d streak", "Best: 16d • 12 visits/mo"),
            LeaderboardEntry(8, "m_8", "Noah Bennett", null, 7, "7d streak", "Best: 14d • 11 visits/mo")
        )
        LeaderboardCategory.HEALTHY_HABITS_RACE -> listOf(
            LeaderboardEntry(1, "m_2", "Elena Rostova", null, 98, "98% goal", "145g avg protein • 28d logged", isMonthlyWinner = true),
            LeaderboardEntry(2, "m_5", "Priya Nair", null, 94, "94% goal", "138g avg protein • 25d logged"),
            LeaderboardEntry(3, "m_9", "Tariq Al-Mansoor", null, 91, "91% goal", "152g avg protein • 24d logged"),
            LeaderboardEntry(4, "mem_1042", "Jordan Vance", null, 86, "86% goal", "134g avg protein • You", isCurrentUser = true),
            LeaderboardEntry(5, "m_1", "Marcus Sterling", null, 82, "82% goal", "160g avg protein • 21d logged"),
            LeaderboardEntry(6, "m_10", "Chloe Kim", null, 79, "79% goal", "118g avg protein • 20d logged"),
            LeaderboardEntry(7, "m_3", "Devon Brooks", null, 75, "75% goal", "129g avg protein • 18d logged")
        )
        LeaderboardCategory.CONSISTENCY_CHAMPION -> listOf(
            LeaderboardEntry(1, "m_1", "Marcus Sterling", null, 16, "16 wks", "4.8 visits/wk avg • 3+/wk target", isMonthlyWinner = true),
            LeaderboardEntry(2, "m_6", "Lucas Thorne", null, 12, "12 wks", "4.1 visits/wk avg • 3+/wk target"),
            LeaderboardEntry(3, "m_2", "Elena Rostova", null, 11, "11 wks", "3.9 visits/wk avg • 3+/wk target"),
            LeaderboardEntry(4, "m_7", "Amina Hassan", null, 9, "9 wks", "3.6 visits/wk avg • 3+/wk target"),
            LeaderboardEntry(5, "mem_1042", "Jordan Vance", null, 7, "7 wks", "3.5 visits/wk avg • You", isCurrentUser = true),
            LeaderboardEntry(6, "m_11", "Soren Lindqvist", null, 6, "6 wks", "3.2 visits/wk avg • 3+/wk target")
        )
        LeaderboardCategory.REFERRAL_CHAMPION -> listOf(
            LeaderboardEntry(1, "m_3", "Devon Brooks", null, 8, "8 referrals", "8 verified active members", isMonthlyWinner = true),
            LeaderboardEntry(2, "m_10", "Chloe Kim", null, 6, "6 referrals", "6 verified active members"),
            LeaderboardEntry(3, "m_5", "Priya Nair", null, 5, "5 referrals", "5 verified active members"),
            LeaderboardEntry(4, "m_1", "Marcus Sterling", null, 4, "4 referrals", "4 verified active members"),
            LeaderboardEntry(5, "mem_1042", "Jordan Vance", null, 3, "3 referrals", "3 verified active members • You", isCurrentUser = true),
            LeaderboardEntry(6, "m_8", "Noah Bennett", null, 2, "2 referrals", "2 verified active members")
        )
    }

    val announcements = listOf(
        GymAnnouncement(
            id = "ann_1",
            title = "Winter Iron Kettlebell Classic — Dec 6",
            description = "Registration is now open for our 200-member club endurance and kettlebell strength showdown. Top 3 finishers in each division win 2 months of Apex All-Access Pro and custom gear.",
            dateLabel = "Nov 16, 2026",
            category = AnnouncementCategory.COMPETITION,
            isImportant = true,
            bannerDrawableRes = R.drawable.img_gym_event_competition,
            ctaLabel = "Event Details at Front Desk"
        ),
        GymAnnouncement(
            id = "ann_2",
            title = "New Olympic Lifting Platforms & Recovery Zone Open",
            description = "Floor B has been upgraded with four competition-grade squat racks, calibrated plates, and cold-plunge recovery suites. Remember to scan the main entrance QR on arrival.",
            dateLabel = "Nov 13, 2026",
            category = AnnouncementCategory.EVENT,
            isImportant = false,
            bannerDrawableRes = R.drawable.img_gym_hero_banner,
            ctaLabel = "Explore Floor Hours"
        ),
        GymAnnouncement(
            id = "ann_3",
            title = "Holiday Maintenance & Early Closing Notice",
            description = "ApexPulse Performance Club will operate on modified hours (6:00 AM – 4:00 PM) on Nov 26 for annual turf sanitization and cable calibration.",
            dateLabel = "Nov 10, 2026",
            category = AnnouncementCategory.IMPORTANT,
            isImportant = true,
            bannerDrawableRes = null,
            ctaLabel = null
        ),
        GymAnnouncement(
            id = "ann_4",
            title = "30-Day Protein & Healthy Habits Challenge Winners",
            description = "Congratulations to Elena Rostova and all 42 members who hit 85%+ of their daily whole-food nutrition goals last cycle! Check the Healthy Habits Race leaderboard.",
            dateLabel = "Nov 5, 2026",
            category = AnnouncementCategory.COMPETITION,
            isImportant = false,
            bannerDrawableRes = null,
            ctaLabel = null
        )
    )

    val appUpdateInfo = AppUpdateInfo(
        installedVersion = AppConfig.INSTALLED_VERSION,
        latestVersion = "1.2.0",
        releaseDate = "Nov 14, 2026",
        availabilityState = UpdateAvailabilityState.UPDATE_AVAILABLE,
        releaseHighlights = listOf(
            "Faster camera autofocus for the common entrance QR code scanner",
            "Expanded protein breakdown & weekly nutrition trend charts",
            "Refined dark/light athletic contrast for gym floor visibility"
        )
    )

    val reservedAdminModules = listOf(
        AdminModuleCardModel("adm_members", "Member Directory (214 Active)", "Manage member profiles, roles, and freeze requests"),
        AdminModuleCardModel("adm_qr", "Common Entrance QR Terminal", "Generate & rotate the physical reception entrance QR code"),
        AdminModuleCardModel("adm_payments", "Payment Verification & Cycles", "Approve front-desk / digital renewals & adjust cycle dates"),
        AdminModuleCardModel("adm_broadcast", "Announcements & Challenges", "Publish pinned alerts, gym competitions, and push notifications")
    )
}
