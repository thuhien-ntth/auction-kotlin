package com.auction.app.ui.screens.register_product

import android.net.Uri
import com.auction.app.network.model.CategoryDto

data class ProductEditUiState(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val startPrice: String = "",
    val currency: String = "VND",
    val currencyDropdownExpanded: Boolean = false,
    val auctionStartAt: String = "",
    val auctionEndAt: String = "",
    val imageUri: Uri? = null,          // ảnh mới user vừa chọn từ thiết bị
    val existingImageUrl: String? = null, // ảnh đã có trên server
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
    val categories: List<CategoryDto> = emptyList(),
    val categoryDropdownExpanded: Boolean = false
)
