package com.example.services

import com.example.models.MembershipOverview
import com.example.models.MembershipStatus

interface MembershipService {
    fun getMembershipOverview(): MembershipOverview
    fun previewStatus(status: MembershipStatus): MembershipOverview
}

class MockMembershipService : MembershipService {
    private var current = SampleGymData.membershipOverview

    override fun getMembershipOverview(): MembershipOverview = current

    override fun previewStatus(status: MembershipStatus): MembershipOverview {
        current = current.copy(status = status)
        return current
    }
}
