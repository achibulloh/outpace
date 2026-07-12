package com.example.pace.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pace.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout layoutEmail;
    private TextInputEditText etEmail;
    private TextView tvSubtitle;
    private Button btnAction;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();
        initUI();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnAction.setOnClickListener(v -> sendResetEmail());
    }

    private void initUI() {
        layoutEmail = findViewById(R.id.layoutEmail);
        etEmail = findViewById(R.id.etEmail);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        btnAction = findViewById(R.id.btnAction);

        // Hide OTP and New Password sections as we'll use Firebase Link
        if (findViewById(R.id.layoutOtp) != null) findViewById(R.id.layoutOtp).setVisibility(View.GONE);
        if (findViewById(R.id.layoutNewPassword) != null) findViewById(R.id.layoutNewPassword).setVisibility(View.GONE);
        
        btnAction.setText("Kirim Link Reset");
        tvSubtitle.setText("Masukkan email Anda untuk menerima link reset kata sandi");
    }

    private void sendResetEmail() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Masukkan email Anda", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAction.setEnabled(false);
        btnAction.setText("Mengirim...");

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    btnAction.setEnabled(true);
                    btnAction.setText("Kirim Link Reset");
                    
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this, 
                            "Link reset telah dikirim ke email Anda. Silakan cek Inbox/Spam.", 
                            Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Gagal mengirim email";
                        Toast.makeText(ForgotPasswordActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
