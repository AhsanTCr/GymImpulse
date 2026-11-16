package com.example.services

import com.example.models.AuthSession
import com.example.models.MemberProfile
import com.example.models.PersonalStats
import com.example.models.UserRole

interface AuthService {
    fun getSession(): AuthSession
    fun login(identifier: String, password: String, role: UserRole = UserRole.MEMBER): AuthSession
    fun register(
        fullName: String,
        phone: String,
        email: String,
        avatarUri: String?
    ): AuthSession
    fun updateAvatar(avatarUri: String?)
    fun logout(): AuthSession
}

class MockAuthService : AuthService {
    private var currentSession = AuthSession(
        isLoggedIn = false,
        activeRole = UserRole.MEMBER,
        currentMember = SampleGymData.currentMember
    )

    override fun getSession(): AuthSession = currentSession

    override fun login(identifier: String, password: String, role: UserRole): AuthSession {
        val base = SampleGymData.currentMember
        val updatedMember = if (identifier.contains("@")) {
            base.copy(email = identifier.trim(), role = role)
        } else if (identifier.isNotBlank()) {
            base.copy(phone = identifier.trim(), role = role)
        } else {
            base.copy(role = role)
        }
        currentSession = AuthSession(
            isLoggedIn = true,
            activeRole = role,
            currentMember = updatedMember
        )
        return currentSession
    }

    override fun register(
        fullName: String,
        phone: String,
        email: String,
        avatarUri: String?
    ): AuthSession {
        val newMember = MemberProfile(
            id = "mem_new_${System.currentTimeMillis() % 10000}",
            memberCode = "APX-0215",
            fullName = fullName.ifBlank { "New Member" },
            phone = phone.ifBlank { "+1 (555) 010-2026" },
            email = email.ifBlank { "member@apexpulse.fit" },
            avatarUri = avatarUri,
            role = UserRole.MEMBER,
            joinedDate = "Nov 16, 2026",
            fitnessGoal = "Strength & Daily Consistency",
            stats = PersonalStats(
                currentStreakDays = 1,
                longestStreakDays = 1,
                totalWorkouts = 1,
                monthlyWorkouts = 1,
                weeklyTargetMetWeeks = 1,
                referralsCount = 0,
                leaderboardRank = 185,
                totalGymMembers = 215
            )
        )
        currentSession = AuthSession(
            isLoggedIn = true,
            activeRole = UserRole.MEMBER,
            currentMember = newMember
        )
        return currentSession
    }

    override fun updateAvatar(avatarUri: String?) {
        currentSession = currentSession.copy(
            currentMember = currentSession.currentMember?.copy(avatarUri = avatarUri)
        )
    }

    override fun logout(): AuthSession {
        currentSession = currentSession.copy(isLoggedIn = false)
        return currentSession
    }
}
