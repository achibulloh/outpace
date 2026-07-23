package com.example.pace.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.pace.R;
import com.example.pace.activities.LeaderboardActivity;
import com.example.pace.database.AppDatabase;
import com.example.pace.model.RunRecord;
import com.example.pace.utils.GeminiAssistant;
import com.example.pace.utils.LocaleHelper;
import com.example.pace.views.BarChartView;
import com.example.pace.views.CalendarDotsView;
import com.example.pace.views.LineChartView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ProgressFragment extends Fragment {

    private View progressMonthly, progressWeekly;
    private TextView tvTargetAchieved, tvTargetPercent, tvMonthlyTargetKm;
    private TextView tvWeeklyAchieved, tvWeeklyPercent, tvWeeklyTargetKm;
    private TextView tvWeeklyRange, tvCurrentMonthYear, tvPaceTrend, tvAIProgressInsights;
    private BarChartView barChart;
    private LineChartView lineChart;
    private CalendarDotsView calendarDots;

    private float monthlyTargetKm = 50.0f;
    private float weeklyTargetKm = 12.5f;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = null;
        try {
            view = inflater.inflate(R.layout.fragment_progress, container, false);
            initUI(view);
            
            View btnLeaderboard = view.findViewById(R.id.btnLeaderboard);
            if (btnLeaderboard != null) {
                btnLeaderboard.setOnClickListener(v -> {
                    if (isAdded()) startActivity(new Intent(getActivity(), LeaderboardActivity.class));
                });
            }
            
            loadPreferences();
            loadProgressData();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFirestorePreferences();
    }

    private void loadFirestorePreferences() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!isAdded() || getActivity() == null) return;
                        if (documentSnapshot.exists()) {
                            Object targetObj = documentSnapshot.get("monthly_target");
                            int monthlyTarget = 50; // Default
                            
                            if (targetObj != null) {
                                try {
                                    if (targetObj instanceof Number) {
                                        monthlyTarget = ((Number) targetObj).intValue();
                                    } else {
                                        monthlyTarget = (int) Double.parseDouble(String.valueOf(targetObj));
                                    }
                                } catch (Exception ignored) {}
                            }
                            
                            monthlyTargetKm = monthlyTarget;
                            weeklyTargetKm = monthlyTargetKm / 4.0f;
                            
                            tvMonthlyTargetKm.setText(getString(R.string.distance_km_val_short, monthlyTargetKm));
                            tvWeeklyTargetKm.setText(getString(R.string.distance_km_val, weeklyTargetKm));
                            loadProgressData(); // Refresh data with new targets
                        } else {
                            loadPreferences();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) loadPreferences();
                    });
        }
    }

    private void loadPreferences() {
        Context context = getContext();
        if (context == null) return;
        
        SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        monthlyTargetKm = prefs.getInt("monthly_target", 50);
        weeklyTargetKm = monthlyTargetKm / 4.0f;
        
        if (getActivity() != null) {
            tvMonthlyTargetKm.setText(getString(R.string.distance_km_val_short, monthlyTargetKm));
            tvWeeklyTargetKm.setText(getString(R.string.distance_km_val, weeklyTargetKm));
        }
    }

    private void initUI(View view) {
        progressMonthly = view.findViewById(R.id.progressMonthly);
        progressWeekly = view.findViewById(R.id.progressWeekly);
        
        tvMonthlyTargetKm = view.findViewById(R.id.tvMonthlyTargetKm);
        tvTargetAchieved = view.findViewById(R.id.tvTargetAchieved);
        tvTargetPercent = view.findViewById(R.id.tvTargetPercent);
        
        tvWeeklyTargetKm = view.findViewById(R.id.tvWeeklyTargetKm);
        tvWeeklyAchieved = view.findViewById(R.id.tvWeeklyAchieved);
        tvWeeklyPercent = view.findViewById(R.id.tvWeeklyPercent);
        
        tvWeeklyRange = view.findViewById(R.id.tvWeeklyRange);
        tvCurrentMonthYear = view.findViewById(R.id.tvCurrentMonthYear);
        tvAIProgressInsights = view.findViewById(R.id.tvAIProgressInsights);
        tvPaceTrend = view.findViewById(R.id.tvPaceTrend);
        
        barChart = view.findViewById(R.id.barChart);
        lineChart = view.findViewById(R.id.lineChart);
        calendarDots = view.findViewById(R.id.calendarDots);

        if (barChart != null) {
            barChart.setLabels(new String[]{
                    getString(R.string.day_mon_short),
                    getString(R.string.day_tue_short),
                    getString(R.string.day_wed_short),
                    getString(R.string.day_thu_short),
                    getString(R.string.day_fri_short),
                    getString(R.string.day_sat_short),
                    getString(R.string.day_sun_short)
            });
        }

        tvMonthlyTargetKm.setText(getString(R.string.distance_km_val_short, monthlyTargetKm));
        tvWeeklyTargetKm.setText(getString(R.string.distance_km_val, weeklyTargetKm));
        
        setLabels();
    }

    private void setLabels() {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        tvCurrentMonthYear.setText(new SimpleDateFormat("MMMM yyyy", new Locale("id", "ID")).format(cal.getTime()));
        
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String start = new SimpleDateFormat("d", new Locale("id", "ID")).format(cal.getTime());
        cal.add(Calendar.DAY_OF_WEEK, 6);
        String end = new SimpleDateFormat("d MMM yyyy", new Locale("id", "ID")).format(cal.getTime());
        tvWeeklyRange.setText(String.format("%s - %s", start, end));
    }

    private void loadProgressData() {
        Context context = getContext();
        if (context == null) return;
        
        new Thread(() -> {
            try {
                List<RunRecord> records = AppDatabase.getInstance(context).runDao().getAllRuns();
                if (isAdded() && getActivity() != null) {
                    calculateAndDisplay(records);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void calculateAndDisplay(List<RunRecord> records) {
        if (!isAdded() || getActivity() == null) return;
        fetchAIProgressInsights(records);

        double monthlyKm = 0;
        double weeklyKm = 0;
        Set<Integer> activeDays = new HashSet<>();
        
        float[] weeklyDistData = new float[7]; // Mon to Sun
        float[] weeklyPaceData = new float[4];
        int[] runCountPerWeek = new int[4];

        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        int curMonth = cal.get(Calendar.MONTH);
        int curYear = cal.get(Calendar.YEAR);
        int curWeek = cal.get(Calendar.WEEK_OF_YEAR);

        for (RunRecord r : records) {
            cal.setTimeInMillis(r.getTimestamp());
            int runMonth = cal.get(Calendar.MONTH);
            int runYear = cal.get(Calendar.YEAR);
            int runWeek = cal.get(Calendar.WEEK_OF_YEAR);
            int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

            if (runMonth == curMonth && runYear == curYear) {
                monthlyKm += r.getDistance();
                activeDays.add(dayOfMonth);
                
                int weekIndex = (dayOfMonth - 1) / 7;
                if (weekIndex < 4) {
                    weeklyPaceData[weekIndex] += (float) r.getPace();
                    runCountPerWeek[weekIndex]++;
                }
            }

            if (runWeek == curWeek && runYear == curYear) {
                weeklyKm += r.getDistance();
                
                // Map day of week to index 0-6 (Monday to Sunday)
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                int dayIndex = (dayOfWeek + 5) % 7; 
                weeklyDistData[dayIndex] += (float) r.getDistance();
            }
        }

        for (int i = 0; i < 4; i++) {
            if (runCountPerWeek[i] > 0) weeklyPaceData[i] /= runCountPerWeek[i];
        }

        final double finalMonthlyKm = monthlyKm;
        final double finalWeeklyKm = weeklyKm;
        final Set<Integer> finalActiveDays = activeDays;
        
        getActivity().runOnUiThread(() -> {
            if (!isAdded() || getActivity() == null) return;

            updateProgress(progressMonthly, tvTargetAchieved, tvTargetPercent, (float)finalMonthlyKm, monthlyTargetKm);
            updateProgress(progressWeekly, tvWeeklyAchieved, tvWeeklyPercent, (float)finalWeeklyKm, weeklyTargetKm);
            
            if (barChart != null) barChart.setData(weeklyDistData);
            if (lineChart != null) {
                lineChart.setDetailedData(weeklyPaceData, new String[]{"W1", "W2", "W3", "W4"});
            }
            if (calendarDots != null) calendarDots.setActiveDays(finalActiveDays);
            
            if (runCountPerWeek[3] > 0 && runCountPerWeek[2] > 0) {
                if (weeklyPaceData[3] < weeklyPaceData[2]) tvPaceTrend.setText(getString(R.string.pace_improving));
                else tvPaceTrend.setText(getString(R.string.pace_declining));
            }
        });
    }

    private void fetchAIProgressInsights(List<RunRecord> allRuns) {
        if (!isAdded() || getContext() == null) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || allRuns == null || allRuns.isEmpty()) return;

        // SMART CACHE (LOCAL + CLOUD)
        SharedPreferences prefs = getContext().getSharedPreferences("ai_cache", Context.MODE_PRIVATE);
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        String lang = LocaleHelper.getLanguage(getContext());
        String progressKey = "progress_" + allRuns.size() + "_" + today + "_" + lang;
        String cachedProgress = prefs.getString(progressKey, null);

        if (cachedProgress != null) {
            tvAIProgressInsights.setText(cachedProgress);
            return;
        }

        // CHECK CLOUD BEFORE CALLING AI
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    if (doc.exists() && doc.contains("cachedProgressInsights")) {
                        String cloudKey = doc.getString("cachedProgressKey");
                        if (progressKey.equals(cloudKey)) {
                            String cloudInsights = doc.getString("cachedProgressInsights");
                            if (cloudInsights != null) {
                                prefs.edit().putString(progressKey, cloudInsights).apply();
                                if (getActivity() != null) getActivity().runOnUiThread(() -> tvAIProgressInsights.setText(cloudInsights));
                                return;
                            }
                        }
                    }
                    
                    // CALL AI IF NO CACHE
                    generateNewProgressInsights(allRuns, user.getUid(), progressKey, prefs);
                });
    }

    private void generateNewProgressInsights(List<RunRecord> allRuns, String uid, String progressKey, SharedPreferences prefs) {
        if (!isAdded()) return;
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    com.example.pace.model.User userModel = doc.toObject(com.example.pace.model.User.class);
                    if (userModel == null) return;

                    StringBuilder history = new StringBuilder();
                    for (int i = 0; i < Math.min(3, allRuns.size()); i++) {
                        RunRecord r = allRuns.get(i);
                        history.append(String.format(Locale.getDefault(), "R%d: %.1fkm, P:%.1f. ", i+1, r.getDistance(), r.getPace()));
                    }

                    String prompt = String.format(Locale.getDefault(),
                        "History: %s. Goal: %s. Brief progress insight + 1 advice. Max 2 sentences.",
                        history.toString(), userModel.getGoal());

                    String firstName = userModel.getName() != null ? userModel.getName().split(" ")[0] : "Runner";
                    String lang = LocaleHelper.getLanguage(getContext());
                    GeminiAssistant.getInstance().chat(getContext(), prompt, firstName, true, lang, new GeminiAssistant.AIResponseCallback() {
                        @Override
                        public void onSuccess(String response) {
                            if (isAdded() && getActivity() != null) getActivity().runOnUiThread(() -> {
                                if (!response.contains("retry") && !response.contains("busy") && !response.contains("{") && !response.contains("Unexpected")) {
                                    prefs.edit().putString(progressKey, response).apply();
                                    // Save to Firestore
                                    Map<String, Object> data = new HashMap<>();
                                    data.put("cachedProgressKey", progressKey);
                                    data.put("cachedProgressInsights", response);
                                    FirebaseFirestore.getInstance().collection("users").document(uid).update(data);
                                }
                                tvAIProgressInsights.setText(response);
                            });
                        }
                        @Override
                        public void onError(String friendlyError) {
                            if (isAdded() && getActivity() != null) getActivity().runOnUiThread(() -> tvAIProgressInsights.setText(friendlyError));
                        }
                    });
                });
    }

    private void updateProgress(View bar, TextView tvAchieved, TextView tvPercent, float current, float target) {
        if (bar == null || tvAchieved == null || tvPercent == null) return;
        
        float percent = (target > 0) ? current / target : 0;
        if (percent > 1.0f) percent = 1.0f;
        final float finalPercent = percent;

        tvAchieved.setText(getString(R.string.target_achieved_format, current));
        tvPercent.setText(getString(R.string.percent_val, (int)(finalPercent * 100)));

        bar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (bar.getViewTreeObserver().isAlive()) {
                    bar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                if (!isAdded() || getActivity() == null) return;

                if (bar.getParent() instanceof FrameLayout) {
                    int parentWidth = ((FrameLayout) bar.getParent()).getWidth();
                    ViewGroup.LayoutParams lp = bar.getLayoutParams();
                    lp.width = (int)(parentWidth * finalPercent);
                    bar.setLayoutParams(lp);
                    bar.setScaleX(0f);
                    bar.setPivotX(0f);
                    bar.animate().scaleX(1f).setDuration(900).start();
                }
            }
        });
    }
}
