package com.example.database

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

/**
 * Logs structured diagnostic JSON for every Firestore failure via Log.e("FirestoreError", ...).
 * Never surfaces raw JSON or unhandled exceptions to Compose UI.
 */
fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?,
    authProvider: () -> FirebaseAuth? = { runCatching { Firebase.auth }.getOrNull() }
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = runCatching { authProvider()?.currentUser }.getOrNull()

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

/**
 * Unwraps CancellationException emitted by Query.snapshots()/DocumentReference.snapshots()
 * on Firestore errors down to the underlying FirebaseFirestoreException.
 */
fun Throwable.findFirestoreException(): FirebaseFirestoreException? {
    return generateSequence(this) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
}

fun Throwable.toFriendlyFirestoreMessage(fallback: String): String {
    val fs = findFirestoreException()
    return when (fs?.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Access denied for this operation. Please verify your session or gym verification status."
        FirebaseFirestoreException.Code.ALREADY_EXISTS ->
            "This record already exists for today."
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Club sync is temporarily unavailable. Check your connection and retry."
        else -> fs?.localizedMessage ?: localizedMessage ?: fallback
    }
}
