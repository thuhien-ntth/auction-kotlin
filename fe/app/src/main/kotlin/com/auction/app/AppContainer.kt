package com.auction.app

import android.app.Application
import androidx.compose.runtime.compositionLocalOf
import com.auction.app.data.AuctionRepository
import com.auction.app.data.TokenStore
import com.auction.app.network.ApiClient
import com.auction.app.network.AuctionApi

class AppContainer(application: Application) {
    val tokenStore = TokenStore(application)

    val api: AuctionApi = ApiClient.create(tokenStore)

    val repository = AuctionRepository(api, tokenStore)
}

class AuctionApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("Lỗi hệ thống")
}
