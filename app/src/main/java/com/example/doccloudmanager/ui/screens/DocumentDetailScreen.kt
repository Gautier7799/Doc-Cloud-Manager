package com.example.doccloudmanager.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.doccloudmanager.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val document by viewModel.selectedDocument.collectAsState()
    val notes by viewModel.notesState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(document) {
        if (document == null) {
            onBackClick()
        }
    }

    if (document == null) return

    var selectedTab by remember { mutableIntStateOf(0) }
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var showAddNoteDialog by remember { mutableStateOf(false) }

    val tabs = listOf("التلخيص", "الملاحظات")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(document?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        document?.fileUrl?.let { url ->
                            shareDocumentLink(context, document!!.title, url)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ملخص المستند (AI):", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (document?.summary.isNullOrBlank()) "لا يوجد تلخيص متاح لهذا المستند بعد." else document!!.summary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                1 -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (notes.isEmpty()) {
                            Text(
                                "لا توجد ملاحظات مضافة",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(notes) { note ->
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(note.title, style = MaterialTheme.typography.titleSmall)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(note.content, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { showAddNoteDialog = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        ) {
                            Text("إضافة ملاحظة")
                        }
                    }
                }
            }
        }
    }

    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("ملاحظة جديدة") },
            text = {
                Column {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("العنوان") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("المحتوى") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (noteTitle.isNotBlank()) {
                        viewModel.addNoteToSelectedDocument(noteTitle, noteContent)
                        noteTitle = ""
                        noteContent = ""
                        showAddNoteDialog = false
                    }
                }) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

private fun shareDocumentLink(context: Context, title: String, url: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "رابط المستند: $title\n$url")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "مشاركة المستند عبر")
    context.startActivity(shareIntent)
}
