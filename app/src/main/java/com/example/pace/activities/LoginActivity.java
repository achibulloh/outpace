package com.example.pace.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pace.R;
import com.example.pace.model.User;
import com.example.pace.utils.LocaleHelper;
import com.example.pace.utils.ProfileUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        setupGoogleSignIn();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        LinearLayout btnGoogle = findViewById(R.id.btnGoogle);
        MaterialCardView btnLanguage = findViewById(R.id.btnLanguage);
        ImageView ivFlag = findViewById(R.id.ivFlag);
        TextView tvLanguageName = findViewById(R.id.tvLanguageName);

        // Set initial language UI
        String currentLang = LocaleHelper.getLanguage(this);
        if (currentLang.equals("id") || currentLang.equals("in")) {
            ivFlag.setImageResource(R.drawable.ic_flag_id);
            tvLanguageName.setText(R.string.lang_id);
        } else {
            ivFlag.setImageResource(R.drawable.ic_flag_en);
            tvLanguageName.setText(R.string.lang_en);
        }

        btnLogin.setOnClickListener(v -> loginUser());

        btnGoogle.setOnClickListener(v -> initiateGoogleLogin());

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });

        btnLanguage.setOnClickListener(this::showLanguageMenu);
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.fill_email_password, Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, SplashActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, getString(R.string.login_failed, task.getException().getMessage()),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setupGoogleSignIn() {
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

            // Register launcher here in onCreate/init - NEVER call this again
            if (googleSignInLauncher == null) {
                googleSignInLauncher = registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                                handleSignInResult(task);
                            }
                        }
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initiateGoogleLogin() {
        if (mGoogleSignInClient == null) {
            Toast.makeText(this, "Google Sign-In is initializing...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Force sign out first to ensure account picker always appears
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            try {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            } catch (Exception e) {
                Log.e("LoginActivity", "Failed to launch Google Sign In", e);
            }
        });
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null && account.getIdToken() != null) {
                firebaseAuthWithGoogle(account.getIdToken());
            } else {
                Toast.makeText(this, "Google login failed: Empty token", Toast.LENGTH_SHORT).show();
            }
        } catch (ApiException e) {
            Log.e("LoginActivity", "Google login failed: " + e.getStatusCode());
            Toast.makeText(this, "Google Sign-In failed (Code " + e.getStatusCode() + ")", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserInFirestore(user);
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, R.string.firebase_auth_failed_val, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserInFirestore(FirebaseUser firebaseUser) {
        db.collection("users").document(firebaseUser.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().exists()) {
                            // User baru dari Google, simpan ke Firestore
                            saveUserToFirestore(firebaseUser.getUid(), firebaseUser.getDisplayName(), firebaseUser.getEmail());
                        } else {
                            // User sudah ada
                            Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, SplashActivity.class));
                            finish();
                        }
                    }
                });
    }

    private void saveUserToFirestore(String uid, String name, String email) {
        User user = new User(uid, name, email);
        db.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, SplashActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, getString(R.string.data_save_failed_val, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }

    private void showLanguageMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add(0, 1, 0, getString(R.string.lang_en));
        popup.getMenu().add(0, 2, 1, getString(R.string.lang_id));

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                LocaleHelper.setLocale(this, "en");
            } else {
                LocaleHelper.setLocale(this, "id");
            }
            recreate();
            return true;
        });
        popup.show();
    }
}
