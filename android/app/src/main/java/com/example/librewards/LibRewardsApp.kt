package com.example.librewards

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class LibRewardsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isEmpty()) {
            Log.i("LibRewardsApp", "Initialising Firebase app...")
            FirebaseApp.initializeApp(this)
        }
    }
}
