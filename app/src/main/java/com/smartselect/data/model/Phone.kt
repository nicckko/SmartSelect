package com.smartselect.data.model

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class Phone(
    @DocumentId
    val id: String = "",

    // Basic Info
    val brand: String = "",
    val model: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val stock: Int = 10,
    val isBestValue: Boolean = false,
    val imageUrl: String = "",

    // Core Specs
    val storage: String = "",
    val ram: String = "",
    val camera: String = "",
    val battery: String = "",
    val chipset: String = "",
    val display: String = "",

    // NEW SPECS (GSMArena)
    val os: String = "",              // Android 14, iOS 18, etc.
    val network: String = "",         // 5G, 4G LTE, etc.
    val weight: String = "",          // "187g"
    val dimensions: String = "",      // "160.9 x 75.9 x 8.2 mm"
    val build: String = "",           // Glass front/back, aluminum frame
    val protection: String = "",      // Gorilla Glass Victus 2, IP68
    val gpu: String = "",             // GPU model
    val charging: String = "",        // "45W wired, 15W wireless"
    val sensors: String = "",         // "Fingerprint, accelerometer, gyro"
    val colors: String = "",          // "Black, White, Blue"
    val releaseDate: String = "",     // "2024-01-01"

    @get:com.google.firebase.firestore.PropertyName("isDeleted")
    @set:com.google.firebase.firestore.PropertyName("isDeleted")
    var isDeleted: Boolean = false
) : Parcelable