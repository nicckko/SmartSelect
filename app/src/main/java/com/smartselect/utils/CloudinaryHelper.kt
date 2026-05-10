package com.smartselect.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.UUID

object CloudinaryHelper {

    private const val TAG = "CloudinaryHelper"

    // !!! IMPORTANT: REPLACE WITH YOUR CLOUDINARY CLOUD NAME !!!
    // Get this from your Cloudinary dashboard after signing up at https://cloudinary.com
    private const val CLOUD_NAME = "ddcxny88s"

    // Create this upload preset in Cloudinary: Settings → Upload → Upload Presets → Add Upload Preset
    // Name it "smartselect_phones" and set Mode to "Unsigned"
    private const val UPLOAD_PRESET = "smartselect_phones"

    private var isInitialized = false

    fun initialize(context: Context) {
        if (!isInitialized) {
            val config = hashMapOf(
                "cloud_name" to CLOUD_NAME,
                "secure" to true
            )
            MediaManager.init(context, config)
            isInitialized = true
            Log.d(TAG, "Cloudinary initialized with cloud name: $CLOUD_NAME")
        }
    }

    suspend fun uploadImage(uri: Uri): String = suspendCancellableCoroutine { continuation ->
        Log.d(TAG, "Starting upload for URI: $uri")

        MediaManager.get().upload(uri)
            .unsigned(UPLOAD_PRESET)
            .option("public_id", "phones/${UUID.randomUUID()}")
            .option("folder", "smartselect")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    Log.d(TAG, "Upload started: $requestId")
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val progress = (bytes * 100 / totalBytes).toInt()
                    Log.d(TAG, "Upload progress: $progress%")
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as? String
                    if (url != null) {
                        Log.d(TAG, "Upload success! URL: $url")
                        continuation.resume(url)
                    } else {
                        Log.e(TAG, "No URL returned from Cloudinary")
                        continuation.resumeWithException(Exception("No URL returned from Cloudinary"))
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    Log.e(TAG, "Upload error: ${error.description}")
                    continuation.resumeWithException(Exception(error.description))
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    Log.e(TAG, "Upload rescheduled: ${error.description}")
                }
            })
            .dispatch()
    }
}