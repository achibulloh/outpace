package com.example.pace.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import com.example.pace.R;
import com.example.pace.utils.LocaleHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private SwitchCompat switchDarkMode;
    private ImageView ivSettingsFlag;
    private TextView tvSettingsLanguageName, tvGoogleStatus, tvLinkAction;
    private ProgressBar pbGoogleLink;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initUI();
        setupGoogleSignIn();
        loadSettings();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnLanguage).setOnClickListener(this::showLanguageMenu);
        findViewById(R.id.btnLinkGoogle).setOnClickListener(v -> linkGoogleAccount());

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            saveThemePreference(isChecked);
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void initUI() {
        switchDarkMode = findViewById(R.id.switchDarkMode);
        ivSettingsFlag = findViewById(R.id.ivSettingsFlag);
        tvSettingsLanguageName = findViewById(R.id.tvSettingsLanguageName);
        tvGoogleStatus = findViewById(R.id.tvGoogleStatus);
        tvLinkAction = findViewById(R.id.tvLinkAction);
        pbGoogleLink = findViewById(R.id.pbGoogleLink);
    }

    private void setupGoogleSignIn() {
        Log.d(TAG, "Setting up Google Sign In");
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "ActivityResult received: " + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        handleSignInResult(task);
                    } else if (result.getResultCode() == RESULT_CANCELED) {
                        Toast.makeText(this, R.string.selection_cancelled, Toast.LENGTH_SHORT).show();
                        setLoading(false);
                    } else {
                        Toast.makeText(this, getString(R.string.sign_in_failed) + " (Code: " + result.getResultCode() + ")", Toast.LENGTH_SHORT).show();
                        setLoading(false);
                    }
                }
        );
    }

    private void loadSettings() {
        // Load Theme
        SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", true);
        switchDarkMode.setChecked(isDarkMode);

        // Load Language UI
        String currentLang = LocaleHelper.getLanguage(this);
        if (currentLang.equals("id") || currentLang.equals("in")) {
            ivSettingsFlag.setImageResource(R.drawable.ic_flag_id);
            tvSettingsLanguageName.setText(R.string.lang_id);
        } else {
            ivSettingsFlag.setImageResource(R.drawable.ic_flag_en);
            tvSettingsLanguageName.setText(R.string.lang_en);
        }

        // Load Google Link Status from Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String googleEmail = null;
        if (user != null) {
            for (UserInfo profile : user.getProviderData()) {
                if (profile.getProviderId().equals(GoogleAuthProvider.PROVIDER_ID)) {
                    googleEmail = profile.getEmail();
                    break;
                }
            }
        }

        if (googleEmail != null) {
            tvGoogleStatus.setText(getString(R.string.linked_with, googleEmail));
            tvLinkAction.setText(getString(R.string.action_unlink));
            tvLinkAction.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
        } else {
            tvGoogleStatus.setText(getString(R.string.link_google));
            tvLinkAction.setText(getString(R.string.action_link));
            tvLinkAction.setTextColor(ContextCompat.getColor(this, R.color.lime));
        }
    }

    private void linkGoogleAccount() {
        Log.d(TAG, "linkGoogleAccount clicked");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.login_first, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isLinked = false;
        for (UserInfo profile : user.getProviderData()) {
            Log.d(TAG, "Provider found: " + profile.getProviderId());
            if (profile.getProviderId().equals(GoogleAuthProvider.PROVIDER_ID)) {
                isLinked = true;
                break;
            }
        }

        if (isLinked) {
            Log.d(TAG, "Account is already linked, initiating unlink");
            setLoading(true);
            user.unlink(GoogleAuthProvider.PROVIDER_ID)
                    .addOnCompleteListener(task -> {
                        setLoading(false);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Unlink successful");
                            mGoogleSignInClient.signOut();
                            loadSettings();
                            Toast.makeText(this, R.string.google_account_unlinked, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "Unlink failed", task.getException());
                            String error = task.getException() != null ? task.getException().getMessage() : getString(R.string.error_unknown);
                            Toast.makeText(this, getString(R.string.unlinking_failed_val, error), Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            Log.d(TAG, "Account not linked, launching Google Sign In");
            setLoading(true);
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                Log.d(TAG, "Google Sign In successful, linking with Firebase. Email: " + account.getEmail());
                firebaseLinkWithGoogle(account.getIdToken());
            } else {
                Log.e(TAG, "Google Account is null");
                setLoading(false);
                Toast.makeText(this, R.string.google_info_failed_val, Toast.LENGTH_SHORT).show();
            }
        } catch (ApiException e) {
            Log.e(TAG, "Google Sign In failed. Status code: " + e.getStatusCode() + ", Message: " + e.getMessage());
            setLoading(false);
            
            String detailedError;
            switch (e.getStatusCode()) {
                case 7: detailedError = "Network Error (Cek internet)"; break;
                case 10: detailedError = "Developer Error (SHA-1 atau Client ID salah)"; break;
                case 12500: detailedError = "Sign-in failed"; break;
                case 12501: detailedError = "Pemilihan akun dibatalkan/dihentikan"; break;
                default: detailedError = "Error " + e.getStatusCode();
            }
            
            Toast.makeText(this, "Gagal: " + detailedError, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Saran: Pastikan SHA-1 05:F7:20:82:A0:53:CD:3F:F7:B2:61:C2:38:3A:64:28:B2:5D:3D:12 sudah ada di Firebase Console.");
        }
    }

    private void firebaseLinkWithGoogle(String idToken) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            setLoading(false);
            return;
        }

        Log.d(TAG, "firebaseLinkWithGoogle initiating");
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        user.linkWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase linking successful");
                        loadSettings();
                        Toast.makeText(this, R.string.account_linking_success, Toast.LENGTH_LONG).show();
                    } else {
                        Log.e(TAG, "Firebase linking failed", task.getException());
                        String error = task.getException() != null ? task.getException().getMessage() : getString(R.string.error_unknown);
                        if (error != null && error.contains("credential-already-in-use")) {
                            error = getString(R.string.google_account_already_linked);
                            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, getString(R.string.linking_failed_val, error), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        pbGoogleLink.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        tvLinkAction.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        findViewById(R.id.btnLinkGoogle).setEnabled(!isLoading);
    }

    private void saveThemePreference(boolean isDarkMode) {
        SharedPreferences.Editor editor = getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit();
        editor.putBoolean("dark_mode", isDarkMode);
        editor.apply();
    }

    private void showLanguageMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add(0, 1, 0, R.string.lang_en);
        popup.getMenu().add(0, 2, 1, R.string.lang_id);

        popup.setOnMenuItemClickListener(item -> {
            String lang = (item.getItemId() == 1) ? "en" : "id";
            LocaleHelper.setLocale(this, lang);
            
            // Explicitly update application context as well
            LocaleHelper.onAttach(getApplicationContext());
            
            // Restart the app to apply language changes everywhere
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        });
        popup.show();
    }
}
