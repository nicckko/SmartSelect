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

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    suspend fun login(identifier: String, password: String): Resource<User> {
        return try {
            // Determine if identifier is email or username
            val email = if (isValidEmail(identifier)) {
                identifier
            } else {
                // If not email, try to find user by username
                val userQuery = usersCollection.whereEqualTo("username", identifier).get().await()
                val userDoc = userQuery.documents.firstOrNull()
                if (userDoc == null) {
                    return Resource.Error("User not found")
                }
                userDoc.getString("email") ?: return Resource.Error("Email not found for this username")
            }

            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Resource.Error("Login failed")
            val userDoc = usersCollection.document(uid).get().await()
            val user = userDoc.toObject(User::class.java)
                ?: return Resource.Error("User data not found")
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Login failed. Please check your credentials.")
        }
    }

    suspend fun register(
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        password: String
    ): Resource<User> {
        return try {
            // Check if username already exists
            val usernameQuery = usersCollection.whereEqualTo("username", username).get().await()
            if (!usernameQuery.isEmpty) {
                return Resource.Error("Username already taken. Please choose another.")
            }

            // Check if email already exists
            val emailQuery = usersCollection.whereEqualTo("email", email).get().await()
            if (!emailQuery.isEmpty) {
                return Resource.Error("Email already registered. Please use another.")
            }

            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Resource.Error("Registration failed")

            val fullName = "$firstName $lastName"

            // Update auth profile with display name
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()
            result.user?.updateProfile(profileUpdates)?.await()

            val user = User(
                id = uid,
                firstName = firstName,
                lastName = lastName,
                username = username,
                email = email,
                role = "customer",
                address = "",
                contact = "",
                profilePictureUrl = ""
            )
            usersCollection.document(uid).set(user).await()

            // Sign out immediately so it doesn't log them in automatically
            auth.signOut()

            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Registration failed")
        }
    }

    // Old register method for compatibility (if needed elsewhere)
    suspend fun register(name: String, email: String, password: String): Resource<User> {
        val nameParts = name.split(" ", limit = 2)
        val firstName = nameParts[0]
        val lastName = if (nameParts.size > 1) nameParts[1] else ""
        val username = email.substringBefore("@")
        return register(firstName, lastName, username, email, password)
    }

    suspend fun createAdminAccount(): Resource<User> {
        return try {
            val adminEmail = "admin@smartselect.com"
            val adminPassword = "SmartAdmin2024!"

            // Check if admin already exists
            val existingUser = auth.fetchSignInMethodsForEmail(adminEmail).await().signInMethods?.isNotEmpty() == true
            if (existingUser) {
                return Resource.Error("Admin account already exists")
            }

            val result = auth.createUserWithEmailAndPassword(adminEmail, adminPassword).await()
            val uid = result.user?.uid ?: return Resource.Error("Admin creation failed")

            val user = User(
                id = uid,
                firstName = "Admin",
                lastName = "",
                username = "admin",
                email = adminEmail,
                role = "admin",
                address = "",
                contact = "",
                profilePictureUrl = ""
            )
            usersCollection.document(uid).set(user).await()

            // Sign out immediately after creating admin
            auth.signOut()

            Resource.Success(user)
        } catch (e: Exception) {
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

            // Split full name into first and last name
            val nameParts = newName.split(" ", limit = 2)
            val firstName = nameParts[0]
            val lastName = if (nameParts.size > 1) nameParts[1] else ""

            // Check if username is taken by another user
            if (newUsername.isNotEmpty()) {
                val usernameQuery = usersCollection
                    .whereEqualTo("username", newUsername)
                    .whereNotEqualTo("id", uid)
                    .get()
                    .await()
                if (!usernameQuery.isEmpty) {
                    return Resource.Error("Username already taken")
                }
            }

            // Update auth profile name
            val profileUpdatesBuilder = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(newName)

            if (newProfileUrl != null && newProfileUrl.isNotEmpty()) {
                profileUpdatesBuilder.setPhotoUri(android.net.Uri.parse(newProfileUrl))
            }

            auth.currentUser?.updateProfile(profileUpdatesBuilder.build())?.await()

            // Update password if provided
            if (newPassword != null && newPassword.isNotEmpty()) {
                if (newPassword.length < 6) {
                    return Resource.Error("Password must be at least 6 characters")
                }
                auth.currentUser?.updatePassword(newPassword)?.await()
            }

            // Update firestore user document
            val updates = mutableMapOf<String, Any>(
                "firstName" to firstName,
                "lastName" to lastName,
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