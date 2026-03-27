package com.example.jonayskieprints

import com.google.gson.annotations.SerializedName

data class AdminStatsResponse(
    val success: Boolean,
    val data: AdminStats,
    val chartData: List<ChartPoint>?
)

data class AdminStats(
    val totalOrders: Int,
    val pendingOrders: Int,
    val totalCustomers: Int,
    @SerializedName("totalRevenue")
    val totalSales: String,
    val inProgressOrders: Int,
    val completedOrders: Int,
    val cancelledOrders: Int
)

data class ChartPoint(
    val label: String,
    val Revenue: Double,
    val Orders: Int
)

data class SimpleResponse(
    val success: Boolean,
    val message: String
)