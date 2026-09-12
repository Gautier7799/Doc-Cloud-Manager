package com.example.doccloudmanager.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.doccloudmanager.data.repository.FirebaseRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    repository: FirebaseRepository = remember { FirebaseRepository() }
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("محرر النصوص TXT") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(
                        enabled = !isSaving && title.isNotBlank() && content.isNotBlank(),
                        onClick = {
                            isSaving = true
                            scope.launch {
                                repository.createTextDocument(
                                    title = title.trim(),
                                    content = content,
                                    tags = listOf("TXT", "مُنشأ محلياً")
                                ).fold(
                                    onSuccess = {
                                        isSaving = false
                                        Toast.makeText(context, "تم حفظ الملف في السحاب بنجاح", Toast.LENGTH_SHORT).show()
                                        onSaveSuccess()
                                    },
                                    onFailure = { error ->
                                        isSaving = false
                                        Toast.makeText(context, "فشل الحفظ: ${error.message}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        }
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "حفظ في السحاب")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // حقل عنوان المستند
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان المستند (.txt)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // شريط أدوات التنسيق (Editor Toolbar)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // زر عريض (Bold)
                    FilterChip(
                        selected = isBold,
                        onClick = { isBold = !isBold },
                        label = { Icon(Icons.Default.FormatBold, contentDescription = "عريض") }
                    )

                    // زر مائل (Italic)
                    FilterChip(
                        selected = isItalic,
                        onClick = { isItalic = !isItalic },
                        label = { Icon(Icons.Default.FormatItalic, contentDescription = "مائل") }
                    )

                    // إضافة نقطة للقائمة (Bullet Point)
                    IconButton(onClick = { content += "\n• " }) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = "قائمة")
                    }

                    // إضافة اقتباس (Quote)
                    IconButton(onClick = { content += "\n\" \" " }) {
                        Icon(Icons.Default.FormatQuote, contentDescription = "اقتباس")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // حقل محرر النص الرئيسي
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("اكتب محتوى الوثيقة هنا...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
