package com.example.doccloudmanager.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ChapterModel(
    val id: String = "",
    val documentId: String = "",
    val chapterTitle: String = "",
    val contentText: String = "",
    val startPage: Int = 1,
    val endPage: Int = 1,
    @ServerTimestamp
    val createdAt: Date? = null
)
