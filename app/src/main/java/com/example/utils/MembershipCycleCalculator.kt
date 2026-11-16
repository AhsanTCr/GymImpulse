package com.example.utils

import com.example.config.AppConfig
import com.example.database.MembershipRecordDoc
import com.example.database.PaymentRecordDoc
import com.example.models.MembershipCycle
import com.example.models.MembershipOverview
import com.example.models.MembershipStatus
import com.example.models.PaymentMethod
import com.example.models.PaymentRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Computes payment-date-anchored membership cycles and derives real-time membership statuses.
 */
object MembershipCycleCalculator {

    private val shortDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
    private val monthDayFormat = SimpleDateFormat("MMM d", Locale.US)

    fun parseStatus(raw: String, cycleEndDate: String, nowMillis: Long = System.currentTimeMillis()): MembershipStatus {
        val base = runCatching { MembershipStatus.valueOf(raw) }.getOrDefault(MembershipStatus.EXPIRED)
        if (base == MembershipStatus.PAYMENT_OVERDUE || base == MembershipStatus.EXPIRED) {
            return base
        }
        val endDate = runCatching { shortDateFormat.parse(cycleEndDate) }.getOrNull() ?: return base
        val diffDays = TimeUnit.MILLISECONDS.toDays(endDate.time - nowMillis).toInt()
        return when {
            diffDays < -3 -> MembershipStatus.PAYMENT_OVERDUE
            diffDays < 0 -> MembershipStatus.EXPIRED
            diffDays <= 5 -> MembershipStatus.EXPIRING_SOON
            else -> MembershipStatus.ACTIVE
        }
    }

    fun toOverview(
        record: MembershipRecordDoc?,
        payments: List<PaymentRecordDoc>,
        nowMillis: Long = System.currentTimeMillis()
    ): MembershipOverview {
        val today = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val todayLabel = shortDateFormat.format(today.time)

        val effectiveRecord = record ?: MembershipRecordDoc(
            planName = AppConfig.DEFAULT_PLAN_NAME,
            monthlyFeeUsd = AppConfig.DEFAULT_MONTHLY_FEE_USD,
            membershipStartDate = todayLabel,
            cycleStartDate = todayLabel,
            cycleEndDate = todayLabel,
            nextRenewalDate = todayLabel,
            status = "EXPIRED",
            autoRenewEnabled = false
        )

        val startMillis = runCatching { shortDateFormat.parse(effectiveRecord.cycleStartDate)?.time }
            .getOrNull() ?: nowMillis
        val endMillis = runCatching { shortDateFormat.parse(effectiveRecord.cycleEndDate)?.time }
            .getOrNull() ?: nowMillis

        val totalDays = TimeUnit.MILLISECONDS.toDays((endMillis - startMillis).coerceAtLeast(1L)).toInt().coerceAtLeast(1)
        val daysRemaining = TimeUnit.MILLISECONDS.toDays(endMillis - nowMillis).toInt().coerceAtLeast(0)

        val cycleLabel = runCatching {
            val s = shortDateFormat.parse(effectiveRecord.cycleStartDate)
            val e = shortDateFormat.parse(effectiveRecord.cycleEndDate)
            if (s != null && e != null) "${monthDayFormat.format(s)} – ${shortDateFormat.format(e)}"
            else "${effectiveRecord.cycleStartDate} – ${effectiveRecord.cycleEndDate}"
        }.getOrDefault("${effectiveRecord.cycleStartDate} – ${effectiveRecord.cycleEndDate}")

        val status = parseStatus(effectiveRecord.status, effectiveRecord.cycleEndDate, nowMillis)

        val mappedPayments = payments.map { doc ->
            PaymentRecord(
                id = doc.id,
                paymentDate = doc.paymentDate,
                cycleCovered = doc.cycleCovered,
                amountFormatted = doc.amountFormatted,
                method = when {
                    doc.method.contains("UPI", ignoreCase = true) || doc.method.contains("Bank", ignoreCase = true) -> PaymentMethod.UPI_INSTANT
                    doc.method.contains("Front", ignoreCase = true) || doc.method.contains("Cash", ignoreCase = true) -> PaymentMethod.FRONT_DESK
                    else -> PaymentMethod.CARD
                },
                referenceNumber = doc.referenceNumber,
                isVerified = doc.isVerified
            )
        }

        return MembershipOverview(
            planName = effectiveRecord.planName,
            monthlyFeeFormatted = "$${String.format(Locale.US, "%.2f", effectiveRecord.monthlyFeeUsd)} / month",
            membershipStartDate = effectiveRecord.membershipStartDate,
            currentCycle = MembershipCycle(
                cycleLabel = cycleLabel,
                startDate = effectiveRecord.cycleStartDate,
                endDate = effectiveRecord.cycleEndDate,
                daysRemaining = daysRemaining,
                totalDaysInCycle = totalDays
            ),
            nextRenewalDate = effectiveRecord.nextRenewalDate,
            status = status,
            autoRenewEnabled = effectiveRecord.autoRenewEnabled,
            paymentHistory = mappedPayments
        )
    }
}
