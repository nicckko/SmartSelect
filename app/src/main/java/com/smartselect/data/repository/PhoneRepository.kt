package com.smartselect.data.repository

import com.google.firebase.firestore.FirebaseFirestore
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
            val phones = snapshot?.toObjects(Phone::class.java) ?: emptyList()
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
            var phones = snapshot?.toObjects(Phone::class.java) ?: emptyList()
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

    suspend fun checkIfExists(brand: String, model: String): Boolean {
        return try {
            val querySnapshot = phonesCollection
                .whereEqualTo("brand", brand)
                .whereEqualTo("model", model)
                .get().await()
            !querySnapshot.isEmpty
        } catch (e: Exception) {
            false
        }
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
            phonesCollection.document(id).delete().await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

}