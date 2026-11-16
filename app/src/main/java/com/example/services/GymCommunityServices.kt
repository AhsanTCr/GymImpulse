package com.example.services

import com.example.models.AppUpdateInfo
import com.example.models.GymAnnouncement
import com.example.models.LeaderboardCategory
import com.example.models.LeaderboardEntry

interface LeaderboardService {
    fun getEntries(category: LeaderboardCategory): List<LeaderboardEntry>
}

class MockLeaderboardService : LeaderboardService {
    override fun getEntries(category: LeaderboardCategory): List<LeaderboardEntry> =
        SampleGymData.getLeaderboardEntries(category)
}

interface AnnouncementService {
    fun getAnnouncements(): List<GymAnnouncement>
}

class MockAnnouncementService : AnnouncementService {
    override fun getAnnouncements(): List<GymAnnouncement> = SampleGymData.announcements
}

interface AppUpdateService {
    fun getCurrentUpdateInfo(): AppUpdateInfo
}

class MockAppUpdateService : AppUpdateService {
    override fun getCurrentUpdateInfo(): AppUpdateInfo = SampleGymData.appUpdateInfo
}

/**
 * Central service locator for Phase 1 UI architecture.
 * Ready to be swapped with DI/Room/Cloud implementations in Phase 2.
 */
object ServiceRegistry {
    val authService: AuthService = MockAuthService()
    val attendanceService: AttendanceService = MockAttendanceService()
    val membershipService: MembershipService = MockMembershipService()
    val nutritionService: NutritionService = MockNutritionService()
    val leaderboardService: LeaderboardService = MockLeaderboardService()
    val announcementService: AnnouncementService = MockAnnouncementService()
    val appUpdateService: AppUpdateService = MockAppUpdateService()
}
