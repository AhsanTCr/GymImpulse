package com.example.features.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.LeaderboardHistoryDoc
import com.example.database.MemberFirestoreRepository
import com.example.database.toFriendlyFirestoreMessage
import com.example.models.LeaderboardCategory
import com.example.models.LeaderboardEntry
import com.example.utils.StreakAndLeaderboardEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LeaderboardUiState(
    val selectedCategory: LeaderboardCategory = LeaderboardCategory.ATTENDANCE_STREAK,
    val entries: List<LeaderboardEntry> = emptyList(),
    val historyItems: List<LeaderboardHistoryDoc> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class LeaderboardViewModel(
    private val repository: MemberFirestoreRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(LeaderboardCategory.ATTENDANCE_STREAK)

    val uiState: StateFlow<LeaderboardUiState> = combine(
        _selectedCategory,
        repository.observeLeaderboardStats(),
        repository.observeLeaderboardHistory().catch { emit(emptyList()) }
    ) { category, stats, history ->
        val ranked = StreakAndLeaderboardEngine.rankEntries(
            stats = stats,
            category = category,
            currentUserId = currentUserId
        )
        LeaderboardUiState(
            selectedCategory = category,
            entries = ranked,
            historyItems = history,
            isLoading = false,
            errorMessage = null
        )
    }.catch { error ->
        emit(
            LeaderboardUiState(
                selectedCategory = _selectedCategory.value,
                isLoading = false,
                errorMessage = error.toFriendlyFirestoreMessage("Unable to load leaderboards.")
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = LeaderboardUiState(isLoading = true)
    )

    fun selectCategory(category: LeaderboardCategory) {
        _selectedCategory.value = category
    }
}
