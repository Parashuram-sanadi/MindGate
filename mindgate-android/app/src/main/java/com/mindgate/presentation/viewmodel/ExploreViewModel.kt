package com.mindgate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.mindgate.data.local.content.ExploreCategory
import com.mindgate.data.local.content.ExploreContentLibrary
import com.mindgate.data.local.content.InspirationVideo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ExploreUiState(
    val selectedCategory: ExploreCategory? = null,
    val videos: List<InspirationVideo> = ExploreContentLibrary.videos,
    val currentlyPlayingVideo: InspirationVideo? = null
)

@HiltViewModel
class ExploreViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    fun selectCategory(category: ExploreCategory?) {
        _uiState.update { state ->
            state.copy(
                selectedCategory = category,
                videos = if (category == null) {
                    ExploreContentLibrary.videos
                } else {
                    ExploreContentLibrary.forCategory(category)
                }
            )
        }
    }

    fun playVideo(video: InspirationVideo?) {
        _uiState.update { it.copy(currentlyPlayingVideo = video) }
    }
}
