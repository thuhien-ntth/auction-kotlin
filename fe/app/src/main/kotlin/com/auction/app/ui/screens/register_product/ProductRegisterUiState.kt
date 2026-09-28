package com.auction.app.ui.screens.register_product

import android.net.Uri
import com.auction.app.network.model.CategoryDto

data class ProductRegisterUiState(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val startPrice: String = "",
    val currency: String = "VND",
    val currencyDropdownExpanded: Boolean = false,
    val auctionStartAt: String = "",
    val auctionEndAt: String = "",
    val imageUri: Uri? = null,
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
    val categories: List<CategoryDto> = emptyList(),
    val categoryDropdownExpanded: Boolean = false
)
