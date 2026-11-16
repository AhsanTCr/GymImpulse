package com.example.features.announcements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.MemberFirestoreRepository
import com.example.database.toFriendlyFirestoreMessage
import com.example.features.member.toDomainModel
import com.example.models.AnnouncementCategory
import com.example.models.GymAnnouncement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AnnouncementsUiState(
    val isLoading: Boolean = true,
    val selectedFilter: AnnouncementCategory = AnnouncementCategory.ALL,
    val allAnnouncements: List<GymAnnouncement> = emptyList(),
    val filteredAnnouncements: List<GymAnnouncement> = emptyList(),
    val errorMessage: String? = null
)

class AnnouncementsViewModel(
    repository: MemberFirestoreRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(AnnouncementCategory.ALL)

    val uiState: StateFlow<AnnouncementsUiState> = combine(
        repository.observeAnnouncements(),
        _selectedFilter
    ) { docs, filter ->
        val domainList = docs.map { it.toDomainModel() }
        val filtered = when (filter) {
            AnnouncementCategory.ALL -> domainList
            AnnouncementCategory.IMPORTANT -> domainList.filter { it.isImportant }
            else -> domainList.filter { it.category == filter }
        }
        AnnouncementsUiState(
            isLoading = false,
            selectedFilter = filter,
            allAnnouncements = domainList,
            filteredAnnouncements = filtered,
            errorMessage = null
        )
    }.catch { error ->
        emit(
            AnnouncementsUiState(
                isLoading = false,
                selectedFilter = _selectedFilter.value,
                errorMessage = error.toFriendlyFirestoreMessage("Unable to load announcements.")
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AnnouncementsUiState(isLoading = true)
    )

    fun selectFilter(category: AnnouncementCategory) {
        _selectedFilter.value = category
    }
}
