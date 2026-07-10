package com.example.pace.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pace.R;

public class HelpActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
