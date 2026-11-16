package com.example.services

import com.example.models.AttendanceRecord
import com.example.models.TodayAttendanceState

interface AttendanceService {
    fun getTodayAttendanceState(): TodayAttendanceState
    fun getRecentAttendance(): List<AttendanceRecord>
    fun recordCommonQrCheckIn(memberName: String, entranceCode: String): AttendanceRecord
}

class MockAttendanceService : AttendanceService {
    private var todayState = TodayAttendanceState.NOT_CHECKED_IN
    private val records = SampleGymData.recentAttendance.toMutableList()

    override fun getTodayAttendanceState(): TodayAttendanceState = todayState

    override fun getRecentAttendance(): List<AttendanceRecord> = records.toList()

    override fun recordCommonQrCheckIn(memberName: String, entranceCode: String): AttendanceRecord {
        val newRecord = AttendanceRecord(
            id = "att_today_${System.currentTimeMillis()}",
            dateLabel = "Today • Nov 16, 2026",
            checkInTime = "11:15 AM",
            zoneLabel = "Main Entrance QR ($entranceCode)",
            verifiedViaCommonQr = true
        )
        if (todayState == TodayAttendanceState.NOT_CHECKED_IN) {
            records.add(0, newRecord)
            todayState = TodayAttendanceState.CHECKED_IN
        }
        return records.first()
    }
}
