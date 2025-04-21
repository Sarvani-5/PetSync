package com.example.petsync

import android.app.Application
import com.google.firebase.FirebaseApp

class PetSyncApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}