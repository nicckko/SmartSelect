package com.smartselect.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.smartselect.data.model.Order
import com.smartselect.data.model.User
import com.smartselect.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")

    fun getCurrentUser() = auth.currentUser

    suspend fun login(email: String, password: String): Resource<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Resource.Error("Login failed")
            val userDoc = usersCollection.document(uid).get().await()
            val user = userDoc.toObject(User::class.java) ?: User(uid = uid, email = email, name = email)
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Login failed")
        }
    }

    suspend fun register(name: String, email: String, password: String): Resource<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Resource.Error("Registration failed")
            val user = User(uid = uid, name = name, email = email, role = "user")
            usersCollection.document(uid).set(user).await()
            
            // Sign out immediately so it doesn't log them in automatically
            auth.signOut()
            
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Registration failed")
        }
    }

    /**
     * Creates the admin account. Call this ONCE from the app to set up admin.
     * Admin credentials: admin@smartselect.com / SmartAdmin2024!
     */
    suspend fun createAdminAccount(): Resource<User> {
        return try {
            val adminEmail = "admin@smartselect.com"
            val adminPassword = "SmartAdmin2024!"
            val adminName = "Admin"

            val result = auth.createUserWithEmailAndPassword(adminEmail, adminPassword).await()
            val uid = result.user?.uid ?: return Resource.Error("Admin creation failed")
            val user = User(uid = uid, name = adminName, email = adminEmail, role = "admin")
            usersCollection.document(uid).set(user).await()

            // Sign out immediately after creating admin (so current session isn't affected)
            auth.signOut()

            Resource.Success(user)
        } catch (e: Exception) {
            // If already exists, it's fine
            Resource.Error(e.message ?: "Admin creation failed")
        }
    }

    suspend fun getCurrentUserData(): Resource<User> {
        return try {
            val uid = auth.currentUser?.uid ?: return Resource.Error("Not logged in")
            val doc = usersCollection.document(uid).get().await()
            val user = doc.toObject(User::class.java) ?: return Resource.Error("User not found")
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error fetching user")
        }
    }

    suspend fun updateProfile(newName: String, newUsername: String, newPassword: String?, newProfileUrl: String?): Resource<User> {
        return try {
            val uid = auth.currentUser?.uid ?: return Resource.Error("Not logged in")
            
            // Update auth profile name and photo
            val profileUpdatesBuilder = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
            
            if (newProfileUrl != null && newProfileUrl.isNotEmpty()) {
                profileUpdatesBuilder.setPhotoUri(android.net.Uri.parse(newProfileUrl))
            }
            
            auth.currentUser?.updateProfile(profileUpdatesBuilder.build())?.await()
            
            // Update password if provided
            if (newPassword != null && newPassword.isNotEmpty()) {
                auth.currentUser?.updatePassword(newPassword)?.await()
            }

            // Update firestore user document
            val updates = mutableMapOf<String, Any>(
                "name" to newName,
                "username" to newUsername
            )
            if (newProfileUrl != null && newProfileUrl.isNotEmpty()) {
                updates["profilePictureUrl"] = newProfileUrl
            }
            usersCollection.document(uid).update(updates).await()
            
            val doc = usersCollection.document(uid).get().await()
            val user = doc.toObject(User::class.java) ?: return Resource.Error("User not found after update")
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update profile")
        }
    }

    fun logout() = auth.signOut()
}

@Singleton
class OrderRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val ordersCollection = firestore.collection("orders")

    suspend fun placeOrder(order: Order): Resource<Boolean> {
        return try {
            ordersCollection.add(order).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Order failed")
        }
    }

    suspend fun deleteOrder(orderId: String): Resource<Boolean> {
        return try {
            ordersCollection.document(orderId).delete().await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Delete failed")
        }
    }

    /**
     * Auto-cancels any orders whose pickupDate is in the past
     * and whose status is still "pending" or "confirmed".
     */
    suspend fun autoCancel(orderId: String): Resource<Boolean> {
        return try {
            ordersCollection.document(orderId).update("status", "cancelled").await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Auto-cancel failed")
        }
    }

    fun getUserOrders(): Flow<Resource<List<Order>>> = callbackFlow {
        trySend(Resource.Loading())
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Not logged in"))
            close()
            return@callbackFlow
        }
        val listener = ordersCollection
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(Resource.Error(error.message ?: "Error")); return@addSnapshotListener }
                val orders = snapshot?.toObjects(Order::class.java) ?: emptyList()
                trySend(Resource.Success(orders))
            }
        awaitClose { listener.remove() }
    }

    fun getAllOrders(): Flow<Resource<List<Order>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = ordersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) { trySend(Resource.Error(error.message ?: "Error")); return@addSnapshotListener }
            val orders = snapshot?.toObjects(Order::class.java) ?: emptyList()
            trySend(Resource.Success(orders))
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateOrderStatus(orderId: String, status: String): Resource<Boolean> {
        return try {
            ordersCollection.document(orderId).update("status", status).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Update failed")
        }
    }

    suspend fun updateOrderPickupDate(orderId: String, newDate: Timestamp): Resource<Boolean> {
        return try {
            ordersCollection.document(orderId).update("pickupDate", newDate).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Update failed")
        }
    }

    /** Checks all active orders and cancels those whose pickup date has passed. */
    suspend fun checkAndCancelExpiredOrders(orders: List<Order>) {
        val now = Date()
        orders.forEach { order ->
            val pickupDate = order.pickupDate?.toDate()
            if (pickupDate != null
                && pickupDate.before(now)
                && order.status != "picked_up"
                && order.status != "cancelled"
            ) {
                autoCancel(order.orderId)
            }
        }
    }
}