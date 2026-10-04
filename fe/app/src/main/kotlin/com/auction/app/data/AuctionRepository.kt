package com.auction.app.data

import com.auction.app.network.AuctionApi
import com.auction.app.network.model.*
import okhttp3.MultipartBody
import java.math.BigDecimal

class AuctionRepository(
    private val api: AuctionApi,
    val tokenStore: TokenStore
) {
    // Auth
    private inline fun <T> runApi(block: () -> T): Result<T> = runCatching {
        try {
            block()
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val msg = extractMessage(errorBody) ?: "Đã có lỗi xảy ra (mã ${e.code()})"
            throw Exception(msg)
        } catch (e: java.net.SocketTimeoutException) {
            throw Exception("Máy chủ phản hồi quá lâu, vui lòng thử lại")
        } catch (e: java.io.IOException) {
            throw Exception("Không kết nối được máy chủ, vui lòng kiểm tra mạng và thử lại")
        }
    }

    private fun extractMessage(errorBody: String?): String? {
        if (errorBody == null) return null
        return try {
            org.json.JSONObject(errorBody).optString("message").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun login(email: String, password: String): Result<LoginResponse> = runApi {
        api.login(LoginRequest(email, password))
    }

    suspend fun register(email: String, password: String, fullName: String): Result<Unit> = runApi {
        val response = api.register(RegisterRequest(email, password, fullName))
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            val msg = extractMessage(errorBody) ?: "Đã có lỗi xảy ra (mã ${response.code()})"
            throw Exception(msg)
        }
    }

    suspend fun verifyEmail(email: String, token: String): Result<Unit> = runApi {
        val response = api.verifyEmail(VerifyRequest(email, token))
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            val msg = extractMessage(errorBody) ?: "Đã có lỗi xảy ra (mã ${response.code()})"
            throw Exception(msg)
        }
    }

    suspend fun logout(): Result<Unit> = runApi {
        val response = api.logout()
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            val msg = extractMessage(errorBody) ?: "Đã có lỗi xảy ra (mã ${response.code()})"
            throw Exception(msg)
        }
    }

    suspend fun saveSession(token: String, userId: String, fullName: String, isAdmin: Boolean) {
        tokenStore.save(token, userId, fullName, isAdmin)
    }

    suspend fun clearSession() {
        tokenStore.clear()
    }

    // Products

    suspend fun searchProducts(
        keyword: String?,
        category: String?,
        page: Int? = null,
        size: Int? = null
    ): Result<PageResponse<ProductSummary>> = runApi {
        api.searchProducts(keyword, category, page, size)
    }

    suspend fun getProduct(id: String): Result<ProductDetail> = runApi {
        api.getProduct(id)
    }

    suspend fun registerProduct(request: CreateProductRequest): Result<ProductDetail> = runApi {
        api.registerProduct(request)
    }

    suspend fun updateProduct(id: String, request: CreateProductRequest): Result<ProductDetail> = runApi {
        api.updateProduct(id, request)
    }

    suspend fun uploadProductImage(
        productId: String,
        file: MultipartBody.Part
    ): Result<ProductDetail> = runApi {
        api.uploadProductImage(productId, file)
    }

    suspend fun myProducts(status: String?): Result<PageResponse<ProductSummary>> = runApi {
        api.myProducts(status)
    }

    // Bidding

    suspend fun placeBid(productId: String, amount: BigDecimal): Result<BidResponse> = runApi {
        api.placeBid(productId, PlaceBidRequest(amount))
    }

    suspend fun bidHistory(productId: String): Result<PageResponse<BidResponse>> = runApi {
        api.bidHistory(productId)
    }

    suspend fun participating(): Result<PageResponse<AuctionStateResponse>> = runApi {
        api.participating()
    }

    suspend fun won(): Result<PageResponse<AuctionStateResponse>> = runApi {
        api.won()
    }

    // Admin

    suspend fun getPendingProducts(): Result<PageResponse<ProductSummary>> = runApi {
        api.getPendingProducts()
    }

    suspend fun getReviewedProducts(result: String): Result<PageResponse<ProductSummary>> = runApi {
        api.getReviewedProducts(result)
    }

    suspend fun approveProduct(id: String): Result<Unit> = runApi {
        val response = api.approveProduct(id)
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            val msg = extractMessage(errorBody) ?: "Đã có lỗi xảy ra (mã ${response.code()})"
            throw Exception(msg)
        }
    }

    suspend fun rejectProduct(id: String, reason: String): Result<Unit> = runApi {
        val response = api.rejectProduct(id, RejectProductRequest(reason.trim()))
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            val msg = extractMessage(errorBody) ?: "Đã có lỗi xảy ra (mã ${response.code()})"
            throw Exception(msg)
        }
    }

    suspend fun getProfile(): Result<UserResponse> = runApi {
        api.getProfile()
    }

    // Categories

    suspend fun getCategories(): Result<List<CategoryDto>> = runApi {
        api.getCategories()
    }
}
