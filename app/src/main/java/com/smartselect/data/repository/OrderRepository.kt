package com.smartselect.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.smartselect.data.model.Order
import com.smartselect.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

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
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Error"))
                    return@addSnapshotListener
                }
                val orders = snapshot?.toObjects(Order::class.java) ?: emptyList()
                trySend(Resource.Success(orders))
            }
        awaitClose { listener.remove() }
    }

    fun getAllOrders(): Flow<Resource<List<Order>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = ordersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.message ?: "Error"))
                return@addSnapshotListener
            }
            val orders = snapshot?.toObjects(Order::class.java) ?: emptyList()
            trySend(Resource.Success(orders))
        }
        awaitClose { listener.remove() }
    }

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

    // Stock management functions
    suspend fun decreaseOrderStock(order: Order): Resource<Boolean> {
        return try {
            val batch = firestore.batch()
            order.phoneIds.zip(order.phoneQuantities).forEach { (phoneId, quantity) ->
                val phoneRef = firestore.collection("phones").document(phoneId)
                batch.update(phoneRef, "stock", com.google.firebase.firestore.FieldValue.increment(-quantity.toLong()))
            }
            batch.commit().await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update stock")
        }
    }

    suspend fun restoreOrderStock(order: Order): Resource<Boolean> {
        return try {
            val batch = firestore.batch()
            order.phoneIds.zip(order.phoneQuantities).forEach { (phoneId, quantity) ->
                val phoneRef = firestore.collection("phones").document(phoneId)
                batch.update(phoneRef, "stock", com.google.firebase.firestore.FieldValue.increment(quantity.toLong()))
            }
            batch.commit().await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to restore stock")
        }
    }
}