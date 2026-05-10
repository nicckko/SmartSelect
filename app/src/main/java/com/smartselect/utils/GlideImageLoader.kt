package com.smartselect.utils

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.google.firebase.auth.FirebaseAuth
import com.smartselect.R

object GlideImageLoader {

    private fun getAuthenticatedGlideUrl(url: String): GlideUrl {
        val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.result?.token ?: ""
        return GlideUrl(
            url,
            LazyHeaders.Builder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        )
    }

    fun loadImage(context: Context, imageUrl: String, imageView: ImageView) {
        if (imageUrl.isEmpty()) {
            Glide.with(context)
                .load(R.drawable.placeholder_phone)
                .into(imageView)
            return
        }

        try {
            val glideUrl = getAuthenticatedGlideUrl(imageUrl)
            Glide.with(context)
                .load(glideUrl)
                .placeholder(R.drawable.placeholder_phone)
                .error(R.drawable.placeholder_phone)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView)
        } catch (e: Exception) {
            // Fallback to regular load if token fails
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_phone)
                .error(R.drawable.placeholder_phone)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView)
        }
    }
}