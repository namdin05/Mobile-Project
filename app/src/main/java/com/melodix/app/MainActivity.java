package com.melodix.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.melodix.app.View.auth.AuthActivity;
import com.melodix.app.View.home.HomeActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MelodixMainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        routeSafely();
    }

    private void routeSafely() {
        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            Intent intent;

            if (currentUser == null) {
                intent = new Intent(this, AuthActivity.class);
            } else {
                intent = new Intent(this, HomeActivity.class);
            }

            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Failed to route initial screen.", e);
            try {
                setContentView(R.layout.activity_main);
            } catch (Exception innerException) {
                Log.e(TAG, "Fallback layout inflation failed.", innerException);
            }
        }
    }
}