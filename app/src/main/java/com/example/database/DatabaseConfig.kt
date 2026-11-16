package com.example.database

/**
 * Reserved database configuration and schema contract for Phase 2 implementation.
 * Will back Room local cache and Cloud Firestore / backend sync in Phase 2.
 */
object DatabaseConfig {
    const val LOCAL_DB_NAME = "apexpulse_gym.db"
    const val LOCAL_DB_VERSION = 1

    object Collections {
        const val MEMBERS = "members"
        const val MEMBERSHIPS = "memberships"
        const val PAYMENTS = "payments"
        const val ATTENDANCE_LOGS = "attendance_logs"
        const val NUTRITION_LOGS = "nutrition_logs"
        const val ANNOUNCEMENTS = "announcements"
        const val LEADERBOARD_SNAPSHOTS = "leaderboard_snapshots"
        const val GYM_QR_CONFIG = "gym_qr_config"
    }
}

/**
 * Contract for Phase 2 repository wiring and offline-first sync orchestration.
 */
interface GymDatabaseContract {
    suspend fun initializeDatabase()
    suspend fun clearLocalSessionCache()
}
