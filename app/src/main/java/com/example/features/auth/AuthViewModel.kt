package com.example.features.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.MemberFirestoreRepository
import com.example.database.safeFirebaseAuth
import com.example.database.toFriendlyFirestoreMessage
import com.example.models.UserRole
import com.example.services.GoogleSignInOutcome
import com.example.services.GymAuthManager
import com.example.utils.ImageCompressionUtils
import com.example.utils.ValidationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val selectedRole: UserRole = UserRole.MEMBER,
    val isSigningIn: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

data class RegisterUiState(
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val fitnessGoal: String = "Lean Strength & 140g Daily Protein",
    val referralCode: String = "",
    val selectedAvatarUri: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val repository: MemberFirestoreRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(RegisterUiState())
    val registerState: StateFlow<RegisterUiState> = _registerState.asStateFlow()

    fun selectLoginRole(role: UserRole) {
        _loginState.update { it.copy(selectedRole = role, infoMessage = null, errorMessage = null) }
    }

    fun signInWithGoogle(context: Context, onAuthenticated: () -> Unit = {}) {
        if (_loginState.value.isSigningIn) return
        _loginState.update { it.copy(isSigningIn = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            when (val outcome = GymAuthManager.signInWithGoogleInteractive(context)) {
                is GoogleSignInOutcome.Success -> {
                    prefillRegistrationFromUser(
                        displayName = outcome.user.displayName.orEmpty(),
                        email = outcome.user.email.orEmpty()
                    )
                    _loginState.update { it.copy(isSigningIn = false) }
                    onAuthenticated()
                }
                is GoogleSignInOutcome.Cancelled -> {
                    _loginState.update {
                        it.copy(
                            isSigningIn = false,
                            infoMessage = "Sign-in was cancelled. Select a Google account to continue."
                        )
                    }
                }
                is GoogleSignInOutcome.Error -> {
                    _loginState.update {
                        it.copy(isSigningIn = false, errorMessage = outcome.message)
                    }
                }
            }
        }
    }

    fun prefillRegistrationFromUser(displayName: String, email: String) {
        _registerState.update { current ->
            current.copy(
                fullName = current.fullName.ifBlank { displayName },
                email = current.email.ifBlank { email }
            )
        }
    }

    fun updateRegisterField(
        fullName: String? = null,
        phone: String? = null,
        email: String? = null,
        fitnessGoal: String? = null,
        referralCode: String? = null
    ) {
        _registerState.update {
            it.copy(
                fullName = fullName ?: it.fullName,
                phone = phone ?: it.phone,
                email = email ?: it.email,
                fitnessGoal = fitnessGoal ?: it.fitnessGoal,
                referralCode = referralCode ?: it.referralCode,
                errorMessage = null
            )
        }
    }

    fun setRegisterAvatar(uri: String?) {
        _registerState.update { it.copy(selectedAvatarUri = uri) }
    }

    fun submitRegistration(context: Context, onSuccess: () -> Unit) {
        val state = _registerState.value
        if (state.isSubmitting) return

        when {
            state.fullName.trim().length < 2 -> {
                _registerState.update { it.copy(errorMessage = "Please enter your full name (at least 2 characters).") }
                return
            }
            state.phone.isNotBlank() && !ValidationUtils.isValidEmailOrPhone(state.phone) -> {
                _registerState.update { it.copy(errorMessage = "Please enter a valid phone number.") }
                return
            }
        }

        _registerState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val firebaseAuth = safeFirebaseAuth(context)
                if (firebaseAuth == null) {
                    _registerState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "Firebase is not configured for this build. Please add google-services.json."
                        )
                    }
                    return@launch
                }

                // Ensure Google Sign-In completed first
                if (firebaseAuth.currentUser == null) {
                    when (val outcome = GymAuthManager.signInWithGoogleInteractive(context, firebaseAuth)) {
                        is GoogleSignInOutcome.Success -> {
                            _registerState.update {
                                it.copy(
                                    email = it.email.ifBlank { outcome.user.email.orEmpty() },
                                    fullName = it.fullName.ifBlank { outcome.user.displayName.orEmpty() }
                                )
                            }
                        }
                        is GoogleSignInOutcome.Cancelled -> {
                            _registerState.update {
                                it.copy(isSubmitting = false, errorMessage = "Google Sign-In cancelled. Please choose an account to register.")
                            }
                            return@launch
                        }
                        is GoogleSignInOutcome.Error -> {
                            _registerState.update { it.copy(isSubmitting = false, errorMessage = outcome.message) }
                            return@launch
                        }
                    }
                }

                val currentUser = firebaseAuth.currentUser
                if (currentUser == null) {
                    _registerState.update { it.copy(isSubmitting = false, errorMessage = "Authentication required.") }
                    return@launch
                }

                val compressedAvatar = ImageCompressionUtils.compressUriToDataUri(
                    context = context,
                    uriString = state.selectedAvatarUri
                )
                val finalEmail = state.email.ifBlank { currentUser.email.orEmpty() }

                repository.registerMemberProfile(
                    fullName = state.fullName.trim(),
                    phone = state.phone.trim().takeIf { it.isNotBlank() },
                    email = finalEmail,
                    avatarData = compressedAvatar,
                    fitnessGoal = state.fitnessGoal.trim(),
                    referredByCode = state.referralCode.trim().takeIf { it.isNotBlank() }
                )

                _registerState.update { it.copy(isSubmitting = false) }
                onSuccess()
            } catch (e: Exception) {
                _registerState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.toFriendlyFirestoreMessage("Failed to create member profile. Please retry.")
                    )
                }
            }
        }
    }

    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            GymAuthManager.signOut(context)
            _registerState.value = RegisterUiState()
            onComplete()
        }
    }
}
