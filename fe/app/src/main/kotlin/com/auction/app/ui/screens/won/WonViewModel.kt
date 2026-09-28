package com.auction.app.ui.screens.won

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.auction.app.data.AuctionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WonViewModel(private val repository: AuctionRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(WonUiState())
    val uiState: StateFlow<WonUiState> = _uiState.asStateFlow()

    init {
        loadWon()
    }

    private fun loadWon() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.won()
                .onSuccess { page ->
                    _uiState.update { it.copy(auctionStates = page.content, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "Lỗi không xác định", isLoading = false) }
                }
        }
    }

    fun onErrorHandled() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        fun factory(repository: AuctionRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = WonViewModel(repository) as T
        }
    }
}
