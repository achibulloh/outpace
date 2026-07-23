package com.example.pace.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pace.R;
import com.example.pace.adapter.RunHistoryAdapter;
import com.example.pace.database.AppDatabase;
import com.example.pace.model.RunRecord;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private RunHistoryAdapter adapter;
    private List<RunRecord> allRecords = new ArrayList<>();
    private List<RunRecord> filteredList = new ArrayList<>();
    
    private TextView tvTotalDistance, tvTotalTime, tvAvgPace, tvHistoryRange;
    private LinearLayout layoutSummary, layoutEmptyHistory, btnCustomFilter;
    private TextView tabWeek, tabMonth, tabAll;
    
    private long customStart = -1, customEnd = -1;
    private String currentFilter = "WEEK";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        rvHistory = view.findViewById(R.id.rvHistory);
        tvTotalDistance = view.findViewById(R.id.tvTotalDistance);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);
        tvAvgPace = view.findViewById(R.id.tvAvgPace);
        tvHistoryRange = view.findViewById(R.id.tvHistoryRange);
        layoutSummary = view.findViewById(R.id.layoutSummary);
        layoutEmptyHistory = view.findViewById(R.id.layoutEmptyHistory);
        btnCustomFilter = view.findViewById(R.id.btnCustomFilter);

        tabWeek = view.findViewById(R.id.tabWeek);
        tabMonth = view.findViewById(R.id.tabMonth);
        tabAll = view.findViewById(R.id.tabAll);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RunHistoryAdapter(filteredList);
        rvHistory.setAdapter(adapter);

        setupTabs();
        loadHistoryData();

        if (btnCustomFilter != null) {
            btnCustomFilter.setOnClickListener(v -> showDateRangePicker());
        }

        return view;
    }

    private void setupTabs() {
        tabWeek.setOnClickListener(v -> { currentFilter = "WEEK"; applyFilter("WEEK"); });
        tabMonth.setOnClickListener(v -> { currentFilter = "MONTH"; applyFilter("MONTH"); });
        tabAll.setOnClickListener(v -> { currentFilter = "ALL"; applyFilter("ALL"); });
    }

    private void showDateRangePicker() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .build();

        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Period")
                .setCalendarConstraints(constraints)
                .build();
                
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection.first != null && selection.second != null) {
                customStart = selection.first;
                customEnd = selection.second + 86399999; // Till end of day
                applyFilter("CUSTOM");
            }
        });
        picker.show(getChildFragmentManager(), "DATE_PICKER");
    }

    private void applyFilter(String type) {
        updateTabUI(type);
        filteredList.clear();

        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        
        long startRange = 0;
        long endRange = Long.MAX_VALUE;
        String rangeText = "";

        SimpleDateFormat sdfRange = new SimpleDateFormat("d MMM", Locale.getDefault());
        SimpleDateFormat sdfMonth = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        if ("WEEK".equals(type)) {
            if (btnCustomFilter != null) btnCustomFilter.setVisibility(View.GONE);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            startRange = cal.getTimeInMillis();
            
            String startDate = sdfRange.format(cal.getTime());
            cal.add(Calendar.DAY_OF_WEEK, 6);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            endRange = cal.getTimeInMillis();
            String endDate = sdfRange.format(cal.getTime());
            
            rangeText = startDate + " - " + endDate;
        } else if ("MONTH".equals(type)) {
            if (btnCustomFilter != null) btnCustomFilter.setVisibility(View.GONE);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            startRange = cal.getTimeInMillis();
            
            rangeText = sdfMonth.format(cal.getTime());
            
            cal.add(Calendar.MONTH, 1);
            cal.add(Calendar.SECOND, -1);
            endRange = cal.getTimeInMillis();
        } else if ("ALL".equals(type)) {
            if (btnCustomFilter != null) btnCustomFilter.setVisibility(View.VISIBLE);
            rangeText = "All Activities";
            startRange = 0;
            endRange = Long.MAX_VALUE;
        } else if ("CUSTOM".equals(type)) {
            if (btnCustomFilter != null) btnCustomFilter.setVisibility(View.VISIBLE);
            startRange = customStart;
            endRange = customEnd;
            rangeText = sdfRange.format(new Date(startRange)) + " - " + sdfRange.format(new Date(endRange));
        }

        if (tvHistoryRange != null) tvHistoryRange.setText(rangeText);

        for (RunRecord r : allRecords) {
            if (r.getTimestamp() >= startRange && r.getTimestamp() <= endRange) {
                filteredList.add(r);
            }
        }

        if (filteredList.isEmpty()) {
            layoutSummary.setVisibility(View.GONE);
            layoutEmptyHistory.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
        } else {
            layoutSummary.setVisibility(View.VISIBLE);
            layoutEmptyHistory.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
            calculateSummary(filteredList);
        }
        adapter.notifyDataSetChanged();
    }

    private void updateTabUI(String type) {
        Context context = getContext();
        if (context == null || !isAdded()) return;
        
        // Reset all
        tabWeek.setBackground(null);
        tabWeek.setTextColor(ContextCompat.getColor(context, R.color.muted_fg));
        tabMonth.setBackground(null);
        tabMonth.setTextColor(ContextCompat.getColor(context, R.color.muted_fg));
        tabAll.setBackground(null);
        tabAll.setTextColor(ContextCompat.getColor(context, R.color.muted_fg));

        TextView activeTab = null;
        if ("WEEK".equals(type)) activeTab = tabWeek;
        else if ("MONTH".equals(type)) activeTab = tabMonth;
        else if ("ALL".equals(type) || "CUSTOM".equals(type)) activeTab = tabAll;

        if (activeTab != null) {
            activeTab.setBackgroundResource(R.drawable.btn_lime);
            activeTab.setTextColor(ContextCompat.getColor(context, R.color.bg));
        }
    }

    private void loadHistoryData() {
        Context context = getContext();
        if (context == null) return;

        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                List<RunRecord> localRecords = db.runDao().getAllRuns();
                long latestLocal = db.runDao().getLatestTimestamp();

                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        allRecords.clear();
                        if (localRecords != null) allRecords.addAll(localRecords);
                        applyFilter(currentFilter); 
                        syncFromFirebase(latestLocal); 
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void syncFromFirebase(long sinceTimestamp) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !isAdded()) return;

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("runs")
                .whereGreaterThan("timestamp", sinceTimestamp)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || queryDocumentSnapshots.isEmpty()) return;
                    
                    List<RunRecord> cloudRecords = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            RunRecord r = doc.toObject(RunRecord.class);
                            if (r != null) {
                                r.setSynced(true);
                                cloudRecords.add(r);
                            }
                        } catch (Exception ignored) {}
                    }
                    
                    if (!cloudRecords.isEmpty()) {
                        new Thread(() -> {
                            Context context = getContext();
                            if (context == null || !isAdded()) return;
                            
                            try {
                                AppDatabase db = AppDatabase.getInstance(context);
                                db.runDao().insertAll(cloudRecords);
                                List<RunRecord> updatedRecords = db.runDao().getAllRuns();
                                
                                if (getActivity() != null && isAdded()) {
                                    getActivity().runOnUiThread(() -> {
                                        if (!isAdded()) return;
                                        allRecords.clear();
                                        allRecords.addAll(updatedRecords);
                                        applyFilter(currentFilter);
                                    });
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                });
    }

    private void calculateSummary(List<RunRecord> records) {
        double totalDist = 0;
        long totalTimeSeconds = 0;
        
        for (RunRecord record : records) {
            totalDist += record.getDistance();
            totalTimeSeconds += record.getDuration();
        }

        tvTotalDistance.setText(getString(R.string.distance_km_val, totalDist));
        
        long hours = totalTimeSeconds / 3600;
        long minutes = (totalTimeSeconds % 3600) / 60;
        
        String timeStr = getString(R.string.duration_format_short, hours, minutes);
        tvTotalTime.setText(timeStr);

        if (totalDist > 0) {
            double avgPaceDecimal = (totalTimeSeconds / 60.0) / totalDist;
            int paceMins = (int) avgPaceDecimal;
            int paceSecs = (int) ((avgPaceDecimal - paceMins) * 60);
            tvAvgPace.setText(getString(R.string.pace_val_no_unit, paceMins, paceSecs));
        } else {
            tvAvgPace.setText(getString(R.string.pace_val_no_unit, 0, 0));
        }
    }
}
