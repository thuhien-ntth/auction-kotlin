package com.auction.app.ui.screens.detail

import com.auction.app.network.model.BidResponse
import com.auction.app.network.model.ProductDetail
import java.math.BigDecimal

data class ProductDetailUiState(
    val isLoading: Boolean = true,
    val detail: ProductDetail? = null,
    val history: List<BidResponse> = emptyList(),
    val isPlacing: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
    val selectedStep: BigDecimal? = null,
    val availableSteps: List<BigDecimal> = emptyList(),
    val showConfirmDialog: Boolean = false,
    val isCurrentHighestBidder: Boolean = false,
    val myUserId: String? = null,
    // Admin: duyệt / từ chối sản phẩm đang chờ duyệt ngay tại màn chi tiết
    val isAdmin: Boolean = false,
    val isReviewing: Boolean = false,
    val showRejectDialog: Boolean = false
)
