package com.example.models

enum class TodayAttendanceState(val label: String) {
    CHECKED_IN("Checked In Today"),
    NOT_CHECKED_IN("Not Checked In Yet")
}

sealed class QrScanUiState {
    data object ReadyToScan : QrScanUiState()
    data object ScanningEntranceQr : QrScanUiState()
    data class Confirmed(
        val memberName: String,
        val memberCode: String,
        val timestamp: String,
        val entranceCode: String,
        val updatedStreakDays: Int
    ) : QrScanUiState()
    data class Error(val message: String) : QrScanUiState()
}

data class AttendanceRecord(
    val id: String,
    val dateLabel: String,
    val checkInTime: String,
    val zoneLabel: String,
    val verifiedViaCommonQr: Boolean = true
)
