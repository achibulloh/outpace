package com.example.pace.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.pace.R;
import com.example.pace.utils.LocaleHelper;
import com.example.pace.utils.NetworkUtils;
import com.example.pace.utils.ProfileUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private FirebaseAuth mAuth;
    private boolean isRouting = false;
    private final Handler handler = new Handler();

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();

        // Start checking immediately
        checkInternetAndProceed();
    }

    private void checkInternetAndProceed() {
        if (isFinishing() || isRouting) return;

        if (NetworkUtils.isInternetAvailable(this)) {
            checkAuthAndRouting();
        } else {
            showNoInternetDialog();
        }
    }

    private void showNoInternetDialog() {
        if (isFinishing()) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.no_internet_title)
                .setMessage(R.string.no_internet_desc)
                .setCancelable(false)
                .setPositiveButton(R.string.retry, (dialog, which) -> checkInternetAndProceed())
                .setNegativeButton(R.string.exit, (dialog, which) -> finish())
                .show();
    }

    private void checkAuthAndRouting() {
        if (isFinishing() || isRouting) return;
        
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkPermissions();
        } else {
            isRouting = true;
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void checkPermissions() {
        if (isFinishing() || isRouting) return;

        boolean location = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        
        boolean backgroundLocation = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        boolean activity = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity = ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
        }

        boolean notification = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notification = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }

        if (!location || !backgroundLocation || !activity || !notification || !isGpsEnabled()) {
            isRouting = true;
            startActivity(new Intent(this, PermissionActivity.class));
            finish();
        } else {
            checkProfileCompletion();
        }
    }

    private boolean isGpsEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Do nothing here to avoid double execution. 
        // Logic is handled by onCreate (initially) or manual triggers.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkPermissions();
    }

    private void checkProfileCompletion() {
        if (isFinishing() || isRouting) return;
        
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isFinishing() || isRouting) return;
                    
                    if (documentSnapshot.exists()) {
                        // SAVE USER INFO TO PREFS FOR OFFLINE ACCESS & AI PERSONALIZATION
                        String name = documentSnapshot.getString("name");
                        String goal = documentSnapshot.getString("goal");
                        if (name != null) {
                            getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                                    .putString("full_name", name)
                                    .putString("goal", goal != null ? goal : "General Fitness")
                                    .apply();
                        }

                        if (ProfileUtils.isProfileComplete(documentSnapshot)) {
                            isRouting = true;
                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        } else {
                            isRouting = true;
                            startActivity(new Intent(SplashActivity.this, CompleteProfileActivity.class));
                        }
                        finish();
                    } else {
                        // User document doesn't exist, create it then go to complete profile
                        isRouting = true;
                        startActivity(new Intent(SplashActivity.this, CompleteProfileActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing()) return;
                    Toast.makeText(this, getString(R.string.profile_verify_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }
}
