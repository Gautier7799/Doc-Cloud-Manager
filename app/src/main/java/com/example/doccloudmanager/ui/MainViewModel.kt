package com.example.doccloudmanager.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doccloudmanager.data.model.DocumentModel
import com.example.doccloudmanager.data.model.FileType
import com.example.doccloudmanager.data.model.NoteModel
import com.example.doccloudmanager.data.repository.FirebaseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class MainViewModel(
    private val repository: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val documentsState: StateFlow<UiState<List<DocumentModel>>> = _searchQuery
        .flatMapLatest { query ->
            repository.getDocumentsFlow(query)
                .map { docs -> UiState.Success(docs) as UiState<List<DocumentModel>> }
                .catch { emit(UiState.Error(it.message ?: "حدث خطأ غير متوقع")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _selectedDocument = MutableStateFlow<DocumentModel?>(null)
    val selectedDocument: StateFlow<DocumentModel?> = _selectedDocument.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notesState: StateFlow<List<NoteModel>> = _selectedDocument
        .flatMapLatest { doc ->
            if (doc != null) repository.getNotesFlow(doc.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectDocument(doc: DocumentModel?) {
        _selectedDocument.value = doc
    }

    fun uploadDocument(uri: Uri, title: String, fileType: FileType, sizeBytes: Long) {
        viewModelScope.launch {
            repository.uploadDocument(uri, title, fileType, sizeBytes, emptyList())
        }
    }

    fun addNoteToSelectedDocument(title: String, content: String, pageIndex: Int? = null) {
        val currentDoc = _selectedDocument.value ?: return
        viewModelScope.launch {
            repository.addNote(currentDoc.id, title, content, pageIndex)
        }
    }

    fun updateDocumentSummary(summary: String) {
        val currentDoc = _selectedDocument.value ?: return
        viewModelScope.launch {
            repository.updateSummary(currentDoc.id, summary)
            _selectedDocument.value = currentDoc.copy(summary = summary)
        }
    }

    fun deleteDocument(doc: DocumentModel) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
        }
    }
}
