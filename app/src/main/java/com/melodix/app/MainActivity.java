package com.melodix.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

// Nhớ import đường dẫn tới SearchFragment của bạn nhé (Alt + Enter nếu nó bị đỏ)
import com.melodix.app.View.search.SearchFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Kiểm tra để tránh add đè Fragment khi xoay màn hình
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new SearchFragment())
                    .commit();
        }
    }
}