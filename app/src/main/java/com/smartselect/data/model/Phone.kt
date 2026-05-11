package com.smartselect.data.model

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class Phone(
    @DocumentId
    val id: String = "",
    val brand: String = "",
    val model: String = "",
    val price: Double = 0.0,
    val storage: String = "",
    val ram: String = "",
    val camera: String = "",
    val battery: String = "",
    val chipset: String = "",
    val display: String = "",
    val imageUrl: String = "",
    val category: String = "",
    val stock: Int = 10,
    val isBestValue: Boolean = false,
    @get:com.google.firebase.firestore.PropertyName("isDeleted")
    @set:com.google.firebase.firestore.PropertyName("isDeleted")
    var isDeleted: Boolean = false
) : Parcelable