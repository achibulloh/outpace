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

    private long lastClickTime = 0;

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
