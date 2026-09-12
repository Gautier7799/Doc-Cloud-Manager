package com.example.doccloudmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.doccloudmanager.ui.MainViewModel
import com.example.doccloudmanager.ui.screens.DocumentDetailScreen
import com.example.doccloudmanager.ui.screens.MainScreen
import com.example.doccloudmanager.ui.screens.TextEditorScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // مشاركة نفس الـ ViewModel بين الشاشة الرئيسية وشاشة التفاصيل لنقل البيانات المحددة
    val viewModel = remember { MainViewModel() }

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        // 1. الشاشة الرئيسية (قائمة المستندات والبحث)
        composable("main") {
            MainScreen(
                viewModel = viewModel,
                onDocumentClick = {
                    navController.navigate("detail")
                },
                onCreateNewTxtClick = {
                    navController.navigate("text_editor")
                }
            )
        }

        // 2. شاشة تفاصيل المستند (التلخيص، الملاحظات، والمشاركة)
        composable("detail") {
            DocumentDetailScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // 3. شاشة محرر النصوص TXT (الإنشاء والتنسيق والحفظ السحابي)
        composable("text_editor") {
            TextEditorScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    navController.popBackStack()
                }
            )
        }
    }
}
