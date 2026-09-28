package com.auction.app.ui.screens.register_product

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.auction.app.data.AuctionRepository
import com.auction.app.network.model.CreateProductRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProductRegisterViewModel(private val repository: AuctionRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductRegisterUiState())
    val uiState: StateFlow<ProductRegisterUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories().onSuccess { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }

    fun onTitleChange(value: String) { _uiState.update { it.copy(title = value) } }
    fun onDescriptionChange(value: String) { _uiState.update { it.copy(description = value) } }
    fun onCategoryChange(value: String) { _uiState.update { it.copy(category = value, categoryDropdownExpanded = false) } }
    fun onCategoryDropdownExpandedChange(expanded: Boolean) { _uiState.update { it.copy(categoryDropdownExpanded = expanded) } }
    fun onStartPriceChange(value: String) { _uiState.update { it.copy(startPrice = value) } }
    fun onCurrencyChange(value: String) { _uiState.update { it.copy(currency = value, currencyDropdownExpanded = false) } }
    fun onCurrencyDropdownExpandedChange(expanded: Boolean) { _uiState.update { it.copy(currencyDropdownExpanded = expanded) } }
    fun onAuctionStartAtChange(value: String) { _uiState.update { it.copy(auctionStartAt = value) } }
    fun onAuctionEndAtChange(value: String) { _uiState.update { it.copy(auctionEndAt = value) } }
    fun onImageSelected(uri: Uri?) { _uiState.update { it.copy(imageUri = uri) } }

    fun submitProduct(context: Context) {
        val currentState = _uiState.value
        val price = currentState.startPrice.toBigDecimalOrNull() ?: return

        _uiState.update { it.copy(isSubmitting = true, message = null) }

        viewModelScope.launch {
            repository.registerProduct(
                CreateProductRequest(
                    title = currentState.title.trim(),
                    description = currentState.description.trim(),
                    category = currentState.category.trim(),
                    startPrice = price,
                    currency = currentState.currency,
                    auctionStartAt = currentState.auctionStartAt.trim().ifEmpty { null },
                    auctionEndAt = currentState.auctionEndAt.trim().ifEmpty { null }
                )
            ).onSuccess { created ->
                val pickedUri = currentState.imageUri
                var message = "Đăng sản phẩm thành công! Sản phẩm đang chờ Admin duyệt."
                var messageIsError = false

                if (pickedUri != null) {
                    try {
                        val bytes = context.contentResolver.openInputStream(pickedUri)?.use { it.readBytes() }
                        if (bytes != null) {
                            val mimeType = context.contentResolver.getType(pickedUri) ?: "image/jpeg"
                            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                            val part = MultipartBody.Part.createFormData("file", "product.jpg", requestBody)
                            
                            repository.uploadProductImage(created.id, part).onFailure { error ->
                                message = error.message ?: "Lỗi không xác định"
                                messageIsError = true
                            }
                        }
                    } catch (e: Exception) {
                        message = e.message ?: "Lỗi không xác định"
                        messageIsError = true
                    }
                }

                _uiState.update {
                    if (!messageIsError) {
                        ProductRegisterUiState(message = message, messageIsError = false) // reset fields
                    } else {
                        it.copy(message = message, messageIsError = true, isSubmitting = false)
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        message = error.message ?: "Lỗi không xác định",
                        messageIsError = true,
                        isSubmitting = false
                    )
                }
            }
        }
    }

    fun onErrorHandled() {
        _uiState.update { it.copy(message = null, messageIsError = false) }
    }

    companion object {
        fun factory(repository: AuctionRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ProductRegisterViewModel(repository) as T
        }
    }
}
