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
import com.example.pace.views.BarChartView;
import com.example.pace.views.CalendarDotsView;
import com.example.pace.views.LineChartView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProgressFragment extends Fragment {

    private View progressMonthly, progressWeekly;
    private TextView tvTargetAchieved, tvTargetPercent, tvMonthlyTargetKm;
    private TextView tvWeeklyAchieved, tvWeeklyPercent, tvWeeklyTargetKm;
    private TextView tvWeeklyRange, tvCurrentMonthYear, tvPaceTrend;
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
                            
                            tvMonthlyTargetKm.setText(String.format(Locale.getDefault(), "%.0f km", monthlyTargetKm));
                            tvWeeklyTargetKm.setText(String.format(Locale.getDefault(), "%.1f km", weeklyTargetKm));
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
            tvMonthlyTargetKm.setText(String.format(Locale.getDefault(), "%.0f km", monthlyTargetKm));
            tvWeeklyTargetKm.setText(String.format(Locale.getDefault(), "%.1f km", weeklyTargetKm));
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
        tvPaceTrend = view.findViewById(R.id.tvPaceTrend);
        
        barChart = view.findViewById(R.id.barChart);
        lineChart = view.findViewById(R.id.lineChart);
        calendarDots = view.findViewById(R.id.calendarDots);

        if (barChart != null) {
            barChart.setLabels(new String[]{"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"});
        }

        tvMonthlyTargetKm.setText(String.format(Locale.getDefault(), "%.0f km", monthlyTargetKm));
        tvWeeklyTargetKm.setText(String.format(Locale.getDefault(), "%.1f km", weeklyTargetKm));
        
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
                if (weeklyPaceData[3] < weeklyPaceData[2]) tvPaceTrend.setText("Membaik");
                else tvPaceTrend.setText("Menurun");
            }
        });
    }

    private void updateProgress(View bar, TextView tvAchieved, TextView tvPercent, float current, float target) {
        if (bar == null || tvAchieved == null || tvPercent == null) return;
        
        float percent = (target > 0) ? current / target : 0;
        if (percent > 1.0f) percent = 1.0f;
        final float finalPercent = percent;

        tvAchieved.setText(String.format(Locale.getDefault(), "%.1f km tercapai", current));
        tvPercent.setText(String.format(Locale.getDefault(), "%d%%", (int)(finalPercent * 100)));

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
