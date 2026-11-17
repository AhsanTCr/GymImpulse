package com.example.config

/**
 * Central application configuration constants for Gymimpulse.
 * Manages role authorization, entrance QR window settings, and gym defaults.
 */
object AppConfig {
    const val APP_NAME = "Gymimpulse"
    const val GYM_NAME = "Gymimpulse Performance Club"
    const val GYM_LOCATION = "Downtown Athletic District"
    val INSTALLED_VERSION: String
        get() = com.example.BuildConfig.VERSION_NAME
    val INSTALLED_BUILD_NUMBER: Int
        get() = com.example.BuildConfig.VERSION_CODE
    val DEFAULT_GITHUB_REPO: String
        get() = com.example.BuildConfig.GITHUB_REPO

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
    const val DEFAULT_PLAN_NAME = "Gymimpulse All-Access Pro"
    const val DEFAULT_MONTHLY_FEE_USD = 69.0

    // SHA-256 digest of the Gym Admin unlock passcode
    const val ADMIN_PASSCODE_SHA256 =
        "dc1ebf74473cba59c8917a0aa2829371320b9cff6f56df96fa77c21ba1a82085"

    // Feature flags
    const val ENABLE_PHASE_2_CLOUD_SYNC = true
    const val ENABLE_PHASE_3_ADMIN_PORTAL = true
}
