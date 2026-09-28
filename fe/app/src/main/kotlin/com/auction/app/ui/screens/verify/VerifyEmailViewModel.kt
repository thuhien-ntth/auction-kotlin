package com.auction.app.ui.screens.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.auction.app.data.AuctionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VerifyEmailViewModel(
    private val repository: AuctionRepository,
    private val email: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerifyEmailUiState())
    val uiState: StateFlow<VerifyEmailUiState> = _uiState.asStateFlow()

    fun onTokenChange(token: String) {
        _uiState.update { it.copy(token = token) }
    }

    fun verify() {
        val currentToken = _uiState.value.token
        if (currentToken.isBlank()) {
            _uiState.update { it.copy(error = "Vui lòng nhập mã xác nhận") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null, success = null) }
        
        viewModelScope.launch {
            repository.verifyEmail(email, currentToken)
                .onSuccess {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            success = "Xác thực thành công! Đang chuyển về màn đăng nhập...",
                            verificationSuccess = true
                        )
                    }
                }
                .onFailure {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Mã xác nhận không đúng hoặc đã hết hạn."
                        )
                    }
                }
        }
    }

    fun onVerificationHandled() {
        _uiState.update { it.copy(verificationSuccess = false) }
    }

    fun onErrorHandled() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        fun factory(repository: AuctionRepository, email: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = VerifyEmailViewModel(repository, email) as T
        }
    }
}
