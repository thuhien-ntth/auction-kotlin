package com.auction.app.ui.screens.search

import com.auction.app.network.model.ProductSummary

data class ProductSearchUiState(
    val keyword: String = "",
    val products: List<ProductSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val totalElements: Long = 0L,
    val totalPages: Int = 1
)
