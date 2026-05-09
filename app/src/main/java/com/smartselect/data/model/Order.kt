package com.smartselect.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp

data class User(
    @DocumentId
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val role: String = "user", // "admin" or "user"
    val address: String = "",
    val contact: String = "",
    val profilePictureUrl: String = ""
)

data class Order(
    @DocumentId
    val orderId: String = "",
    val userId: String = "",
    val phoneIds: List<String> = emptyList(),
    val phoneNames: List<String> = emptyList(),
    val totalPrice: Double = 0.0,
    // Statuses: pending, confirmed, picked_up, cancelled
    val status: String = "pending",
    val timestamp: Timestamp? = null,
    val userName: String = "",
    val contact: String = "",
    // Pickup-specific fields
    val pickupDate: Timestamp? = null,   // Date the customer will pick up
    val pickupCode: String = ""          // Unique 6-char code shown to customer
)

// Fixed: data class so copy() works for immutable quantity updates
data class CartItem(
    val phone: Phone,
    val quantity: Int = 1
)