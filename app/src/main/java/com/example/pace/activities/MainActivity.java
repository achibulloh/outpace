package com.example.pace.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.pace.R;
import com.example.pace.fragments.*;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        loadFragment(new HomeFragment());

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment f;
            int id = item.getItemId();
            if (id == R.id.nav_home)         f = new HomeFragment();
            else if (id == R.id.nav_history) f = new HistoryFragment();
            else if (id == R.id.nav_run)     f = new RunFragment();
            else if (id == R.id.nav_progress)f = new ProgressFragment();
            else                             f = new ProfileFragment();
            loadFragment(f);
            return true;
        });
    }

    private void loadFragment(Fragment f) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, f).commit();
    }
}