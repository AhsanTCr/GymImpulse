package com.example.models

enum class MembershipStatus(val label: String, val subtitle: String) {
    ACTIVE("Active", "Full gym & leaderboard access enabled"),
    EXPIRING_SOON("Expiring Soon", "Renew before cycle end to keep streak perks"),
    EXPIRED("Expired", "Membership cycle ended — renew to check in"),
    PAYMENT_OVERDUE("Payment Overdue", "Action required to restore entrance QR check-in")
}

enum class PaymentMethod(val label: String) {
    CARD("Card •••• 4829"),
    UPI_INSTANT("Instant Bank / UPI"),
    FRONT_DESK("Front Desk Cash / POS")
}

data class MembershipCycle(
    val cycleLabel: String,
    val startDate: String,
    val endDate: String,
    val daysRemaining: Int,
    val totalDaysInCycle: Int
)

data class PaymentRecord(
    val id: String,
    val paymentDate: String,
    val cycleCovered: String,
    val amountFormatted: String,
    val method: PaymentMethod,
    val referenceNumber: String,
    val isVerified: Boolean
)

data class PaymentRequestItem(
    val id: String,
    val amountFormatted: String,
    val paymentMethod: String,
    val referenceNote: String?,
    val paymentDateLabel: String,
    val status: String,
    val isVerified: Boolean
)

data class MembershipOverview(
    val planName: String,
    val monthlyFeeFormatted: String,
    val membershipStartDate: String,
    val currentCycle: MembershipCycle,
    val nextRenewalDate: String,
    val status: MembershipStatus,
    val autoRenewEnabled: Boolean,
    val paymentHistory: List<PaymentRecord>,
    val paymentRequests: List<PaymentRequestItem> = emptyList()
)
