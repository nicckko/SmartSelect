package com.smartselect.utils

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.smartselect.R

object GlideImageLoader {

    fun loadImage(context: Context, imageUrl: String, imageView: ImageView) {
        if (imageUrl.isEmpty()) {
            Glide.with(context)
                .load(R.drawable.placeholder_phone)
                .into(imageView)
            return
        }

        Glide.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.placeholder_phone)
            .error(R.drawable.placeholder_phone)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(imageView)
    }
}