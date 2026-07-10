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
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyHome;
    private View viewNotificationBadge;

    private float weeklyTargetKm = 40.0f;

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
        loadPreferences();
        loadHomeData();
        checkUnreadNotifications();
    }

    private void loadPreferences() {
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int monthlyTarget = prefs.getInt("monthly_target", 150);
        weeklyTargetKm = monthlyTarget / 4.0f;
        tvWeeklyTargetLabel.setText(String.format(Locale.getDefault(), "/ %.1f km", weeklyTargetKm));
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
        new Thread(() -> {
            List<RunRecord> allRecords = AppDatabase.getInstance(requireContext()).runDao().getAllRuns();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    lastRuns.clear();
                    if (allRecords == null || allRecords.isEmpty()) {
                        layoutEmptyHome.setVisibility(View.VISIBLE);
                        rvLastActivities.setVisibility(View.GONE);
                        updateDashboard(new ArrayList<>());
                    } else {
                        layoutEmptyHome.setVisibility(View.GONE);
                        rvLastActivities.setVisibility(View.VISIBLE);
                        for (int i = 0; i < Math.min(allRecords.size(), 3); i++) {
                            lastRuns.add(allRecords.get(i));
                        }
                        updateDashboard(allRecords);
                    }
                    adapter.notifyDataSetChanged();
                });
            }
        }).start();
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
    }

    private void setCurrentDate(TextView tvDate) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMM yyyy", new Locale("id", "ID"));
        String date = dateFormat.format(calendar.getTime()).toUpperCase();
        tvDate.setText(date);
    }
}
