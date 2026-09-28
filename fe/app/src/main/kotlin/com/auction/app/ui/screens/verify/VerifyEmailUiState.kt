package com.auction.app.ui.screens.verify

data class VerifyEmailUiState(
    val token: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val verificationSuccess: Boolean = false
)
