package com.auction.app.ui.screens.exhibition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.auction.app.data.AuctionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExhibitionManagementViewModel(private val repository: AuctionRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ExhibitionManagementUiState())
    val uiState: StateFlow<ExhibitionManagementUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.myProducts(null)
                .onSuccess { page ->
                    _uiState.update { it.copy(products = page.content, isLoading = false) }
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
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ExhibitionManagementViewModel(repository) as T
        }
    }
}
