package com.example.pace.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
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
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        setupGoogleSignIn();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        Button btnRegister = findViewById(R.id.btnRegister);
        LinearLayout btnGoogleRegister = findViewById(R.id.btnGoogleRegister);
        TextView tvLogin = findViewById(R.id.tvLogin);
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

        btnRegister.setOnClickListener(v -> registerUser());

        btnGoogleRegister.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        tvLogin.setOnClickListener(v -> {
            finish();
        });

        btnLanguage.setOnClickListener(this::showLanguageMenu);
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.fill_email_password, Toast.LENGTH_SHORT).show(); // Reusing the same string
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, R.string.password_not_match, Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, R.string.password_min_chars, Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            saveUserToFirestore(firebaseUser.getUid(), name, email);
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, getString(R.string.register_failed, task.getException().getMessage()),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        handleSignInResult(task);
                    }
                }
        );
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account.getIdToken());
            }
        } catch (ApiException e) {
            Toast.makeText(this, getString(R.string.google_sign_in_failed_val, e.getStatusCode()), Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(RegisterActivity.this, R.string.firebase_auth_failed_val, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserInFirestore(FirebaseUser firebaseUser) {
        db.collection("users").document(firebaseUser.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().exists()) {
                            saveUserToFirestore(firebaseUser.getUid(), firebaseUser.getDisplayName(), firebaseUser.getEmail());
                        } else {
                            Toast.makeText(RegisterActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, SplashActivity.class));
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
                    Toast.makeText(RegisterActivity.this, R.string.register_success, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, SplashActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, getString(R.string.data_save_failed_val, e.getMessage()), Toast.LENGTH_SHORT).show();
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
