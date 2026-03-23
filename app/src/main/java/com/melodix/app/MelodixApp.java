package com.melodix.app;

import android.app.Application;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.FirebaseApp;

public class MelodixApp extends Application {

    private static final String TAG = "MelodixApp";

    @Override
    public void onCreate() {
        super.onCreate();
        initializeFirebaseSafely();
        initializeThemeSafely();
    }

    private void initializeFirebaseSafely() {
        try {
            FirebaseApp firebaseApp = FirebaseApp.initializeApp(this);
            if (firebaseApp == null) {
                Log.e(TAG, "FirebaseApp is null. Please check google-services.json in app/ folder.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed.", e);
        }
    }

    private void initializeThemeSafely() {
        try {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply default dark mode.", e);
        }
    }
}