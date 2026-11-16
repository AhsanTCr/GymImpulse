package com.example.features.membership

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.MemberFirestoreRepository
import com.example.database.toFriendlyFirestoreMessage
import com.example.models.MembershipOverview
import com.example.models.PaymentRequestItem
import com.example.utils.MembershipCycleCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MembershipUiState(
    val isLoading: Boolean = true,
    val memberName: String = "",
    val memberCode: String = "",
    val overview: MembershipOverview? = null,
    val isSubmittingPayment: Boolean = false,
    val submissionMessage: String? = null,
    val errorMessage: String? = null
)

class MembershipViewModel(
    private val repository: MemberFirestoreRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _isSubmitting = MutableStateFlow(false)
    private val _submissionMessage = MutableStateFlow<String?>(null)
    private val _actionError = MutableStateFlow<String?>(null)

    private val dataStreams = combine(
        repository.observeUserProfile(currentUserId),
        repository.observeMembership(currentUserId),
        repository.observePaymentHistory(currentUserId),
        repository.observePaymentRequests(currentUserId)
    ) { profile, membershipDoc, payments, requests ->
        val overview = MembershipCycleCalculator.toOverview(
            record = membershipDoc,
            payments = payments
        ).copy(
            paymentRequests = requests.map { req ->
                PaymentRequestItem(
                    id = req.id,
                    amountFormatted = req.amountFormatted,
                    paymentMethod = req.paymentMethod,
                    referenceNote = req.referenceNote,
                    paymentDateLabel = req.paymentDateLabel,
                    status = req.status,
                    isVerified = req.isVerified
                )
            }
        )
        Triple(profile, overview, null as String?)
    }

    val uiState: StateFlow<MembershipUiState> = combine(
        dataStreams,
        _isSubmitting,
        _submissionMessage,
        _actionError
    ) { (profile, overview, _), submitting, message, actionErr ->
        MembershipUiState(
            isLoading = false,
            memberName = profile?.fullName ?: "Club Member",
            memberCode = profile?.memberCode ?: "GP-MEMBER",
            overview = overview,
            isSubmittingPayment = submitting,
            submissionMessage = message,
            errorMessage = actionErr
        )
    }.catch { error ->
        emit(
            MembershipUiState(
                isLoading = false,
                errorMessage = error.toFriendlyFirestoreMessage("Unable to load membership details.")
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = MembershipUiState(isLoading = true)
    )

    fun submitPaymentNotification(paymentMethod: String, referenceNote: String) {
        val current = uiState.value
        val overview = current.overview ?: return
        if (current.isSubmittingPayment) return

        _isSubmitting.value = true
        _actionError.value = null
        _submissionMessage.value = null

        viewModelScope.launch {
            try {
                repository.submitPaymentRequest(
                    memberName = current.memberName,
                    memberCode = current.memberCode,
                    amountFormatted = overview.monthlyFeeFormatted.substringBefore(" /"),
                    paymentMethod = paymentMethod,
                    referenceNote = referenceNote
                )
                _isSubmitting.value = false
                _submissionMessage.value =
                    "Payment notification submitted for club admin verification. Your cycle activates once verified."
            } catch (e: Exception) {
                _isSubmitting.value = false
                _actionError.value = e.toFriendlyFirestoreMessage("Could not submit payment notification.")
            }
        }
    }
}
