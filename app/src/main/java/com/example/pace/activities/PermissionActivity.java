package com.example.pace.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.pace.R;

public class PermissionActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;
    private ImageView ivCheckLocation, ivCheckActivity, ivCheckNotification;
    private Button btnGrantAll;
    private android.view.View btnLocationSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        ivCheckLocation = findViewById(R.id.ivCheckLocation);
        ivCheckActivity = findViewById(R.id.ivCheckActivity);
        ivCheckNotification = findViewById(R.id.ivCheckNotification);
        btnGrantAll = findViewById(R.id.btnGrantAll);
        btnLocationSetting = findViewById(R.id.btnLocationSetting);

        btnGrantAll.setOnClickListener(v -> requestNecessaryPermissions());
        
        btnLocationSetting.setOnClickListener(v -> requestNecessaryPermissions());

        updateStatus();
    }

    private void updateStatus() {
        boolean hasLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasBackgroundLocation = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasBackgroundLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        
        boolean locationAndGps = hasLocationPermission && hasBackgroundLocation && isGpsEnabled();
        ivCheckLocation.setColorFilter(locationAndGps ? ContextCompat.getColor(this, R.color.lime) : ContextCompat.getColor(this, R.color.muted));

        boolean activity = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity = ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
        }
        ivCheckActivity.setColorFilter(activity ? ContextCompat.getColor(this, R.color.lime) : ContextCompat.getColor(this, R.color.muted));

        boolean notification = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notification = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        ivCheckNotification.setColorFilter(notification ? ContextCompat.getColor(this, R.color.lime) : ContextCompat.getColor(this, R.color.muted));

        if (locationAndGps && activity && notification) {
            btnGrantAll.setText("Lanjutkan");
            btnGrantAll.setEnabled(true);
            btnGrantAll.setAlpha(1.0f);
            btnGrantAll.setOnClickListener(v -> {
                startActivity(new Intent(this, SplashActivity.class));
                finish();
            });
        } else {
            btnGrantAll.setText("Izinkan Semua");
            btnGrantAll.setEnabled(true); // Biarkan aktif agar bisa memicu request permission
            btnGrantAll.setOnClickListener(v -> {
                if (!locationAndGps) {
                    Toast.makeText(this, "Harap pastikan izin Lokasi 'Izinkan sepanjang waktu' dan GPS aktif!", Toast.LENGTH_LONG).show();
                    requestNecessaryPermissions();
                } else {
                    requestNecessaryPermissions();
                }
            });
        }
    }

    private boolean isGpsEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void requestNecessaryPermissions() {
        // 1. Request Basic Location Permission (Foreground)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
            return;
        }

        // 2. Request Background Location (Always On) for Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Langsung arahkan ke pengaturan izin lokasi tanpa dialog perantara
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, PERMISSION_REQUEST_CODE);
                return;
            }
        }

        // 3. Check GPS Hardware
        if (!isGpsEnabled()) {
            showGpsDialog();
            return;
        }

        // 4. Then other permissions based on API Level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    Manifest.permission.POST_NOTIFICATIONS
            }, PERMISSION_REQUEST_CODE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACTIVITY_RECOGNITION
            }, PERMISSION_REQUEST_CODE);
        }
    }

    private void showGpsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("GPS Tidak Aktif")
                .setMessage("Silakan aktifkan GPS untuk melanjutkan.")
                .setPositiveButton("Pengaturan", (d, w) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("Batal", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        updateStatus();
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) updateStatus();
            else Toast.makeText(this, "Beberapa izin ditolak. Harap izinkan semua untuk melanjutkan.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
}
