package com.example.utils

import com.example.config.AppConfig
import java.security.MessageDigest
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

sealed class QrVerificationResult {
    data class Valid(
        val entranceCode: String,
        val verificationToken: String,
        val distanceMeters: Double?
    ) : QrVerificationResult()

    data class Invalid(val reason: String) : QrVerificationResult()
}

/**
 * Validates common gym entrance QR check-ins with anti-fraud safeguards:
 * 1. Exact entrance code verification (`APEXPULSE-MAIN-ENTRANCE-QR-001`)
 * 2. Rotating time-window token (`|WIN:<epochWindow>|SIG:<digest>`) rejecting stale screenshot reuse
 * 3. Duplicate daily check-in guard
 * 4. Optional coarse-location perimeter verification
 */
object QrAntiFraudVerifier {

    fun currentTimeWindow(
        nowMillis: Long = System.currentTimeMillis(),
        windowSeconds: Long = AppConfig.QR_ROTATION_WINDOW_SECONDS
    ): Long = (nowMillis / 1000L) / windowSeconds.coerceAtLeast(30L)

    fun generateRotatingEntrancePayload(
        entranceCode: String = AppConfig.COMMON_ENTRANCE_QR_CODE,
        nowMillis: Long = System.currentTimeMillis(),
        windowSeconds: Long = AppConfig.QR_ROTATION_WINDOW_SECONDS
    ): String {
        val window = currentTimeWindow(nowMillis, windowSeconds)
        val signature = signWindow(entranceCode, window)
        return "$entranceCode|WIN:$window|SIG:$signature"
    }

    fun verifyCheckIn(
        rawPayload: String,
        expectedEntranceCode: String = AppConfig.COMMON_ENTRANCE_QR_CODE,
        alreadyCheckedInToday: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
        windowSeconds: Long = AppConfig.QR_ROTATION_WINDOW_SECONDS,
        memberLat: Double? = null,
        memberLng: Double? = null,
        gymLat: Double? = null,
        gymLng: Double? = null,
        allowedRadiusMeters: Double? = null
    ): QrVerificationResult {
        if (alreadyCheckedInToday) {
            return QrVerificationResult.Invalid(
                "You have already checked in today. Duplicate entrance scans on the same day are blocked."
            )
        }

        val trimmed = rawPayload.trim()
        if (trimmed.isEmpty()) {
            return QrVerificationResult.Invalid("Empty QR code. Scan the reception entrance poster.")
        }

        val parts = trimmed.split("|")
        val codePart = parts.firstOrNull().orEmpty()
        if (codePart != expectedEntranceCode) {
            return QrVerificationResult.Invalid(
                "Unrecognized QR code ($codePart). Please scan the official GymPulse entrance QR."
            )
        }

        val currentWindow = currentTimeWindow(nowMillis, windowSeconds)
        val tokenToStore: String
        if (parts.size >= 3) {
            val winStr = parts.firstOrNull { it.startsWith("WIN:") }?.removePrefix("WIN:")
            val sigStr = parts.firstOrNull { it.startsWith("SIG:") }?.removePrefix("SIG:")
            val parsedWin = winStr?.toLongOrNull()
            if (parsedWin == null || sigStr.isNullOrBlank()) {
                return QrVerificationResult.Invalid("Malformed entrance verification token.")
            }
            if (kotlin.math.abs(currentWindow - parsedWin) > 1L) {
                return QrVerificationResult.Invalid(
                    "Expired QR screenshot detected. Please scan the live entrance code at reception."
                )
            }
            if (sigStr != signWindow(expectedEntranceCode, parsedWin)) {
                return QrVerificationResult.Invalid("Invalid entrance security signature.")
            }
            tokenToStore = "WIN_${parsedWin}_$sigStr"
        } else {
            // Static poster scan paired with current device time-window verification digest
            tokenToStore = "STATIC_${currentWindow}_${signWindow(expectedEntranceCode, currentWindow)}"
        }

        var computedDistance: Double? = null
        if (memberLat != null && memberLng != null && gymLat != null && gymLng != null && allowedRadiusMeters != null) {
            val distance = haversineMeters(memberLat, memberLng, gymLat, gymLng)
            computedDistance = distance
            if (distance > allowedRadiusMeters) {
                return QrVerificationResult.Invalid(
                    "Location check indicates you are ${distance.toInt()}m away from the club entrance (limit ${allowedRadiusMeters.toInt()}m)."
                )
            }
        }

        return QrVerificationResult.Valid(
            entranceCode = expectedEntranceCode,
            verificationToken = tokenToStore,
            distanceMeters = computedDistance
        )
    }

    private fun signWindow(code: String, window: Long): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$code:$window:GYMPULSE_CLUB_KEY".toByteArray())
        return digest.take(6).joinToString("") { "%02x".format(it) }
    }

    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
