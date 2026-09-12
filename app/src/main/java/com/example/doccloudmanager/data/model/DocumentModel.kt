package com.example.doccloudmanager.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class DocumentModel(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val fileUrl: String = "",
    val storagePath: String = "",
    val fileType: String = "TXT", // تم التغيير إلى String لمنع كراش Firestore
    val sizeBytes: Long = 0L,
    val tags: List<String> = emptyList(),
    val summary: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
)
