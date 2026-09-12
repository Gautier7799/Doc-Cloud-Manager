package com.example.doccloudmanager

import android.app.Application
import com.google.firebase.FirebaseApp

class DocCloudApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching {
            FirebaseApp.initializeApp(this)
        }
    }
}
