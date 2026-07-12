package com.example.pace.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pace.R;
import com.example.pace.activities.NotificationActivity;
import com.example.pace.activities.WeatherActivity;
import com.example.pace.adapter.RecentActivityAdapter;
import com.example.pace.database.AppDatabase;
import com.example.pace.model.RunRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private RecyclerView rvLastActivities;
    private RecentActivityAdapter adapter;
    private List<RunRecord> lastRuns = new ArrayList<>();

    private TextView tvWeeklyProgress, tvProgressPercent, tvStreak, tvAvgPace, tvTotalCalories;
    private TextView tvWeeklyTargetLabel, tvGreetingHoliday, tvGreeting;
    private TextView tvDayMon, tvDayTue, tvDayWed, tvDayThu, tvDayFri, tvDaySat, tvDaySun;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyHome;
    private View viewNotificationBadge;

    private float weeklyTargetKm = 12.5f;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        TextView tvCurrentDate = view.findViewById(R.id.tvCurrentDate);
        setCurrentDate(tvCurrentDate);

        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvGreetingHoliday = view.findViewById(R.id.tvGreetingHoliday);
        tvWeeklyProgress = view.findViewById(R.id.tvWeeklyProgress);
        tvProgressPercent = view.findViewById(R.id.tvProgressPercent);
        tvWeeklyTargetLabel = view.findViewById(R.id.tvWeeklyTargetLabel);
        tvStreak = view.findViewById(R.id.tvStreak);
        tvAvgPace = view.findViewById(R.id.tvAvgPace);
        tvTotalCalories = view.findViewById(R.id.tvTotalCalories);
        
        tvDayMon = view.findViewById(R.id.tvDayMon);
        tvDayTue = view.findViewById(R.id.tvDayTue);
        tvDayWed = view.findViewById(R.id.tvDayWed);
        tvDayThu = view.findViewById(R.id.tvDayThu);
        tvDayFri = view.findViewById(R.id.tvDayFri);
        tvDaySat = view.findViewById(R.id.tvDaySat);
        tvDaySun = view.findViewById(R.id.tvDaySun);

        progressBar = view.findViewById(R.id.progressBar);
        layoutEmptyHome = view.findViewById(R.id.layoutEmptyHome);
        viewNotificationBadge = view.findViewById(R.id.viewNotificationBadge);

        rvLastActivities = view.findViewById(R.id.rvLastActivities);
        rvLastActivities.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecentActivityAdapter(lastRuns);
        rvLastActivities.setAdapter(adapter);

        view.findViewById(R.id.btnWeather).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), WeatherActivity.class));
        });

        view.findViewById(R.id.btnNotification).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), NotificationActivity.class));
        });

        view.findViewById(R.id.btnViewAll).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new HistoryFragment())
                    .addToBackStack(null)
                    .commit();
        });

        checkSpecialOccasions();

        return view;
    }

    private void checkSpecialOccasions() {
        // 1. Check Birthday
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String birthday = prefs.getString("dob", ""); // Format usually YYYY-MM-DD
        String name = prefs.getString("full_name", "Runner");
        
        Calendar cal = Calendar.getInstance();
        String today = String.format(Locale.getDefault(), "%02d-%02d", cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        
        if (birthday.endsWith(today)) {
            tvGreetingHoliday.setVisibility(View.VISIBLE);
            tvGreetingHoliday.setText("🎂 Selamat Ulang Tahun, " + name + "!");
            tvGreeting.setText("Rayakan dengan lari sehat! 🏃‍♂️");
        } else {
            // 2. Check Holiday from API (Indonesian Holidays)
            fetchHolidays();
        }
    }

    private void fetchHolidays() {
        String url = "https://api-harilibur.vercel.app/api";
        RequestQueue queue = Volley.newRequestQueue(requireContext());

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Calendar cal = Calendar.getInstance();
                        int year = cal.get(Calendar.YEAR);
                        int month = cal.get(Calendar.MONTH) + 1;
                        int day = cal.get(Calendar.DAY_OF_MONTH);
                        
                        String todayStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day);

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject holiday = response.getJSONObject(i);
                            if (todayStr.equals(holiday.getString("holiday_date"))) {
                                tvGreetingHoliday.setVisibility(View.VISIBLE);
                                tvGreetingHoliday.setText("🎉 Selamat " + holiday.getString("holiday_name") + "!");
                                return;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, error -> {});
        queue.add(request);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFirestorePreferences();
        loadHomeData();
        checkUnreadNotifications();
    }

    private void loadFirestorePreferences() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("name");
                            if (fullName == null) fullName = "Runner";
                            String firstName = fullName.split(" ")[0];
                            
                            Object targetObj = documentSnapshot.get("monthly_target");
                            int monthlyTarget = 50; 
                            
                            if (targetObj != null) {
                                try {
                                    if (targetObj instanceof Number) {
                                        monthlyTarget = ((Number) targetObj).intValue();
                                    } else {
                                        monthlyTarget = (int) Double.parseDouble(String.valueOf(targetObj));
                                    }
                                } catch (Exception ignored) {}
                            }
                            
                            weeklyTargetKm = monthlyTarget / 4.0f;
                            if (getActivity() != null) {
                                tvGreeting.setText("Hei, " + firstName + " 👋");
                                tvWeeklyTargetLabel.setText(String.format(Locale.getDefault(), "/ %.1f km", weeklyTargetKm));
                                // Refresh dashboard progress if we already have records
                                loadHomeData();
                            }
                        } else {
                            loadPreferences();
                        }
                    })
                    .addOnFailureListener(e -> loadPreferences());
        } else {
            loadPreferences();
        }
    }

    private void loadPreferences() {
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int monthlyTarget = prefs.getInt("monthly_target", 50);
        weeklyTargetKm = monthlyTarget / 4.0f;
        tvWeeklyTargetLabel.setText(String.format(Locale.getDefault(), "/ %.1f km", weeklyTargetKm));
        
        String fullName = prefs.getString("full_name", "Runner");
        String firstName = fullName.split(" ")[0];
        tvGreeting.setText("Hei, " + firstName + " 👋");
    }

    private void checkUnreadNotifications() {
        new Thread(() -> {
            int unreadCount = AppDatabase.getInstance(requireContext()).notificationDao().getUnreadCount();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    viewNotificationBadge.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
                });
            }
        }).start();
    }

    private void loadHomeData() {
        Context context = getContext();
        if (context == null) return;
        
        new Thread(() -> {
            List<RunRecord> allRecords = AppDatabase.getInstance(context).runDao().getAllRuns();
            if (getActivity() != null && isAdded()) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    lastRuns.clear();
                    if (allRecords == null || allRecords.isEmpty()) {
                        layoutEmptyHome.setVisibility(View.VISIBLE);
                        rvLastActivities.setVisibility(View.GONE);
                        updateDashboard(new ArrayList<>());
                    } else {
                        layoutEmptyHome.setVisibility(View.GONE);
                        rvLastActivities.setVisibility(View.VISIBLE);
                        // Show up to 10 most recent activities
                        for (int i = 0; i < Math.min(allRecords.size(), 10); i++) {
                            lastRuns.add(allRecords.get(i));
                        }
                        updateDashboard(allRecords);
                    }
                    adapter.notifyDataSetChanged();
                    syncHomeFromFirebase();
                });
            }
        }).start();
    }

    private void syncHomeFromFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !isAdded()) return;

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("runs")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;
                    
                    List<RunRecord> cloudRecords = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        cloudRecords.add(doc.toObject(RunRecord.class));
                    }

                    if (!cloudRecords.isEmpty()) {
                        new Thread(() -> {
                            Context context = getContext();
                            if (context == null || !isAdded()) return;
                            
                            AppDatabase.getInstance(context).runDao().insertAll(cloudRecords);
                            List<RunRecord> updatedRecords = AppDatabase.getInstance(context).runDao().getAllRuns();
                            if (getActivity() != null && isAdded()) {
                                getActivity().runOnUiThread(() -> {
                                    if (!isAdded()) return;
                                    lastRuns.clear();
                                    layoutEmptyHome.setVisibility(View.GONE);
                                    rvLastActivities.setVisibility(View.VISIBLE);
                                    for (int i = 0; i < Math.min(updatedRecords.size(), 10); i++) {
                                        lastRuns.add(updatedRecords.get(i));
                                    }
                                    updateDashboard(updatedRecords);
                                    adapter.notifyDataSetChanged();
                                });
                            }
                        }).start();
                    }
                });
    }

    private void updateDashboard(List<RunRecord> records) {
        double totalKm = 0;
        double currentWeekKm = 0;
        int totalCal = 0;
        double totalPaceDecimal = 0;
        int count = records.size();

        Calendar cal = Calendar.getInstance();
        int curWeek = cal.get(Calendar.WEEK_OF_YEAR);
        int curYear = cal.get(Calendar.YEAR);

        for (RunRecord r : records) {
            totalKm += r.getDistance();
            totalCal += r.getCalories();
            totalPaceDecimal += r.getPace();

            cal.setTimeInMillis(r.getTimestamp());
            if (cal.get(Calendar.WEEK_OF_YEAR) == curWeek && cal.get(Calendar.YEAR) == curYear) {
                currentWeekKm += r.getDistance();
            }
        }

        tvWeeklyProgress.setText(String.format(Locale.getDefault(), "%.1f", currentWeekKm));
        tvTotalCalories.setText(String.valueOf(totalCal));
        
        int progress = (int) ((currentWeekKm / weeklyTargetKm) * 100);
        progressBar.setProgress(Math.min(progress, 100));
        tvProgressPercent.setText(Math.min(progress, 100) + "%");

        if (count > 0) {
            double avgPace = totalPaceDecimal / count;
            int mins = (int) avgPace;
            int secs = (int) ((avgPace - mins) * 60);
            tvAvgPace.setText(String.format(Locale.getDefault(), "%d:%02d", mins, secs));
        } else {
            tvAvgPace.setText("0:00");
        }

        tvStreak.setText(String.valueOf(count));
        
        updateDayCircles(records);
    }

    private void updateDayCircles(List<RunRecord> records) {
        TextView[] dayViews = {tvDayMon, tvDayTue, tvDayWed, tvDayThu, tvDayFri, tvDaySat, tvDaySun};
        boolean[] hasRun = new boolean[7];

        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        int curWeek = cal.get(Calendar.WEEK_OF_YEAR);
        int curYear = cal.get(Calendar.YEAR);
        int todayIndex = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7; // 0=Mon, 6=Sun

        for (RunRecord r : records) {
            cal.setTimeInMillis(r.getTimestamp());
            if (cal.get(Calendar.WEEK_OF_YEAR) == curWeek && cal.get(Calendar.YEAR) == curYear) {
                int dayIndex = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
                hasRun[dayIndex] = true;
            }
        }

        for (int i = 0; i < 7; i++) {
            TextView tv = dayViews[i];
            if (hasRun[i]) {
                tv.setBackgroundResource(R.drawable.day_active);
                tv.setTextColor(getResources().getColor(R.color.bg));
            } else if (i < todayIndex) {
                tv.setBackgroundResource(R.drawable.day_missed);
                tv.setTextColor(getResources().getColor(R.color.text_primary));
            } else {
                tv.setBackgroundResource(R.drawable.day_future);
                tv.setTextColor(getResources().getColor(R.color.muted_fg));
            }
        }
    }

    private void setCurrentDate(TextView tvDate) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMM yyyy", new Locale("id", "ID"));
        String date = dateFormat.format(calendar.getTime()).toUpperCase();
        tvDate.setText(date);
    }
}
