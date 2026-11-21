package com.example.services

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Manages Firebase Authentication exclusively via Google Sign-In + Jetpack Credential Manager.
 * Never uses anonymous auth or email/password auth in application code.
 */
object GymAuthManager {

    private const val TAG = "GymAuthManager"

    private fun resolveDefaultWebClientId(context: Context): String? {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resId == 0) return null
        return runCatching { context.getString(resId) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    fun authStateFlow(auth: FirebaseAuth = Firebase.auth): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Silent auto sign-in using GetGoogleIdOption in its own single-option GetCredentialRequest.
     */
    suspend fun attemptSilentSignIn(
        context: Context,
        auth: FirebaseAuth = Firebase.auth
    ): Boolean {
        if (auth.currentUser != null) return true
        val clientId = resolveDefaultWebClientId(context) ?: return false

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(authCredential).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.d(TAG, "Silent Google Sign-In unavailable: ${e.message}")
            false
        }
    }

    /**
     * Interactive Google Sign-In using GetSignInWithGoogleOption in its own single-option GetCredentialRequest.
     */
    suspend fun signInWithGoogleInteractive(
        context: Context,
        auth: FirebaseAuth = Firebase.auth
    ): GoogleSignInOutcome {
        val clientId = resolveDefaultWebClientId(context)
            ?: return GoogleSignInOutcome.Error(
                "Google Sign-In configuration missing (default_web_client_id)."
            )

        val activityContext = context as? Activity ?: return GoogleSignInOutcome.Error(
            "Unable to launch Google Sign-In sheet from this context."
        )

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                    ?: return GoogleSignInOutcome.Error("Authentication completed without a user session.")
                GoogleSignInOutcome.Success(user)
            } else {
                GoogleSignInOutcome.Error("Unexpected credential type returned by Credential Manager.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
            GoogleSignInOutcome.Cancelled
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            GoogleSignInOutcome.Error(e.localizedMessage ?: "Google Sign-In failed. Please try again.")
        }
    }

    suspend fun signOut(
        context: Context,
        auth: FirebaseAuth = Firebase.auth
    ) {
        withContext(NonCancellable) {
            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                auth.signOut()
            }
        }
    }
}

sealed interface GoogleSignInOutcome {
    data class Success(val user: FirebaseUser) : GoogleSignInOutcome
    data object Cancelled : GoogleSignInOutcome
    data class Error(val message: String) : GoogleSignInOutcome
}
