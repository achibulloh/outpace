package com.example.pace.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pace.R;
import com.example.pace.adapter.LeaderboardAdapter;
import com.example.pace.model.LeaderboardUser;
import com.example.pace.model.User;
import com.example.pace.utils.LocaleHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LeaderboardActivity extends AppCompatActivity {

    private enum Category { DISTANCE, PACE, STREAK }
    private enum TimeFilter { TODAY, WEEK, MONTH }

    private Category currentCategory = Category.DISTANCE;
    private TimeFilter currentTimeFilter = TimeFilter.TODAY;

    private TextView tabDistance, tabPace, tabStreak;
    private TextView filterToday, filterWeek, filterMonth;
    private RecyclerView rvRankings;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    // Podium Views
    private View containerRank1, containerRank2, containerRank3;
    private TextView tvName1, tvStat1, tvName2, tvStat2, tvName3, tvStat3;

    private LeaderboardAdapter adapter;
    private List<LeaderboardUser> fullList = new ArrayList<>();
    private String currentUid;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        currentUid = FirebaseAuth.getInstance().getUid();
        initViews();
        setupListeners();
        loadData();
    }

    private void initViews() {
        tabDistance = findViewById(R.id.tabDistance);
        tabPace = findViewById(R.id.tabPace);
        tabStreak = findViewById(R.id.tabStreak);
        filterToday = findViewById(R.id.filterToday);
        filterWeek = findViewById(R.id.filterWeek);
        filterMonth = findViewById(R.id.filterMonth);
        rvRankings = findViewById(R.id.rvRankings);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        containerRank1 = findViewById(R.id.containerRank1);
        containerRank2 = findViewById(R.id.containerRank2);
        containerRank3 = findViewById(R.id.containerRank3);
        tvName1 = findViewById(R.id.tvNameRank1);
        tvStat1 = findViewById(R.id.tvStatRank1);
        tvName2 = findViewById(R.id.tvNameRank2);
        tvStat2 = findViewById(R.id.tvStatRank2);
        tvName3 = findViewById(R.id.tvNameRank3);
        tvStat3 = findViewById(R.id.tvStatRank3);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvRankings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter(new ArrayList<>());
        rvRankings.setAdapter(adapter);
    }

    private void setupListeners() {
        tabDistance.setOnClickListener(v -> selectCategory(Category.DISTANCE));
        tabPace.setOnClickListener(v -> selectCategory(Category.PACE));
        tabStreak.setOnClickListener(v -> selectCategory(Category.STREAK));

        filterToday.setOnClickListener(v -> selectTimeFilter(TimeFilter.TODAY));
        filterWeek.setOnClickListener(v -> selectTimeFilter(TimeFilter.WEEK));
        filterMonth.setOnClickListener(v -> selectTimeFilter(TimeFilter.MONTH));
    }

    private void selectCategory(Category cat) {
        if (currentCategory == cat) return;
        currentCategory = cat;
        
        // Handle filter visibility for Streak
        if (cat == Category.STREAK) {
            filterToday.setVisibility(View.GONE);
            if (currentTimeFilter == TimeFilter.TODAY) {
                currentTimeFilter = TimeFilter.WEEK;
                updateFilterUI();
            }
        } else {
            filterToday.setVisibility(View.VISIBLE);
        }

        updateTabUI();
        loadData();
    }

    private void selectTimeFilter(TimeFilter filter) {
        if (currentTimeFilter == filter) return;
        currentTimeFilter = filter;
        updateFilterUI();
        loadData();
    }

    private void updateTabUI() {
        tabDistance.setBackground(null);
        tabDistance.setTextColor(ContextCompat.getColor(this, R.color.muted_fg));
        tabPace.setBackground(null);
        tabPace.setTextColor(ContextCompat.getColor(this, R.color.muted_fg));
        tabStreak.setBackground(null);
        tabStreak.setTextColor(ContextCompat.getColor(this, R.color.muted_fg));

        TextView selectedViewTab = tabDistance;
        if (currentCategory == Category.PACE) selectedViewTab = tabPace;
        else if (currentCategory == Category.STREAK) selectedViewTab = tabStreak;

        selectedViewTab.setBackgroundResource(R.drawable.tab_selected);
        selectedViewTab.setTextColor(ContextCompat.getColor(this, R.color.bg));
        selectedViewTab.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void updateFilterUI() {
        filterToday.setBackground(null);
        filterToday.setTextColor(ContextCompat.getColor(this, R.color.muted_fg));
        filterWeek.setBackground(null);
        filterWeek.setTextColor(ContextCompat.getColor(this, R.color.muted_fg));
        filterMonth.setBackground(null);
        filterMonth.setTextColor(ContextCompat.getColor(this, R.color.muted_fg));

        TextView selectedViewFilter = filterToday;
        if (currentTimeFilter == TimeFilter.WEEK) selectedViewFilter = filterWeek;
        else if (currentTimeFilter == TimeFilter.MONTH) selectedViewFilter = filterMonth;

        selectedViewFilter.setBackgroundResource(R.drawable.btn_outline_lime);
        selectedViewFilter.setTextColor(ContextCompat.getColor(this, R.color.lime));
        selectedViewFilter.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        
        Calendar cal = Calendar.getInstance();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        int thisWeek = cal.get(Calendar.WEEK_OF_YEAR);
        int thisMonth = cal.get(Calendar.MONTH);

        String orderByField;
        if (currentCategory == Category.DISTANCE) {
            if (currentTimeFilter == TimeFilter.TODAY) orderByField = "totalDistanceToday";
            else if (currentTimeFilter == TimeFilter.WEEK) orderByField = "totalDistanceWeek";
            else orderByField = "totalDistanceMonth";
        } else if (currentCategory == Category.PACE) {
            if (currentTimeFilter == TimeFilter.TODAY) orderByField = "bestPaceToday";
            else if (currentTimeFilter == TimeFilter.WEEK) orderByField = "bestPaceWeek";
            else orderByField = "bestPaceMonth";
        } else {
            if (currentTimeFilter == TimeFilter.WEEK) orderByField = "streakWeek";
            else if (currentTimeFilter == TimeFilter.MONTH) orderByField = "streakMonth";
            else orderByField = "currentStreak";
        }

        Query.Direction direction = (currentCategory == Category.PACE) ? Query.Direction.ASCENDING : Query.Direction.DESCENDING;

        FirebaseFirestore.getInstance().collection("users")
                .orderBy(orderByField, direction)
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    if (isDestroyed()) return;
                    progressBar.setVisibility(View.GONE);
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value == null) return;

                    List<User> users = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        User u = doc.toObject(User.class);
                        
                        // Automatic reset/filter check: only show users whose stats belong to current period
                        if (currentTimeFilter == TimeFilter.TODAY && !today.equals(u.getLastRunDate())) continue;
                        if (currentTimeFilter == TimeFilter.WEEK && thisWeek != u.getLastRunWeek()) continue;
                        if (currentTimeFilter == TimeFilter.MONTH && thisMonth != u.getLastRunMonth()) continue;

                        // Filter out users with default stats to avoid cluttering
                        if (currentCategory == Category.PACE) {
                            double p = (currentTimeFilter == TimeFilter.TODAY) ? u.getBestPaceToday() :
                                       (currentTimeFilter == TimeFilter.WEEK) ? u.getBestPaceWeek() : u.getBestPaceMonth();
                            if (p >= 999) continue;
                        }
                        if (currentCategory == Category.DISTANCE) {
                             double dist = (currentTimeFilter == TimeFilter.TODAY) ? u.getTotalDistanceToday() :
                                            (currentTimeFilter == TimeFilter.WEEK) ? u.getTotalDistanceWeek() : u.getTotalDistanceMonth();
                             if (dist <= 0) continue;
                        }
                        if (currentCategory == Category.STREAK && u.getCurrentStreak() <= 0) continue;
                        users.add(u);
                    }
                    processLeaderboard(users);
                });
    }

    private String formatName(String name) {
        if (name == null || name.isEmpty()) return "Runner";
        if (name.equals(getString(R.string.me))) return name;
        
        String[] parts = name.split("\\s+");
        if (parts.length >= 2) {
            return parts[0] + " " + parts[1].charAt(0) + ".";
        }
        return name;
    }

    private void processLeaderboard(List<User> users) {
        fullList.clear();
        
        for (int i = 0; i < Math.max(10, users.size()); i++) {
            String rankLabel = "#" + (i + 1);
            String name;
            String statStr;
            String subStat = "";
            boolean isMe = false;
            int icon = R.drawable.ic_person;

            if (i < users.size()) {
                User u = users.get(i);
                name = u.getName();
                if (name == null || name.isEmpty()) name = "Runner " + (i + 1);
                isMe = u.getUid() != null && u.getUid().equals(currentUid);
                if (isMe) name = getString(R.string.me);

                if (currentCategory == Category.DISTANCE) {
                    double d = (currentTimeFilter == TimeFilter.TODAY) ? u.getTotalDistanceToday() :
                               (currentTimeFilter == TimeFilter.WEEK) ? u.getTotalDistanceWeek() : u.getTotalDistanceMonth();
                    statStr = getString(R.string.label_km_stat, d);
                } else if (currentCategory == Category.PACE) {
                    double p = (currentTimeFilter == TimeFilter.TODAY) ? u.getBestPaceToday() :
                               (currentTimeFilter == TimeFilter.WEEK) ? u.getBestPaceWeek() : u.getBestPaceMonth();
                    int mins = (int)p;
                    int secs = (int)((p - mins) * 60);
                    statStr = getString(R.string.label_pace_stat, mins, secs);
                } else {
                    int s = (currentTimeFilter == TimeFilter.WEEK) ? u.getStreakWeek() :
                            (currentTimeFilter == TimeFilter.MONTH) ? u.getStreakMonth() : u.getCurrentStreak();
                    statStr = getString(R.string.label_consecutive_days, s);
                }

                if (currentCategory != Category.PACE) {
                    double p = u.getBestPace();
                    if (p < 999) {
                        int mins = (int)p;
                        int secs = (int)((p - mins) * 60);
                        subStat = getString(R.string.best_pace_format, String.format(Locale.getDefault(), "%d:%02d/km", mins, secs));
                    }
                }
                if (isMe) icon = R.drawable.ic_logo;
            } else {
                name = "Runner " + (i + 1);
                if (currentCategory == Category.DISTANCE) statStr = "0.0 km";
                else if (currentCategory == Category.STREAK) statStr = "0 days";
                else statStr = "--:-- /km";
            }

            if (i < 3) icon = R.drawable.ic_trophy;
            String displayName = (i < 3) ? formatName(name) : name;
            fullList.add(new LeaderboardUser(rankLabel, displayName, subStat, statStr, icon, isMe));
        }

        // Top 3 for Podium
        User u1 = !users.isEmpty() ? users.get(0) : null;
        User u2 = users.size() > 1 ? users.get(1) : null;
        User u3 = users.size() > 2 ? users.get(2) : null;
        // Animation if I am Top 3
        if ((u1 != null && u1.getUid().equals(currentUid)) || 
            (u2 != null && u2.getUid().equals(currentUid)) || 
            (u3 != null && u3.getUid().equals(currentUid))) {
            showCongratsAnimation();
        }

        // Sequential Podium Display (Always show, even if placeholders)
        updatePodium(fullList.get(0), fullList.get(1), fullList.get(2));

        List<LeaderboardUser> listItems = new ArrayList<>();
        if (fullList.size() > 3) {
            listItems.addAll(fullList.subList(3, fullList.size()));
        }
        
        adapter = new LeaderboardAdapter(listItems);
        adapter.enableSequentialAnimation(1300); // Start after podium
        rvRankings.setAdapter(adapter);
    }

    private void showCongratsAnimation() {
        View podium = findViewById(R.id.layoutPodium);
        podium.animate().scaleX(1.1f).scaleY(1.1f).setDuration(400).withEndAction(() -> 
            podium.animate().scaleX(1.0f).scaleY(1.0f).setDuration(400).start()
        ).start();
        Toast.makeText(this, "🎉 CONGRATULATIONS! You're on the podium! 🏆", Toast.LENGTH_LONG).show();
    }

    private void updatePodium(LeaderboardUser lu1, LeaderboardUser lu2, LeaderboardUser lu3) {
        // Reset scale and alpha for sequential entry animation
        containerRank1.setScaleX(0f); containerRank1.setScaleY(0f); containerRank1.setAlpha(0f);
        containerRank2.setScaleX(0f); containerRank2.setScaleY(0f); containerRank2.setAlpha(0f);
        containerRank3.setScaleX(0f); containerRank3.setScaleY(0f); containerRank3.setAlpha(0f);

        containerRank1.setVisibility(View.VISIBLE);
        containerRank2.setVisibility(View.VISIBLE);
        containerRank3.setVisibility(View.VISIBLE);

        // Juara 1
        tvName1.setText(lu1.getName());
        tvStat1.setText(lu1.getDistance());
        containerRank1.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(500).setStartDelay(200).start();

        // Juara 2
        tvName2.setText(lu2.getName());
        tvStat2.setText(lu2.getDistance());
        containerRank2.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(500).setStartDelay(500).start();

        // Juara 3
        tvName3.setText(lu3.getName());
        tvStat3.setText(lu3.getDistance());
        containerRank3.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(500).setStartDelay(800).start();
    }

    private String getStatString(User u) {
        if (currentCategory == Category.DISTANCE) {
            double d = (currentTimeFilter == TimeFilter.TODAY) ? u.getTotalDistanceToday() :
                       (currentTimeFilter == TimeFilter.WEEK) ? u.getTotalDistanceWeek() : u.getTotalDistanceMonth();
            return String.format(Locale.getDefault(), "%.1f km", d);
        } else if (currentCategory == Category.PACE) {
            double p = (currentTimeFilter == TimeFilter.TODAY) ? u.getBestPaceToday() :
                       (currentTimeFilter == TimeFilter.WEEK) ? u.getBestPaceWeek() : u.getBestPaceMonth();
            int mins = (int)p;
            int secs = (int)((p - mins) * 60);
            return String.format(Locale.getDefault(), "%d:%02d/km", mins, secs);
        } else {
            return u.getCurrentStreak() + " days";
        }
    }
}
