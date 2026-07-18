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
import com.example.pace.utils.LocaleHelper;
import com.example.pace.utils.VolleySingleton;

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
import com.android.volley.toolbox.JsonObjectRequest;
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
    private TextView tvWeeklyTargetLabel, tvGreetingHoliday, tvGreeting, tvWeatherTemp, tvWeatherIcon, tvWeatherMotivation;
    private TextView tvDayMon, tvDayTue, tvDayWed, tvDayThu, tvDayFri, tvDaySat, tvDaySun;
    private TextView tvTargetTitle, tvStreakUnit, tvStreakLabel, tvAvgPaceUnit, tvAvgPaceLabel, tvCaloriesUnit, tvCaloriesLabel, tvLastActivityTitle, tvEmptyTitle, tvEmptyDesc;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyHome;
    private View viewNotificationBadge, aiTipDivider;
    private TextView tvAIDailyTip;

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
        
        tvWeatherTemp = view.findViewById(R.id.tvWeatherTemp);
        tvWeatherIcon = view.findViewById(R.id.tvWeatherIcon);
        tvWeatherMotivation = view.findViewById(R.id.tvWeatherMotivation);
        tvAIDailyTip = view.findViewById(R.id.tvAIDailyTip);
        aiTipDivider = view.findViewById(R.id.aiTipDivider);

        tvTargetTitle = view.findViewById(R.id.tvTargetTitle);
        tvStreakUnit = view.findViewById(R.id.tvStreakUnit);
        tvStreakLabel = view.findViewById(R.id.tvStreakLabel);
        tvAvgPaceUnit = view.findViewById(R.id.tvAvgPaceUnit);
        tvAvgPaceLabel = view.findViewById(R.id.tvAvgPaceLabel);
        tvCaloriesUnit = view.findViewById(R.id.tvCaloriesUnit);
        tvCaloriesLabel = view.findViewById(R.id.tvCaloriesLabel);
        tvLastActivityTitle = view.findViewById(R.id.tvLastActivityTitle);
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        tvEmptyDesc = view.findViewById(R.id.tvEmptyDesc);
        
        setupLabels(view);
        setupDayLabels();

        progressBar = view.findViewById(R.id.progressBar);
        layoutEmptyHome = view.findViewById(R.id.layoutEmptyHome);
        viewNotificationBadge = view.findViewById(R.id.viewNotificationBadge);

        rvLastActivities = view.findViewById(R.id.rvLastActivities);
        rvLastActivities.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecentActivityAdapter(lastRuns);
        rvLastActivities.setAdapter(adapter);

        TextView btnViewAll = view.findViewById(R.id.btnViewAll);
        btnViewAll.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new HistoryFragment())
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.btnWeather).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), WeatherActivity.class));
        });

        view.findViewById(R.id.btnAICoach).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), com.example.pace.activities.GeminiChatActivity.class));
        });

        view.findViewById(R.id.btnNotification).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), NotificationActivity.class));
        });

        checkSpecialOccasions();
        fetchAIDailyTip();

        return view;
    }

    private void fetchAIDailyTip() {
        Context context = getContext();
        if (context == null || !isAdded()) return;
        
        SharedPreferences prefs = context.getSharedPreferences("ai_cache", Context.MODE_PRIVATE);
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        String cachedTip = prefs.getString("daily_tip_" + today, null);

        if (cachedTip != null) {
            aiTipDivider.setVisibility(View.VISIBLE);
            tvAIDailyTip.setVisibility(View.VISIBLE);
            tvAIDailyTip.setText("Coach Tip: " + cachedTip);
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    com.example.pace.model.User u = doc.toObject(com.example.pace.model.User.class);
                    if (u == null) return;

                    String prompt = "Give me one short, unique running tip for today. My goal is " + u.getGoal() + ". Max 10 words.";
                    String firstName = u.getName() != null ? u.getName().split(" ")[0] : "Runner";
                    new com.example.pace.utils.GeminiAssistant().chat(prompt, firstName, true, new com.example.pace.utils.GeminiAssistant.AIResponseCallback() {
                        @Override
                        public void onSuccess(String response) {
                            if (isAdded() && getActivity() != null) getActivity().runOnUiThread(() -> {
                                if (!isAdded()) return;
                                // Filter out busy/retry messages
                                if (response.contains("busy") || response.contains("retry") || response.contains("unavailable") || response.contains("Error")) {
                                    tvAIDailyTip.setVisibility(View.GONE);
                                    aiTipDivider.setVisibility(View.GONE);
                                } else {
                                    prefs.edit().putString("daily_tip_" + today, response).apply();
                                    aiTipDivider.setVisibility(View.VISIBLE);
                                    tvAIDailyTip.setVisibility(View.VISIBLE);
                                    tvAIDailyTip.setText("Coach Tip: " + response);
                                }
                            });
                        }
                        @Override
                        public void onError(String friendlyError) {
                            if (isAdded() && getActivity() != null) getActivity().runOnUiThread(() -> {
                                if (!isAdded()) return;
                                tvAIDailyTip.setVisibility(View.GONE);
                                aiTipDivider.setVisibility(View.GONE);
                            });
                        }
                    });
                });
    }

    private void setupLabels(View view) {
        tvTargetTitle.setText(R.string.weekly_target);
        tvStreakUnit.setText(R.string.days_unit);
        tvStreakLabel.setText(R.string.streak);
        tvAvgPaceUnit.setText(R.string.pace_unit);
        tvAvgPaceLabel.setText(R.string.avg_pace);
        tvCaloriesUnit.setText(R.string.kcal);
        tvCaloriesLabel.setText(R.string.calories);
        tvLastActivityTitle.setText(R.string.last_activity);
        tvEmptyTitle.setText(R.string.no_activity_title);
        tvEmptyDesc.setText(R.string.no_activity_desc);
        ((TextView)view.findViewById(R.id.btnViewAll)).setText(R.string.view_all);
    }

    private void setupDayLabels() {
        tvDayMon.setText(R.string.day_mon);
        tvDayTue.setText(R.string.day_tue);
        tvDayWed.setText(R.string.day_wed);
        tvDayThu.setText(R.string.day_thu);
        tvDayFri.setText(R.string.day_fri);
        tvDaySat.setText(R.string.day_sat);
        tvDaySun.setText(R.string.day_sun);
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
            tvGreetingHoliday.setText(getString(R.string.birthday_greeting, name));
            tvGreeting.setText(getString(R.string.birthday_sub));
        } else {
            // 2. Check Holiday from API (Indonesian Holidays)
            fetchHolidays();
        }
    }

    private void fetchHolidays() {
        String url = "https://api-harilibur.vercel.app/api";

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
                                tvGreetingHoliday.setText(getString(R.string.holiday_greeting, holiday.getString("holiday_name")));
                                return;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, error -> {});
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFirestorePreferences();
        loadHomeData();
        checkUnreadNotifications();
        fetchWeather();
    }

    private void fetchWeather() {
        String city = getString(R.string.weather_jakarta);
        String lang = LocaleHelper.getLanguage(requireContext());
        if (lang.equals("in")) lang = "id";
        
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=565c5268e3209aa0aa5599c00cc0c232&units=metric&lang=" + lang;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject main = response.getJSONObject("main");
                        double temp = main.getDouble("temp");
                        
                        JSONObject weather = response.getJSONArray("weather").getJSONObject(0);
                        String desc = weather.getString("description");
                        String icon = weather.getString("icon");
                        
                        if (getActivity() != null) {
                            tvWeatherTemp.setText(String.format(Locale.getDefault(), getString(R.string.weather_temp_format), temp, 
                                    desc.substring(0, 1).toUpperCase() + desc.substring(1)));
                            tvWeatherIcon.setText(getWeatherEmoji(icon));
                            tvWeatherMotivation.setText(R.string.weather_run_motivation);
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }, error -> {});
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    private String getWeatherEmoji(String iconCode) {
        switch (iconCode) {
            case "01d": return "☀️";
            case "01n": return "🌙";
            case "02d": case "02n": return "⛅";
            case "03d": case "03n": return "☁️";
            case "04d": case "04n": return "☁️";
            case "09d": case "09n": return "🌧️";
            case "10d": case "10n": return "🌦️";
            case "11d": case "11n": return "⛈️";
            case "13d": case "13n": return "❄️";
            case "50d": case "50n": return "🌫️";
            default: return "☀️";
        }
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
                                tvGreeting.setText(getString(R.string.greeting_format, firstName));
                                tvWeeklyTargetLabel.setText(getString(R.string.distance_target_format, weeklyTargetKm));
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
        tvWeeklyTargetLabel.setText(getString(R.string.distance_target_format, weeklyTargetKm));
        
        String fullName = prefs.getString("full_name", "Runner");
        String firstName = fullName.split(" ")[0];
        tvGreeting.setText(getString(R.string.greeting_format, firstName));
    }

    private void checkUnreadNotifications() {
        Context context = getContext();
        if (context == null || !isAdded()) return;
        
        new Thread(() -> {
            try {
                int unreadCount = AppDatabase.getInstance(context).notificationDao().getUnreadCount();
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        viewNotificationBadge.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
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
        tvProgressPercent.setText(getString(R.string.percent_val, Math.min(progress, 100)));

        if (count > 0) {
            double avgPace = totalPaceDecimal / count;
            int mins = (int) avgPace;
            int secs = (int) ((avgPace - mins) * 60);
            tvAvgPace.setText(getString(R.string.pace_val_no_unit, mins, secs));
        } else {
            tvAvgPace.setText(getString(R.string.pace_val_no_unit, 0, 0));
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault());
        String date = dateFormat.format(calendar.getTime()).toUpperCase();
        tvDate.setText(date);
    }
}
