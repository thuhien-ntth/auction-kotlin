package com.auction.app.ui.screens.admin

import com.auction.app.network.model.ProductSummary

enum class AdminTab(val label: String, val apiResult: String?) {
    PENDING("Chờ duyệt", null),
    APPROVED("Đã duyệt", "APPROVED"),
    REJECTED("Đã từ chối", "REJECTED")
}

data class AdminDashboardUiState(
    val selectedTab: AdminTab = AdminTab.PENDING,
    val pendingProducts: List<ProductSummary> = emptyList(),
    val approvedProducts: List<ProductSummary> = emptyList(),
    val rejectedProducts: List<ProductSummary> = emptyList(),
    val isLoading: Boolean = true,
    val actionMessage: String? = null,
    val error: String? = null
)
