package com.example.pace.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pace.R;
import com.example.pace.utils.LocaleHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout layoutEmail;
    
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
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
        
        btnAction.setText(R.string.send_otp);
        tvSubtitle.setText(R.string.reset_subtitle);
    }

    private void sendResetEmail() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, R.string.fill_email_password, Toast.LENGTH_SHORT).show();
            return;
        }

        btnAction.setEnabled(false);
        btnAction.setText(R.string.saving);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    btnAction.setEnabled(true);
                    btnAction.setText(R.string.send_otp);
                    
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this, 
                            R.string.password_updated, 
                            Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Error";
                        Toast.makeText(ForgotPasswordActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
