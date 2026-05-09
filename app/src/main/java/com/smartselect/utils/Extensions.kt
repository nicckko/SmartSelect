package com.smartselect.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.smartselect.R
import java.text.NumberFormat
import java.util.Locale

fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun View.snackbar(message: String, action: String? = null, actionClick: (() -> Unit)? = null) {
    val snack = Snackbar.make(this, message, Snackbar.LENGTH_LONG)
    if (action != null && actionClick != null) snack.setAction(action) { actionClick() }
    snack.show()
}

fun Double.toPeso(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("fil", "PH"))
    return format.format(this)
}

fun Int.getStockLabel(): String = when {
    this == 0 -> "Out of Stock"
    this <= 3 -> "Low Stock"
    else -> "In Stock"
}

// Fixed: use R.color instead of android.R.color for proper color resource access
fun Int.getStockColor(): Int = when {
    this == 0 -> R.color.error
    this <= 3 -> R.color.warning
    else -> R.color.success
}

object Constants {
    const val PREF_FAVORITES = "pref_favorites"
    const val PREF_CART = "pref_cart"
    const val COLLECTION_PHONES = "phones"
    const val COLLECTION_USERS = "users"
    const val COLLECTION_ORDERS = "orders"
    const val ROLE_ADMIN = "admin"
    const val ROLE_USER = "user"
}
