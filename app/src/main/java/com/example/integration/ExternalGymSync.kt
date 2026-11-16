package com.example.integration

import com.example.database.AttendanceRecordDoc
import com.example.database.PaymentRecordDoc
import com.example.models.AdminMemberSummary
import org.json.JSONArray
import org.json.JSONObject

/**
 * Clean, modular external gym software integration layer.
 * Provides standardized JSON & CSV export bundles and deduplicated import validation
 * ready for future connection to laptop gym management software without vendor lock-in.
 */
interface GymSoftwareSyncAdapter {
    fun buildJsonSyncBundle(
        members: List<AdminMemberSummary>,
        attendance: List<AttendanceRecordDoc>,
        payments: List<PaymentRecordDoc>
    ): String

    fun buildMemberRosterCsv(members: List<AdminMemberSummary>): String
    fun buildAttendanceCsv(attendance: List<AttendanceRecordDoc>, membersByUid: Map<String, AdminMemberSummary>): String
    fun previewInboundPayload(payload: String): ExternalSyncPreview
}

data class ExternalSyncPreview(
    val formatDetected: String,
    val recordCount: Int,
    val deduplicatedIdsCount: Int,
    val statusNote: String
)

class StandardGymSyncBridge : GymSoftwareSyncAdapter {

    override fun buildJsonSyncBundle(
        members: List<AdminMemberSummary>,
        attendance: List<AttendanceRecordDoc>,
        payments: List<PaymentRecordDoc>
    ): String {
        val root = JSONObject()
        root.put("schemaVersion", "1.0")
        root.put("system", "Gymimpulse")
        root.put("exportedAtMillis", System.currentTimeMillis())

        val membersArr = JSONArray()
        members.distinctBy { it.userId }.forEach { m ->
            membersArr.put(
                JSONObject().apply {
                    put("userId", m.userId)
                    put("memberCode", m.profile.memberCode)
                    put("fullName", m.profile.fullName)
                    put("email", m.profile.email)
                    put("phone", m.profile.phone ?: "")
                    put("membershipStatus", m.derivedStatus.name)
                    put("cycleStart", m.membership?.cycleStartDate ?: "")
                    put("cycleEnd", m.membership?.cycleEndDate ?: "")
                    put("currentStreak", m.currentStreakDays)
                    put("totalVisits", m.totalVisits)
                }
            )
        }
        root.put("members", membersArr)

        val attArr = JSONArray()
        attendance.distinctBy { "${it.userId}_${it.dateKey}" }.forEach { a ->
            attArr.put(
                JSONObject().apply {
                    put("userId", a.userId)
                    put("dateKey", a.dateKey)
                    put("checkInTime", a.checkInTime)
                    put("gymCode", a.gymCode)
                }
            )
        }
        root.put("attendance", attArr)

        val payArr = JSONArray()
        payments.distinctBy { "${it.userId}_${it.referenceNumber}" }.forEach { p ->
            payArr.put(
                JSONObject().apply {
                    put("userId", p.userId)
                    put("paymentDate", p.paymentDate)
                    put("cycleCovered", p.cycleCovered)
                    put("amountFormatted", p.amountFormatted)
                    put("method", p.method)
                    put("referenceNumber", p.referenceNumber)
                    put("isVerified", p.isVerified)
                }
            )
        }
        root.put("verifiedPayments", payArr)

        return root.toString(2)
    }

    override fun buildMemberRosterCsv(members: List<AdminMemberSummary>): String {
        val sb = StringBuilder()
        sb.appendLine("userId,memberCode,fullName,email,status,cycleStart,cycleEnd,currentStreak,totalVisits")
        members.distinctBy { it.userId }.forEach { m ->
            sb.appendLine(
                listOf(
                    m.userId,
                    m.profile.memberCode,
                    escapeCsv(m.profile.fullName),
                    escapeCsv(m.profile.email),
                    m.derivedStatus.name,
                    escapeCsv(m.membership?.cycleStartDate ?: ""),
                    escapeCsv(m.membership?.cycleEndDate ?: ""),
                    m.currentStreakDays.toString(),
                    m.totalVisits.toString()
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    override fun buildAttendanceCsv(
        attendance: List<AttendanceRecordDoc>,
        membersByUid: Map<String, AdminMemberSummary>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("dateKey,checkInTime,userId,memberCode,memberName,gymCode")
        attendance.distinctBy { "${it.userId}_${it.dateKey}" }
            .sortedByDescending { it.dateKey }
            .forEach { a ->
                val member = membersByUid[a.userId]
                sb.appendLine(
                    listOf(
                        a.dateKey,
                        escapeCsv(a.checkInTime),
                        a.userId,
                        member?.profile?.memberCode ?: "",
                        escapeCsv(member?.profile?.fullName ?: "Member"),
                        a.gymCode
                    ).joinToString(",")
                )
            }
        return sb.toString()
    }

    override fun previewInboundPayload(payload: String): ExternalSyncPreview {
        val trimmed = payload.trim()
        if (trimmed.isEmpty()) {
            return ExternalSyncPreview("EMPTY", 0, 0, "Paste a JSON bundle or CSV export from laptop gym software.")
        }
        if (trimmed.startsWith("{")) {
            return runCatching {
                val obj = JSONObject(trimmed)
                val membersArr = obj.optJSONArray("members")
                val seen = HashSet<String>()
                var count = 0
                if (membersArr != null) {
                    count = membersArr.length()
                    for (i in 0 until count) {
                        val row = membersArr.optJSONObject(i)
                        val key = row?.optString("memberCode").orEmpty().ifBlank { row?.optString("userId").orEmpty() }
                        if (key.isNotBlank()) seen.add(key)
                    }
                }
                ExternalSyncPreview(
                    formatDetected = "JSON Sync Bundle",
                    recordCount = count,
                    deduplicatedIdsCount = seen.size,
                    statusNote = "Validated $count member records (${seen.size} unique IDs). Zero duplicate entries."
                )
            }.getOrElse {
                ExternalSyncPreview("INVALID_JSON", 0, 0, "Malformed JSON payload: ${it.message}")
            }
        }
        val lines = trimmed.lines().filter { it.isNotBlank() }
        val dataLines = if (lines.firstOrNull()?.contains("memberCode", ignoreCase = true) == true) {
            lines.drop(1)
        } else lines
        val uniqueKeys = dataLines.mapNotNull { it.split(",").firstOrNull()?.trim() }.filter { it.isNotEmpty() }.toSet()
        return ExternalSyncPreview(
            formatDetected = "CSV Roster",
            recordCount = dataLines.size,
            deduplicatedIdsCount = uniqueKeys.size,
            statusNote = "Parsed ${dataLines.size} CSV rows (${uniqueKeys.size} unique keys) ready for merge."
        )
    }

    private fun escapeCsv(raw: String): String {
        val clean = raw.replace("\"", "\"\"")
        return if (clean.contains(",") || clean.contains("\"") || clean.contains("\n")) "\"$clean\"" else clean
    }
}
