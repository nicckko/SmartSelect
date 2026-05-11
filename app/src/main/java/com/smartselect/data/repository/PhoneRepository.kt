package com.smartselect.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.smartselect.data.model.Phone
import com.smartselect.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val phonesCollection = firestore.collection("phones")

    fun getPhones(): Flow<Resource<List<Phone>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = phonesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.message ?: "Unknown error"))
                return@addSnapshotListener
            }
            val phones = snapshot?.toObjects(Phone::class.java)?.filter { !it.isDeleted } ?: emptyList()
            trySend(Resource.Success(phones))
        }
        awaitClose { listener.remove() }
    }

    fun searchPhones(
        query: String,
        category: String = "",
        minPrice: Double = 0.0,
        maxPrice: Double = Double.MAX_VALUE
    ): Flow<Resource<List<Phone>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = phonesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.message ?: "Unknown error"))
                return@addSnapshotListener
            }
            var phones = snapshot?.toObjects(Phone::class.java)?.filter { !it.isDeleted } ?: emptyList()
            if (query.isNotEmpty()) {
                phones = phones.filter {
                    it.brand.contains(query, true) || it.model.contains(query, true)
                }
            }
            if (category.isNotEmpty()) phones = phones.filter { it.category == category }
            phones = phones.filter { it.price in minPrice..maxPrice }
            trySend(Resource.Success(phones))
        }
        awaitClose { listener.remove() }
    }

    suspend fun getPhoneById(id: String): Resource<Phone> {
        return try {
            val doc = phonesCollection.document(id).get().await()
            val phone = doc.toObject(Phone::class.java)
            if (phone != null) Resource.Success(phone) else Resource.Error("Phone not found")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun isDuplicate(brand: String, model: String, excludeId: String? = null): Boolean {
        return try {
            val snapshot = phonesCollection
                .whereEqualTo("brand", brand)
                .whereEqualTo("model", model)
                .get().await()
            if (excludeId != null) {
                snapshot.documents.any { it.id != excludeId }
            } else {
                !snapshot.isEmpty
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun addPhone(phone: Phone): Resource<Boolean> {
        return try {
            val docRef = phonesCollection.document()
            val phoneWithId = phone.copy(id = docRef.id)
            docRef.set(phoneWithId).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun updatePhone(phone: Phone): Resource<Boolean> {
        return try {
            phonesCollection.document(phone.id).set(phone).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun deletePhone(id: String): Resource<Boolean> {
        return try {
            if (id.isBlank()) return Resource.Error("Invalid phone ID")
            phonesCollection.document(id)
                .set(mapOf("isDeleted" to true), com.google.firebase.firestore.SetOptions.merge())
                .await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun restorePhone(id: String): Resource<Boolean> {
        return try {
            if (id.isBlank()) return Resource.Error("Invalid phone ID")
            phonesCollection.document(id)
                .set(mapOf("isDeleted" to false), com.google.firebase.firestore.SetOptions.merge())
                .await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun permanentlyDeletePhone(id: String): Resource<Boolean> {
        return try {
            if (id.isBlank()) return Resource.Error("Invalid phone ID")
            phonesCollection.document(id).delete().await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    fun getDeletedPhones(): Flow<Resource<List<Phone>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = phonesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.message ?: "Unknown error"))
                return@addSnapshotListener
            }
            val phones = snapshot?.toObjects(Phone::class.java)?.filter { it.isDeleted } ?: emptyList()
            trySend(Resource.Success(phones))
        }
        awaitClose { listener.remove() }
    }

    // Add these to PhoneRepository class

    suspend fun checkStockAvailability(phoneId: String, requestedQuantity: Int): Boolean {
        return try {
            val doc = phonesCollection.document(phoneId).get().await()
            val phone = doc.toObject(Phone::class.java)
            phone != null && phone.stock >= requestedQuantity
        } catch (e: Exception) {
            false
        }
    }

    suspend fun decreaseStock(phoneId: String, quantity: Int): Resource<Boolean> {
        return try {
            val docRef = phonesCollection.document(phoneId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentStock = snapshot.getLong("stock") ?: 0
                val newStock = currentStock - quantity

                if (newStock < 0) {
                    throw Exception("Insufficient stock")
                }

                transaction.update(docRef, "stock", newStock)
            }.await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update stock")
        }
    }

    suspend fun increaseStock(phoneId: String, quantity: Int): Resource<Boolean> {
        return try {
            val docRef = phonesCollection.document(phoneId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentStock = snapshot.getLong("stock") ?: 0
                transaction.update(docRef, "stock", currentStock + quantity)
            }.await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to restore stock")
        }
    }

    suspend fun decreaseMultipleStocks(items: List<Pair<String, Int>>): Resource<Boolean> {
        return try {
            val batch = firestore.batch()
            items.forEach { (phoneId, quantity) ->
                val docRef = phonesCollection.document(phoneId)
                batch.update(docRef, "stock", com.google.firebase.firestore.FieldValue.increment(-quantity.toLong()))
            }
            batch.commit().await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update stocks")
        }
    }
}