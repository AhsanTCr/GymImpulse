package com.example.navigation

/**
 * Unique route identifiers for every screen in ApexPulse.
 */
object AppRoutes {
    const val ROUTE_SPLASH = "splash"
    const val ROUTE_LOGIN = "login"
    const val ROUTE_REGISTER = "register"

    // Member bottom navigation tabs
    const val ROUTE_MEMBER_HOME = "member_home"
    const val ROUTE_LEADERBOARD = "leaderboard"
    const val ROUTE_QR_ATTENDANCE = "qr_attendance"
    const val ROUTE_DAILY_GOALS = "daily_goals"
    const val ROUTE_MEMBER_PROFILE = "member_profile"

    // Secondary Member screens
    const val ROUTE_MY_MEMBERSHIP = "my_membership"
    const val ROUTE_ANNOUNCEMENTS = "announcements"
    const val ROUTE_APP_UPDATES = "app_updates"

    // Reserved Phase 3 Admin module route
    const val ROUTE_ADMIN_PORTAL = "admin_portal_reserved"

    val bottomBarRoutes = setOf(
        ROUTE_MEMBER_HOME,
        ROUTE_LEADERBOARD,
        ROUTE_QR_ATTENDANCE,
        ROUTE_DAILY_GOALS,
        ROUTE_MEMBER_PROFILE
    )
}
