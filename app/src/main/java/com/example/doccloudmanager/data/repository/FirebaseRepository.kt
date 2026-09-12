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

class FirebaseRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: "guest_user"

    // 1. رفع ملف جديد إلى Cloud Storage مع حفظ بياناته في Firestore
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

        // رفع الملف
        storageRef.putFile(fileUri).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()

        val newDoc = DocumentModel(
            id = docId,
            userId = currentUserId,
            title = title,
            fileUrl = downloadUrl,
            storagePath = storagePath,
            fileType = fileType,
            sizeBytes = sizeBytes,
            tags = tags
        )

        // حفظ بيانات الوثيقة في Firestore
        firestore.collection("documents")
            .document(docId)
            .set(newDoc)
            .await()

        newDoc
    }

    // 2. إنشاء ملف نصي جديد مباشرة (TXT Editor)
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
            fileType = FileType.TXT,
            sizeBytes = bytes.size.toLong(),
            tags = tags
        )

        firestore.collection("documents")
            .document(docId)
            .set(newDoc)
            .await()

        newDoc
    }

    // 3. التدفّق اللحظي للوثائق مع دعم البحث بكلمة مفتاحية
    fun getDocumentsFlow(searchQuery: String = ""): Flow<List<DocumentModel>> = callbackFlow {
        var query = firestore.collection("documents")
            .whereEqualTo("userId", currentUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
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
        awaitClose { listener.remove() }
    }

    // 4. حفظ الملاحظات الخاصة بالوثيقة
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

    // 5. جلب الملاحظات المرتبطة بوثيقة معينة
    fun getNotesFlow(documentId: String): Flow<List<NoteModel>> = callbackFlow {
        val listener = firestore.collection("documents")
            .document(documentId)
            .collection("notes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(NoteModel::class.java))
                }
            }
        awaitClose { listener.remove() }
    }

    // 6. حفظ التلخيص للوثيقة
    suspend fun updateSummary(documentId: String, summary: String): Result<Unit> = runCatching {
        firestore.collection("documents")
            .document(documentId)
            .update("summary", summary)
            .await()
    }

    // 7. حفظ فصل مقتطع (Chapter)
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

    // 8. حذف وثيقة وملفها من السحاب
    suspend fun deleteDocument(doc: DocumentModel): Result<Unit> = runCatching {
        if (doc.storagePath.isNotBlank()) {
            storage.reference.child(doc.storagePath).delete().await()
        }
        firestore.collection("documents").document(doc.id).delete().await()
    }
}
