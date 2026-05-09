package com.smartselect.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp

data class AdminLog(
    @DocumentId
    val logId: String = "",
    val adminName: String = "",
    val action: String = "",
    val details: String = "",
    val timestamp: Timestamp? = null
)
