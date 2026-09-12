package com.example.doccloudmanager.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class NoteModel(
    val id: String = "",
    val documentId: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val pageIndex: Int? = null, // رقم الصفحة المعنية بالملاحظة إن وُجدت
    @ServerTimestamp
    val createdAt: Date? = null
)
