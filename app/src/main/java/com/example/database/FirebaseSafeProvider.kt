package com.example.database

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private const val FIREBASE_SAFE_TAG = "FirebaseSafeProvider"

/**
 * Checks whether a default [FirebaseApp] has been initialized (for example when
 * `google-services.json` was absent at build time and `FirebaseInitProvider` did not run).
 */
fun isFirebaseInitialized(context: Context? = null): Boolean {
    return if (context != null) {
        runCatching { FirebaseApp.getApps(context.applicationContext).isNotEmpty() }
            .getOrDefault(false)
    } else {
        runCatching { FirebaseApp.getInstance() }.isSuccess
    }
}

/**
 * Safely resolves [FirebaseAuth] or returns `null` when Firebase is not initialized.
 */
fun safeFirebaseAuth(context: Context? = null): FirebaseAuth? {
    if (!isFirebaseInitialized(context)) return null
    return runCatching { Firebase.auth }
        .onFailure { Log.w(FIREBASE_SAFE_TAG, "FirebaseAuth unavailable: ${it.message}") }
        .getOrNull()
}

/**
 * Safely resolves the configured named [FirebaseFirestore] instance (`R.string.firestore_database_id`)
 * or returns `null` when Firebase is not initialized.
 */
fun safeFirebaseFirestore(context: Context): FirebaseFirestore? {
    val appContext = context.applicationContext
    if (!isFirebaseInitialized(appContext)) return null
    val databaseId = runCatching { appContext.getString(R.string.firestore_database_id) }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: return null

    return runCatching { FirebaseFirestore.getInstance(databaseId) }
        .onFailure { Log.w(FIREBASE_SAFE_TAG, "FirebaseFirestore unavailable: ${it.message}") }
        .getOrNull()
}
