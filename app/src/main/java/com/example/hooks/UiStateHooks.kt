package com.example.hooks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.theme.AppThemeMode

/**
 * Standard UI state wrapper for asynchronous operations (ready for Phase 2 API/DB calls).
 */
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

@Composable
fun rememberThemeController(
    initialMode: AppThemeMode = AppThemeMode.DARK
): Pair<AppThemeMode, (AppThemeMode) -> Unit> {
    var mode by remember { mutableStateOf(initialMode) }
    return mode to { updated -> mode = updated }
}

@Composable
fun rememberDebouncedAction(
    debounceMs: Long = 400L,
    onAction: () -> Unit
): () -> Unit {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    return remember(debounceMs, onAction) {
        {
            val now = System.currentTimeMillis()
            if (now - lastClickTime >= debounceMs) {
                lastClickTime = now
                onAction()
            }
        }
    }
}
