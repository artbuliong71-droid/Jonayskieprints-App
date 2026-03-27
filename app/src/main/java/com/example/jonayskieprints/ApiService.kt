package com.example.jonayskieprints

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("api/mobile/orders")
    suspend fun getUserOrders(
        @Header("Authorization") token: String,
        @Query("status") status: String? = null
    ): Response<MobileOrdersResponse>

    @GET("api/admin/orders")
    suspend fun getAdminOrders(
        @Query("status") status: String? = null
    ): Response<AdminOrdersResponse>

    @Multipart
    @PATCH("api/admin/orders")
    suspend fun updateOrderStatus(
        @Part("order_id") orderId: RequestBody,
        @Part("status") status: RequestBody
    ): Response<SimpleResponse>

    @GET("api/admin/stats")
    suspend fun getAdminStats(
        @Query("period") period: String? = null,
        @Query("date") date: String? = null
    ): Response<AdminStatsResponse>
}

// Mobile
data class MobileOrdersResponse(
    val success: Boolean,
    val data: List<MobileOrder>?
)

data class MobileOrder(
    val order_id: String?,
    val service: String?,
    val serviceType: String?,
    val quantity: Int,
    val specifications: String?,
    val deliveryOption: String?,
    val pickupTime: String?,
    val status: String?,
    val paymentMethod: String?,
    val totalAmount: String?,
    val price: Double,
    val createdAt: String?,
    val date: String?,
    val fileName: String?
)

// Admin Orders
data class AdminOrdersResponse(
    val success: Boolean,
    val data: List<AdminOrder>?
)

data class AdminOrder(
    val _id: String?,
    val order_id: String?,
    val user_name: String?,
    val user_email: String?,
    val service: String?,
    val quantity: Int = 1,
    val specifications: String?,
    val delivery_option: String?,
    val delivery_address: String?,
    val pickup_time: String?,
    val status: String?,
    val payment_method: String?,
    val total_amount: String?,
    val created_at: String?,
    val updated_at: String?,
    val files: List<AdminOrderFile>?
)

data class AdminOrderFile(
    val url: String?,
    val resource_type: String?
)