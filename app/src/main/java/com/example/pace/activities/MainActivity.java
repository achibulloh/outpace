package com.example.pace.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.pace.R;
import com.example.pace.fragments.*;
import com.example.pace.utils.LocaleHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private long lastClickTime = 0;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final String TAG = "MainActivity";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Prevent system from tinting the navigation bar on Xiaomi/MIUI
        getWindow().setNavigationBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.bg));

        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationUpdates();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        FloatingActionButton fabCenter = findViewById(R.id.fabCenter);

        // Initial fragment load
        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            if (System.currentTimeMillis() - lastClickTime < 100) return true;
            lastClickTime = System.currentTimeMillis();

            Fragment f = null;
            String tag = "";
            int id = item.getItemId();
            
            if (id == R.id.nav_home) { f = new HomeFragment(); tag = "HomeFragment"; }
            else if (id == R.id.nav_history) { f = new HistoryFragment(); tag = "HistoryFragment"; }
            else if (id == R.id.nav_progress) { f = new ProgressFragment(); tag = "ProgressFragment"; }
            else if (id == R.id.nav_profile) { f = new ProfileFragment(); tag = "ProfileFragment"; }
            
            if (f != null) {
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                if (current != null && current.getClass().equals(f.getClass())) {
                    return true;
                }
                loadFragment(f, tag);
                return true;
            }
            return false;
        });

        fabCenter.setOnClickListener(v -> {
            if (System.currentTimeMillis() - lastClickTime < 100) return;
            lastClickTime = System.currentTimeMillis();

            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (current == null || !(current instanceof RunFragment)) {
                loadFragment(new RunFragment(), "RunFragment");
                // Uncheck all menu items in bottom nav
                bottomNav.getMenu().setGroupCheckable(0, true, false);
                for (int i = 0; i < bottomNav.getMenu().size(); i++) {
                    bottomNav.getMenu().getItem(i).setChecked(false);
                }
                bottomNav.getMenu().setGroupCheckable(0, true, true);
            }
        });
    }

    private void setupLocationUpdates() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    for (android.location.Location location : locationResult.getLocations()) {
                        updateUserLocation(user.getUid(), location);
                    }
                }
            }
        };
    }

    private void updateUserLocation(String uid, android.location.Location location) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> update = new HashMap<>();
        update.put("latitude", location.getLatitude());
        update.put("longitude", location.getLongitude());
        update.put("lastLocationUpdate", System.currentTimeMillis());
        
        db.collection("users").document(uid).update(update)
                .addOnFailureListener(e -> Log.e(TAG, "Error updating location", e));
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 600000) // Update every 10 mins
                .setMinUpdateIntervalMillis(300000) // At least 5 mins
                .build();
        fusedLocationClient.requestLocationUpdates(request, locationCallback, android.os.Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(android.content.Intent intent) {
        if (intent != null && "RUN_FRAGMENT".equals(intent.getStringExtra("NAVIGATE_TO"))) {
            RunFragment runFragment = new RunFragment();
            if (intent.getBooleanExtra("SHOW_POST_RUN", false)) {
                Bundle args = new Bundle();
                args.putBoolean("SHOW_POST_RUN", true);
                runFragment.setArguments(args);
            }
            loadFragment(runFragment, "RunFragment");
            
            // Update UI
            BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                bottomNav.getMenu().setGroupCheckable(0, true, false);
                for (int i = 0; i < bottomNav.getMenu().size(); i++) {
                    bottomNav.getMenu().getItem(i).setChecked(false);
                }
                bottomNav.getMenu().setGroupCheckable(0, true, true);
            }
        } else {
            // Load Home by default if no special navigation
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (current == null) {
                loadFragment(new HomeFragment(), "HomeFragment");
            }
        }
    }

    public void setNavigationVisibility(boolean visible) {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        FloatingActionButton fabCenter = findViewById(R.id.fabCenter);
        com.google.android.material.bottomappbar.BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);

        int visibility = visible ? android.view.View.VISIBLE : android.view.View.GONE;
        if (bottomNav != null) bottomNav.setVisibility(visibility);
        if (fabCenter != null) fabCenter.setVisibility(visibility);
        if (bottomAppBar != null) bottomAppBar.setVisibility(visibility);
    }

    private void loadFragment(Fragment f, String tag) {
        if (isFinishing() || isDestroyed()) return;
        
        try {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.fragmentContainer, f, tag)
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            com.example.pace.utils.GeminiAssistant.getInstance().shutdown();
            com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
            db.terminate();
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleHelper.onAttach(this);
    }
}
