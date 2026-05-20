package com.example.blesstify.presentation.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.domain.model.SearchHistory
import com.example.blesstify.domain.model.Song
import com.example.blesstify.domain.auth.AuthState
import com.example.blesstify.domain.usecase.SearchSongsUseCase
import com.example.blesstify.domain.usecase.ObserveAuthStateUseCase
import com.example.blesstify.domain.usecase.SaveSearchHistoryUseCase
import com.example.blesstify.domain.usecase.GetRecentSearchesUseCase
import com.example.blesstify.domain.usecase.DeleteSearchHistoryUseCase
import com.example.blesstify.domain.usecase.ClearSearchHistoryUseCase
import com.example.blesstify.domain.usecase.GetSongsByGenreUseCase
import com.example.blesstify.domain.usecase.GetPublicSongsUseCase
import com.example.blesstify.presentation.player.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowseCategory(
    val name: String,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false
)

data class SearchUiState(
    val query: String = "",
    val results: List<Song> = emptyList(),
    val recentSearches: List<SearchHistory> = emptyList(),
    val browseCategories: List<BrowseCategory> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchSongsUseCase: SearchSongsUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val saveSearchHistoryUseCase: SaveSearchHistoryUseCase,
    private val getRecentSearchesUseCase: GetRecentSearchesUseCase,
    private val deleteSearchHistoryUseCase: DeleteSearchHistoryUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase,
    private val getSongsByGenreUseCase: GetSongsByGenreUseCase,
    private val getPublicSongsUseCase: GetPublicSongsUseCase,
    private val musicController: MusicController
) : ViewModel() {

    private val browseCategoryNames = listOf(
        "Pop", "Rock", "Hip Hop", "Rap", "EDM", "Electronic", "Jazz", "Blues",
        "Classical", "R&B", "Country", "Folk", "Metal", "Reggae", "K-pop", "J-pop",
        "C-pop", "Latin", "Phonk", "Lo-fi", "Other"
    )
    private val otherCategoryName = "Other"
    private val otherCategoryFilter = browseCategoryNames.filter { it != otherCategoryName }.toSet()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collectLatest { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        currentUserId = state.userId
                        loadRecentSearches()
                        val currentQuery = _uiState.value.query
                        if (currentQuery.isNotBlank()) {
                            onQueryChange(currentQuery)
                        }
                    }
                    else -> {
                        currentUserId = null
                        _uiState.update { it.copy(recentSearches = emptyList()) }
                    }
                }
            }
        }
        loadBrowseCategories()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }

        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update {
                it.copy(results = emptyList(), isLoading = false, hasSearched = false, error = null)
            }
            return
        }

        searchJob = viewModelScope.launch {
            delay(400) // debounce
            searchSongsUseCase(query, currentUserId).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                results = result.data.orEmpty().take(50),
                                isLoading = false,
                                hasSearched = true
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.error?.toString() ?: "Search failed",
                                hasSearched = true
                            )
                        }
                    }
                }
            }
        }
    }

    /** Call when user explicitly submits a search (press Enter/search icon) */
    fun onSearchSubmit() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        val uid = currentUserId ?: return

        viewModelScope.launch {
            try {
                saveSearchHistoryUseCase(uid, query)
                loadRecentSearches()
            } catch (_: Exception) { /* best effort */ }
        }
    }

    fun onRecentSearchClick(query: String) {
        onQueryChange(query)
        onSearchSubmit()
    }

    fun onDeleteRecentSearch(searchId: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            try {
                deleteSearchHistoryUseCase(uid, searchId)
                loadRecentSearches()
            } catch (_: Exception) { /* best effort */ }
        }
    }

    fun onClearAllHistory() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            try {
                clearSearchHistoryUseCase(uid)
                _uiState.update { it.copy(recentSearches = emptyList()) }
            } catch (_: Exception) { /* best effort */ }
        }
    }

    fun addSongToQueue(song: Song) {
        musicController.addToQueue(song)
    }

    fun playSongNext(song: Song) {
        musicController.addToQueueNext(song)
    }

    private fun loadBrowseCategories() {
        val browseCategories = browseCategoryNames.map { name ->
            BrowseCategory(name = name, isLoading = true)
        }
        _uiState.update { it.copy(browseCategories = browseCategories) }

        browseCategoryNames.forEach { name ->
            if (name == otherCategoryName) {
                loadOtherCategory()
            } else {
                loadGenreCategory(name)
            }
        }
    }

    private fun loadGenreCategory(genre: String) {
        viewModelScope.launch {
            getSongsByGenreUseCase(genre, currentUserId).collect { result ->
                when (result) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        _uiState.update { state ->
                            val updatedList = state.browseCategories.map { cat ->
                                if (cat.name == genre) {
                                    cat.copy(songs = result.data ?: emptyList(), isLoading = false)
                                } else cat
                            }
                            state.copy(browseCategories = updatedList)
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { state ->
                            val updatedList = state.browseCategories.map { cat ->
                                if (cat.name == genre) {
                                    cat.copy(isLoading = false)
                                } else cat
                            }
                            state.copy(browseCategories = updatedList)
                        }
                    }
                }
            }
        }
    }

    private fun loadOtherCategory() {
        viewModelScope.launch {
            getPublicSongsUseCase(limit = 50).collect { result ->
                when (result) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        val filteredSongs = (result.data ?: emptyList()).filter { song ->
                            song.genre.isEmpty() || song.genre.none { it in otherCategoryFilter }
                        }
                        _uiState.update { state ->
                            val updatedList = state.browseCategories.map { cat ->
                                if (cat.name == otherCategoryName) {
                                    cat.copy(songs = filteredSongs, isLoading = false)
                                } else cat
                            }
                            state.copy(browseCategories = updatedList)
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { state ->
                            val updatedList = state.browseCategories.map { cat ->
                                if (cat.name == otherCategoryName) {
                                    cat.copy(isLoading = false)
                                } else cat
                            }
                            state.copy(browseCategories = updatedList)
                        }
                    }
                }
            }
        }
    }

    private fun loadRecentSearches() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            try {
                val searches = getRecentSearchesUseCase(uid, 10)
                _uiState.update { it.copy(recentSearches = searches) }
            } catch (_: Exception) { /* best effort */ }
        }
    }
}
