package com.example.pace.activities;

import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.pace.R;
import com.example.pace.fragments.*;
import com.example.pace.utils.LocaleHelper;

public class MainActivity extends AppCompatActivity {

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

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        FloatingActionButton fabCenter = findViewById(R.id.fabCenter);

        if (getIntent() != null && "RUN_FRAGMENT".equals(getIntent().getStringExtra("NAVIGATE_TO"))) {
            loadFragment(new RunFragment());
        } else {
            loadFragment(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment f = null;
            int id = item.getItemId();
            
            if (id == R.id.nav_home) f = new HomeFragment();
            else if (id == R.id.nav_history) f = new HistoryFragment();
            else if (id == R.id.nav_progress) f = new ProgressFragment();
            else if (id == R.id.nav_profile) f = new ProfileFragment();
            
            if (f != null) {
                // Check if the current fragment is the same to avoid unnecessary reload
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                if (current != null && current.getClass().equals(f.getClass())) {
                    return true;
                }
                loadFragment(f);
                return true;
            }
            return false;
        });

        fabCenter.setOnClickListener(v -> {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (current == null || !(current instanceof RunFragment)) {
                loadFragment(new RunFragment());
            }
        });
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

    private void loadFragment(Fragment f) {
        if (isFinishing() || isDestroyed()) return;
        
        try {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragmentContainer, f, f.getClass().getSimpleName())
                    .commit();
        } catch (IllegalStateException e) {
            // Fallback for cases where state might be saved
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragmentContainer, f, f.getClass().getSimpleName())
                    .commitAllowingStateLoss();
        }
    }

    @Override
    protected void onDestroy() {
        // Terminate Firestore to avoid ManagedChannel leak warning on app close
        try {
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
