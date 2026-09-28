package com.auction.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.auction.app.data.AuctionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductSearchViewModel(
    private val repository: AuctionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductSearchUiState())
    val uiState: StateFlow<ProductSearchUiState> = _uiState.asStateFlow()

    init {
        search(1)
    }

    fun onKeywordChange(keyword: String) {
        _uiState.update { it.copy(keyword = keyword) }
    }

    fun search(page: Int = 1) {
        val currentKeyword = _uiState.value.keyword
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.searchProducts(
                keyword = currentKeyword.ifBlank { null },
                category = null,
                page = page - 1,
                size = 10
            ).onSuccess { paged ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        products = paged.content,
                        totalElements = paged.totalElements,
                        totalPages = paged.totalPages.coerceAtLeast(1),
                        currentPage = page.coerceIn(1, paged.totalPages.coerceAtLeast(1))
                    )
                }
            }.onFailure { e ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = e.message ?: "Lỗi không xác định"
                    )
                }
            }
        }
    }

    fun onErrorHandled() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        fun factory(repository: AuctionRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProductSearchViewModel(repository) as T
            }
        }
    }
}
