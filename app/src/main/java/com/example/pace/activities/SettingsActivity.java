package com.example.pace.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pace.R;

public class SettingsActivity extends AppCompatActivity {

    private EditText etWeight, etMonthlyTarget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etWeight = findViewById(R.id.etWeight);
        etMonthlyTarget = findViewById(R.id.etMonthlyTarget);

        loadCurrentSettings();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveSettings());
    }

    private void loadCurrentSettings() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        etWeight.setText(String.valueOf(prefs.getInt("weight", 70)));
        etMonthlyTarget.setText(String.valueOf(prefs.getInt("monthly_target", 150)));
    }

    private void saveSettings() {
        String weightStr = etWeight.getText().toString();
        String targetStr = etMonthlyTarget.getText().toString();

        if (weightStr.isEmpty() || targetStr.isEmpty()) {
            Toast.makeText(this, "Harap isi semua data", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit();
        editor.putInt("weight", Integer.parseInt(weightStr));
        editor.putInt("monthly_target", Integer.parseInt(targetStr));
        editor.apply();

        Toast.makeText(this, "Pengaturan disimpan", Toast.LENGTH_SHORT).show();
        finish();
    }
}
