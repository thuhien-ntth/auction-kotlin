package com.auction.app

import android.app.Application
import androidx.compose.runtime.compositionLocalOf
import com.auction.app.data.AuctionRepository
import com.auction.app.data.TokenStore
import com.auction.app.network.ApiClient
import com.auction.app.network.AuctionApi
import com.auction.app.network.MockAuctionApi

// DI thủ công (không dùng Hilt/Koin) để giữ khung tối thiểu, dễ đọc cho đồ án.
class AppContainer(application: Application) {
    val tokenStore = TokenStore(application)

    // Đã tắt Mock Data — app luôn gọi backend thật qua ApiClient (be/api-gateway).
    // Bật lại true tạm thời nếu cần chạy demo UI mà không có Docker/Server.
    val api: AuctionApi = if (USE_MOCK_DATA) {
        MockAuctionApi(tokenStore)
    } else {
        ApiClient.create(tokenStore)
    }

    val repository = AuctionRepository(api, tokenStore)

    companion object {
        const val USE_MOCK_DATA = false
    }
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
    error("AppContainer chưa được cung cấp — bọc màn hình trong CompositionLocalProvider ở MainActivity")
}
