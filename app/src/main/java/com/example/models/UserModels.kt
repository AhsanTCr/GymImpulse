package com.example.models

enum class UserRole(val displayName: String) {
    MEMBER("Gym Member"),
    ADMIN("Gym Admin")
}

data class PersonalStats(
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val totalWorkouts: Int,
    val monthlyWorkouts: Int,
    val weeklyTargetMetWeeks: Int,
    val referralsCount: Int,
    val leaderboardRank: Int,
    val totalGymMembers: Int
)

data class MemberProfile(
    val id: String,
    val memberCode: String,
    val fullName: String,
    val phone: String,
    val email: String,
    val avatarUri: String? = null,
    val role: UserRole = UserRole.MEMBER,
    val joinedDate: String,
    val fitnessGoal: String,
    val stats: PersonalStats
)

data class AuthSession(
    val isLoggedIn: Boolean,
    val activeRole: UserRole,
    val currentMember: MemberProfile?
)
