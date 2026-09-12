package com.example.doccloudmanager.data.repository

import android.net.Uri
import com.example.doccloudmanager.data.model.ChapterModel
import com.example.doccloudmanager.data.model.DocumentModel
import com.example.doccloudmanager.data.model.FileType
import com.example.doccloudmanager.data.model.NoteModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseRepository {

    // التعديل الجوهري: استخدام get() يمنع كراش التشغيل الأول عند غياب التهيئة
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage get() = FirebaseStorage.getInstance()
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    private val currentUserId: String
        get() = runCatching { auth.currentUser?.uid }.getOrNull() ?: "guest_user"

    suspend fun uploadDocument(
        fileUri: Uri,
        title: String,
        fileType: FileType,
        sizeBytes: Long,
        tags: List<String>
    ): Result<DocumentModel> = runCatching {
        val docId = UUID.randomUUID().toString()
        val extension = if (fileType == FileType.PDF) "pdf" else "txt"
        val storagePath = "users/$currentUserId/documents/$docId.$extension"
        val storageRef = storage.reference.child(storagePath)

        storageRef.putFile(fileUri).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()

        val newDoc = DocumentModel(
            id = docId,
            userId = currentUserId,
            title = title,
            fileUrl = downloadUrl,
            storagePath = storagePath,
            fileType = fileType.name,
            sizeBytes = sizeBytes,
            tags = tags
        )

        firestore.collection("documents")
            .document(docId)
            .set(newDoc)
            .await()

        newDoc
    }

    suspend fun createTextDocument(
        title: String,
        content: String,
        tags: List<String>
    ): Result<DocumentModel> = runCatching {
        val docId = UUID.randomUUID().toString()
        val storagePath = "users/$currentUserId/documents/$docId.txt"
        val storageRef = storage.reference.child(storagePath)

        val bytes = content.toByteArray(Charsets.UTF_8)
        storageRef.putBytes(bytes).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()

        val newDoc = DocumentModel(
            id = docId,
            userId = currentUserId,
            title = title,
            fileUrl = downloadUrl,
            storagePath = storagePath,
            fileType = "TXT",
            sizeBytes = bytes.size.toLong(),
            tags = tags
        )

        firestore.collection("documents")
            .document(docId)
            .set(newDoc)
            .await()

        newDoc
    }

    fun getDocumentsFlow(searchQuery: String = ""): Flow<List<DocumentModel>> = callbackFlow {
        val listener = try {
            firestore.collection("documents")
                .whereEqualTo("userId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val docs = snapshot.toObjects(DocumentModel::class.java)
                        val filteredDocs = if (searchQuery.isBlank()) {
                            docs
                        } else {
                            docs.filter { doc ->
                                doc.title.contains(searchQuery, ignoreCase = true) ||
                                        doc.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) } ||
                                        doc.summary.contains(searchQuery, ignoreCase = true)
                            }
                        }
                        trySend(filteredDocs)
                    }
                }
        } catch (e: Exception) {
            trySend(emptyList())
            null
        }
        awaitClose { listener?.remove() }
    }

    suspend fun addNote(
        documentId: String,
        title: String,
        content: String,
        pageIndex: Int? = null
    ): Result<Unit> = runCatching {
        val noteId = UUID.randomUUID().toString()
        val note = NoteModel(
            id = noteId,
            documentId = documentId,
            userId = currentUserId,
            title = title,
            content = content,
            pageIndex = pageIndex
        )
        firestore.collection("documents")
            .document(documentId)
            .collection("notes")
            .document(noteId)
            .set(note)
            .await()
    }

    fun getNotesFlow(documentId: String): Flow<List<NoteModel>> = callbackFlow {
        val listener = try {
            firestore.collection("documents")
                .document(documentId)
                .collection("notes")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        trySend(snapshot.toObjects(NoteModel::class.java))
                    }
                }
        } catch (e: Exception) {
            trySend(emptyList())
            null
        }
        awaitClose { listener?.remove() }
    }

    suspend fun updateSummary(documentId: String, summary: String): Result<Unit> = runCatching {
        firestore.collection("documents")
            .document(documentId)
            .update("summary", summary)
            .await()
    }

    suspend fun saveExtractedChapter(chapter: ChapterModel): Result<Unit> = runCatching {
        val chapterId = if (chapter.id.isBlank()) UUID.randomUUID().toString() else chapter.id
        val finalChapter = chapter.copy(id = chapterId)
        firestore.collection("documents")
            .document(chapter.documentId)
            .collection("chapters")
            .document(chapterId)
            .set(finalChapter)
            .await()
    }

    suspend fun deleteDocument(doc: DocumentModel): Result<Unit> = runCatching {
        if (doc.storagePath.isNotBlank()) {
            runCatching { storage.reference.child(doc.storagePath).delete().await() }
        }
        firestore.collection("documents").document(doc.id).delete().await()
    }
}
