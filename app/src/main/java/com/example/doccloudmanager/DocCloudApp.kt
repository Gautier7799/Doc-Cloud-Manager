package com.example.doccloudmanager

import android.app.Application
import com.google.firebase.FirebaseApp

class DocCloudApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // تهيئة محصنة لمنع انهيار التطبيق عند الفتح إذا كانت إعدادات السحاب غائبة
        runCatching {
            FirebaseApp.initializeApp(this)
        }
    }
}
