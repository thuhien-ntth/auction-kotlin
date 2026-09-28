package com.auction.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.auction.app.data.AuctionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminDashboardViewModel(private val repository: AuctionRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadPendingProducts()
    }

    fun loadPendingProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getPendingProducts()
                .onSuccess { page ->
                    _uiState.update { it.copy(pendingProducts = page.content, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "Lỗi không xác định", isLoading = false) }
                }
        }
    }

    fun selectTab(tab: AdminTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        refreshCurrentTab()
    }

    fun refreshCurrentTab() {
        when (val tab = _uiState.value.selectedTab) {
            AdminTab.PENDING -> loadPendingProducts()
            else -> loadReviewedProducts(tab)
        }
    }

    private fun loadReviewedProducts(tab: AdminTab) {
        val result = tab.apiResult ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getReviewedProducts(result)
                .onSuccess { page ->
                    _uiState.update {
                        if (tab == AdminTab.APPROVED) it.copy(approvedProducts = page.content, isLoading = false)
                        else it.copy(rejectedProducts = page.content, isLoading = false)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "Lỗi không xác định", isLoading = false) }
                }
        }
    }

    fun approveProduct(id: String) {
        viewModelScope.launch {
            repository.approveProduct(id)
                .onSuccess {
                    _uiState.update { it.copy(actionMessage = "Đã duyệt sản phẩm thành công") }
                    loadPendingProducts()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "Lỗi không xác định") }
                }
        }
    }

    fun rejectProduct(id: String, reason: String) {
        viewModelScope.launch {
            repository.rejectProduct(id, reason)
                .onSuccess {
                    _uiState.update { it.copy(actionMessage = "Đã từ chối sản phẩm") }
                    loadPendingProducts()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "Lỗi không xác định") }
                }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null, error = null) }
    }

    fun onErrorHandled() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        fun factory(repository: AuctionRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AdminDashboardViewModel(repository) as T
        }
    }
}
