package com.auction.app.ui.screens.participating

import com.auction.app.network.model.AuctionStateResponse

data class ParticipatingUiState(
    val auctionStates: List<AuctionStateResponse> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
