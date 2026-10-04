package com.auction.app.ui.screens.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.auction.app.data.AuctionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val repository: AuctionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFullNameChange(fullName: String) {
        _uiState.update { it.copy(fullName = fullName, fullNameError = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun register() {
        val currentState = _uiState.value

        val fullNameError = if (currentState.fullName.isBlank()) "Vui lòng nhập họ và tên" else null
        val email = currentState.email.trim()
        val emailError = when {
            email.isBlank() -> "Vui lòng nhập email"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email không đúng định dạng"
            else -> null
        }
        val pw = currentState.password
        val passwordError = when {
            pw.isBlank() -> "Vui lòng nhập mật khẩu"
            pw.length < 8 -> "Mật khẩu phải có tối thiểu 8 ký tự"
            !pw.contains(Regex("^(?=.*[A-Za-z])(?=.*\\d).+$")) -> "Mật khẩu phải bao gồm cả chữ và số"
            else -> null
        }
        if (fullNameError != null || emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(fullNameError = fullNameError, emailError = emailError, passwordError = passwordError)
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            repository.register(email, currentState.password, currentState.fullName.trim())
                .onSuccess {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            registrationSuccess = true,
                            registeredEmail = email
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Lỗi không xác định") }
                }
        }
    }

    fun onRegistrationHandled() {
        _uiState.update { it.copy(registrationSuccess = false) }
    }

    fun onErrorHandled() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        fun factory(repository: AuctionRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = RegisterViewModel(repository) as T
        }
    }
}
