package com.auction.app.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.auction.app.data.AuctionRepository
import com.auction.app.network.model.PlaceBidRequest
import com.auction.app.util.StepValueCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class ProductDetailViewModel(
    private val repository: AuctionRepository,
    private val productId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeUserId()
        observeIsAdmin()
    }

    private fun observeIsAdmin() {
        viewModelScope.launch {
            repository.tokenStore.isAdminFlow.collect { admin ->
                _uiState.update { it.copy(isAdmin = admin) }
            }
        }
    }

    // ---- Admin: duyệt / từ chối sản phẩm PENDING_APPROVAL ----
    fun approveProduct() {
        _uiState.update { it.copy(isReviewing = true, message = null) }
        viewModelScope.launch {
            repository.approveProduct(productId)
                .onSuccess {
                    _uiState.update { it.copy(isReviewing = false, message = "Đã duyệt sản phẩm", messageIsError = false) }
                    loadData()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isReviewing = false, message = e.message ?: "Lỗi không xác định", messageIsError = true) }
                }
        }
    }

    fun showRejectDialog() { _uiState.update { it.copy(showRejectDialog = true) } }
    fun dismissRejectDialog() { _uiState.update { it.copy(showRejectDialog = false) } }

    fun rejectProduct(reason: String) {
        _uiState.update { it.copy(showRejectDialog = false, isReviewing = true, message = null) }
        viewModelScope.launch {
            repository.rejectProduct(productId, reason)
                .onSuccess {
                    _uiState.update { it.copy(isReviewing = false, message = "Đã từ chối sản phẩm", messageIsError = false) }
                    loadData()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isReviewing = false, message = e.message ?: "Lỗi không xác định", messageIsError = true) }
                }
        }
    }

    private fun loadData() {
        _uiState.update { it.copy(isLoading = true, message = null, messageIsError = false) }
        viewModelScope.launch {
            val detailResult = repository.getProduct(productId)
            val historyResult = repository.bidHistory(productId)

            if (detailResult.isSuccess && historyResult.isSuccess) {
                val detail = detailResult.getOrNull()
                val history = historyResult.getOrNull()?.content ?: emptyList()
                
                val currentPrice = detail?.currentPrice ?: BigDecimal.ZERO
                val availableSteps = StepValueCalculator.getAvailableSteps(currentPrice, detail?.currency ?: "VND")
                val selectedStep = if (availableSteps.isNotEmpty()) availableSteps.first() else null
                
                val myUserId = _uiState.value.myUserId
                val isCurrentHighestBidder = history.firstOrNull { it.accepted }?.bidderId != null &&
                        history.firstOrNull { it.accepted }?.bidderId == myUserId

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        detail = detail,
                        history = history,
                        availableSteps = availableSteps,
                        selectedStep = it.selectedStep ?: selectedStep,
                        isCurrentHighestBidder = isCurrentHighestBidder
                    )
                }
            } else {
                val e = detailResult.exceptionOrNull() ?: historyResult.exceptionOrNull()
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        message = "Không tải được sản phẩm: ${e?.message}", 
                        messageIsError = true
                    )
                }
            }
        }
    }

    private fun observeUserId() {
        viewModelScope.launch {
            repository.tokenStore.userIdFlow.collect { userId ->
                _uiState.update { state ->
                    val isCurrentHighestBidder = state.history.firstOrNull { it.accepted }?.bidderId != null &&
                            state.history.firstOrNull { it.accepted }?.bidderId == userId
                    state.copy(
                        myUserId = userId,
                        isCurrentHighestBidder = isCurrentHighestBidder
                    )
                }
            }
        }
    }

    fun selectStep(step: BigDecimal) {
        _uiState.update { it.copy(selectedStep = step) }
    }

    fun showConfirmDialog() {
        _uiState.update { it.copy(showConfirmDialog = true) }
    }

    fun dismissConfirmDialog() {
        _uiState.update { it.copy(showConfirmDialog = false) }
    }

    val targetBidPrice: BigDecimal
        get() {
            val currentPrice = _uiState.value.detail?.currentPrice ?: BigDecimal.ZERO
            val stepVal = _uiState.value.selectedStep ?: BigDecimal.ZERO
            return currentPrice.add(stepVal)
        }

    fun placeBid() {
        _uiState.update { it.copy(showConfirmDialog = false, isPlacing = true, message = null) }
        viewModelScope.launch {
            repository.placeBid(productId, targetBidPrice).onSuccess { res ->
                _uiState.update { 
                    it.copy(
                        isPlacing = false,
                        message = if (res.accepted) "Đặt giá thành công!" else "Đặt giá không được chấp nhận.",
                        messageIsError = !res.accepted
                    )
                }
                loadData()
            }.onFailure { e ->
                _uiState.update { 
                    it.copy(
                        isPlacing = false,
                        message = e.message ?: "Lỗi không xác định",
                        messageIsError = true
                    )
                }
            }
        }
    }

    fun onErrorHandled() {
        _uiState.update { it.copy(message = null, messageIsError = false) }
    }

    companion object {
        fun factory(repository: AuctionRepository, productId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProductDetailViewModel(repository, productId) as T
            }
        }
    }
}
