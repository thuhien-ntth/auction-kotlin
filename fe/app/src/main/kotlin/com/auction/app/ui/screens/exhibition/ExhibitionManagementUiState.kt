package com.auction.app.ui.screens.exhibition

import com.auction.app.network.model.ProductSummary

data class ExhibitionManagementUiState(
    val products: List<ProductSummary> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
