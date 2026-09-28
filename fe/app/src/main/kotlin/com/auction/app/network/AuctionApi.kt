package com.auction.app.network

import com.auction.app.network.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

// Mỗi hàm map 1-1 với route trên api-gateway — xem be/api-gateway/src/main/resources/application.yml
interface AuctionApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("auth/verify")
    suspend fun verifyEmail(@Body request: VerifyRequest): Response<Unit>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    // Product search (Auction). page/size theo chuẩn Spring Data Pageable (page bắt đầu từ 0) —
    // backend (catalog-service) đã hỗ trợ sẵn 2 param này tự động, không cần đổi gì phía BE.
    @GET("products")
    suspend fun searchProducts(
        @Query("keyword") keyword: String?,
        @Query("category") category: String?,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null
    ): PageResponse<ProductSummary>

    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: String): ProductDetail

    @POST("products")
    suspend fun registerProduct(@Body request: CreateProductRequest): ProductDetail

    @PUT("products/{id}")
    suspend fun updateProduct(@Path("id") id: String, @Body request: CreateProductRequest): ProductDetail

    // Upload/thay ảnh sản phẩm — multipart/form-data, field "file". Chỉ gọi được sau khi đã có
    // productId (tức là sau registerProduct), khớp endpoint POST products/{id}/image ở catalog-service.
    @Multipart
    @POST("products/{id}/image")
    suspend fun uploadProductImage(
        @Path("id") productId: String,
        @Part file: MultipartBody.Part
    ): ProductDetail

    @GET("my/products")
    suspend fun myProducts(@Query("status") status: String?): PageResponse<ProductSummary>

    // Bidding
    @POST("products/{id}/bids")
    suspend fun placeBid(@Path("id") productId: String, @Body request: PlaceBidRequest): BidResponse

    @GET("products/{id}/bids")
    suspend fun bidHistory(@Path("id") productId: String): PageResponse<BidResponse>

    @GET("my/bids/participating")
    suspend fun participating(): PageResponse<AuctionStateResponse>

    @GET("my/bids/won")
    suspend fun won(): PageResponse<AuctionStateResponse>

    // Admin API
    @GET("admin/products/pending")
    suspend fun getPendingProducts(): PageResponse<ProductSummary>

    // result = "APPROVED" | "REJECTED"
    @GET("admin/products/reviewed")
    suspend fun getReviewedProducts(
        @Query("result") result: String,
        @Query("size") size: Int = 100,
        @Query("sort") sort: String = "updatedAt,desc"
    ): PageResponse<ProductSummary>

    @POST("admin/products/{id}/approve")
    suspend fun approveProduct(@Path("id") id: String): Response<Unit>

    @POST("admin/products/{id}/reject")
    suspend fun rejectProduct(@Path("id") id: String, @Body body: RejectProductRequest): Response<Unit>

    @GET("users/me")
    suspend fun getProfile(): UserResponse

    // Category APIs
    @GET("categories")
    suspend fun getCategories(): List<CategoryDto>
}
