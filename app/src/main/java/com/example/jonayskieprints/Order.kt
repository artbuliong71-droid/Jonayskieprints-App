package com.example.jonayskieprints

data class Order(
    val id: String,
    val serviceType: String,
    val status: String, // e.g., "Pending", "Printing", "Completed"
    val date: String,
    val price: Double,
    val fileName: String
)
