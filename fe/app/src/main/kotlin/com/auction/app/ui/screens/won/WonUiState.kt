package com.auction.app.ui.screens.won

import com.auction.app.network.model.AuctionStateResponse

data class WonUiState(
    val auctionStates: List<AuctionStateResponse> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
