package com.smartselect.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.smartselect.data.model.AdminLog
import com.smartselect.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminLogRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val logsCollection = firestore.collection("admin_logs")

    suspend fun logAction(action: String, details: String) {
        try {
            val user = auth.currentUser
            val name = user?.displayName ?: "Unknown Admin"
            
            val log = AdminLog(
                adminName = name,
                action = action,
                details = details,
                timestamp = Timestamp.now()
            )
            logsCollection.add(log).await()
        } catch (e: Exception) {
            // Silently fail if logging fails to not disrupt main flow
        }
    }

    suspend fun deleteLog(logId: String): Resource<Boolean> {
        return try {
            logsCollection.document(logId).delete().await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete log")
        }
    }

    fun getLogs(): Flow<Resource<List<AdminLog>>> = callbackFlow {
        trySend(Resource.Loading())
        val listener = logsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.message ?: "Error fetching logs"))
                return@addSnapshotListener
            }
            val logs = snapshot?.toObjects(AdminLog::class.java) ?: emptyList()
            // Sort by latest first
            val sortedLogs = logs.sortedByDescending { it.timestamp?.seconds ?: 0L }
            trySend(Resource.Success(sortedLogs))
        }
        awaitClose { listener.remove() }
    }
}
