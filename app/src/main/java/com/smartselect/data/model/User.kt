package com.smartselect.data.model

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    @DocumentId
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val email: String = "",
    val role: String = "customer",
    val address: String = "",
    val contact: String = "",
    val profilePictureUrl: String = ""
) : Parcelable

// Extension functions for backward compatibility
fun User.getFullName(): String = "${this.firstName} ${this.lastName}".trim()
fun User.getName(): String = getFullName()
fun User.getUid(): String = this.id

// Keep Order and CartItem as they were
data class Order(
    @DocumentId
    val orderId: String = "",
    val userId: String = "",
    val phoneIds: List<String> = emptyList(),
    val phoneQuantities: List<Int> = emptyList(), // NEW: quantity for each phone
    val phoneNames: List<String> = emptyList(),
    val totalPrice: Double = 0.0,
    val status: String = "pending",
    val timestamp: Timestamp? = null,
    val userName: String = "",
    val contact: String = "",
    val pickupDate: Timestamp? = null,
    val pickupCode: String = ""
)

data class CartItem(
    val phone: Phone,
    val quantity: Int = 1
)