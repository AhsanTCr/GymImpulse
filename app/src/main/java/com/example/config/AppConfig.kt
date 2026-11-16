package com.example.config

/**
 * Central application configuration constants for GymPulse.
 * Phase 2 connects Cloud Firestore & Google Sign-In while reserving Phase 3 admin portal flags.
 */
object AppConfig {
    const val APP_NAME = "GymPulse"
    const val GYM_NAME = "GymPulse Performance Club"
    const val GYM_LOCATION = "Downtown Athletic District"
    const val INSTALLED_VERSION = "1.0.0"
    const val INSTALLED_BUILD_NUMBER = 100

    // Single physical entrance QR code identifier displayed at the gym reception
    const val COMMON_ENTRANCE_QR_CODE = "APEXPULSE-MAIN-ENTRANCE-QR-001"
    const val QR_ROTATION_WINDOW_SECONDS = 120L

    // Default club coordinates for optional coarse-location anti-fraud check
    const val DEFAULT_GYM_LATITUDE = 37.7749
    const val DEFAULT_GYM_LONGITUDE = -122.4194
    const val DEFAULT_ALLOWED_RADIUS_METERS = 500.0

    // Default daily member nutrition & attendance goals
    const val DEFAULT_PROTEIN_GOAL_GRAMS = 140
    const val DEFAULT_FOOD_TARGET_COUNT = 7
    const val DEFAULT_WEEKLY_ATTENDANCE_TARGET = 3
    const val DEFAULT_CONSISTENCY_WEEKS_BADGE = 4

    // Default plan metadata for starter membership cycles
    const val DEFAULT_PLAN_NAME = "GymPulse All-Access Pro"
    const val DEFAULT_MONTHLY_FEE_USD = 69.0

    // Feature flags
    const val ENABLE_PHASE_2_CLOUD_SYNC = true
    const val ENABLE_PHASE_3_ADMIN_PORTAL = false
}
