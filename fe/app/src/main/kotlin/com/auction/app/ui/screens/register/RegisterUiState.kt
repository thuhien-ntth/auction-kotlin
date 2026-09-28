package com.auction.app.ui.screens.register

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,          // lỗi từ API -> hiện toast
    // Lỗi kiểm tra từng trường -> hiện ngay dưới ô nhập
    val fullNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val registrationSuccess: Boolean = false,
    val registeredEmail: String = ""
)
