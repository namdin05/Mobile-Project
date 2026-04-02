package com.melodix.app;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.melodix.app.Utils.SessionManager;
import com.melodix.app.View.playlist.MyPlaylistsFragment;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (savedInstanceState == null) {
            if (sessionManager.isLoggedIn()) {
                loadMyPlaylistsFragment();
            } else {
                loadMyPlaylistsFragment();
            }
        }
    }

    private void loadMyPlaylistsFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main, new MyPlaylistsFragment())
                .commit();
    }
}