package com.example.database

/**
 * Database collection definitions and contract constants for Gymimpulse.
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
 * Contract for repository wiring and session cache operations.
 */
interface GymDatabaseContract {
    suspend fun initializeDatabase()
    suspend fun clearLocalSessionCache()
}
