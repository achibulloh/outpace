package com.example.pace.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pace.R;
import com.example.pace.utils.LocaleHelper;
import com.google.android.material.card.MaterialCardView;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);
        LinearLayout btnGoogle = findViewById(R.id.btnGoogle);
        MaterialCardView btnLanguage = findViewById(R.id.btnLanguage);
        ImageView ivFlag = findViewById(R.id.ivFlag);
        TextView tvLanguageName = findViewById(R.id.tvLanguageName);

        // Set initial language UI
        String currentLang = LocaleHelper.getLanguage(this);
        if (currentLang.equals("in")) {
            ivFlag.setImageResource(R.drawable.ic_flag_id);
            tvLanguageName.setText("Indonesia");
        } else {
            ivFlag.setImageResource(R.drawable.ic_flag_en);
            tvLanguageName.setText("English");
        }

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        btnGoogle.setOnClickListener(v -> {
            Toast.makeText(this, "Google Login clicked", Toast.LENGTH_SHORT).show();
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        btnLanguage.setOnClickListener(this::showLanguageMenu);
    }

    private void showLanguageMenu(android.view.View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add(0, 1, 0, "English");
        popup.getMenu().add(0, 2, 1, "Indonesia");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                LocaleHelper.setLocale(this, "en");
            } else {
                LocaleHelper.setLocale(this, "in");
            }
            recreate();
            return true;
        });
        popup.show();
    }
}
