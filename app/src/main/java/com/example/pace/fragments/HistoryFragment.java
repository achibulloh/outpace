package com.example.pace.fragments;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private RunHistoryAdapter adapter;
    private List<RunRecord> allRecords = new ArrayList<>();
    private List<RunRecord> filteredList = new ArrayList<>();
    
    private TextView tvTotalDistance, tvTotalTime, tvAvgPace;
    private LinearLayout layoutSummary, layoutEmptyHistory;
    private TextView tabWeek, tabMonth, tabAll;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        rvHistory = view.findViewById(R.id.rvHistory);
        tvTotalDistance = view.findViewById(R.id.tvTotalDistance);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);
        tvAvgPace = view.findViewById(R.id.tvAvgPace);
        layoutSummary = view.findViewById(R.id.layoutSummary);
        layoutEmptyHistory = view.findViewById(R.id.layoutEmptyHistory);

        tabWeek = view.findViewById(R.id.tabWeek);
        tabMonth = view.findViewById(R.id.tabMonth);
        tabAll = view.findViewById(R.id.tabAll);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RunHistoryAdapter(filteredList);
        rvHistory.setAdapter(adapter);

        setupTabs();
        loadHistoryData();

        return view;
    }

    private void setupTabs() {
        tabWeek.setOnClickListener(v -> applyFilter("WEEK"));
        tabMonth.setOnClickListener(v -> applyFilter("MONTH"));
        tabAll.setOnClickListener(v -> applyFilter("ALL"));
    }

    private void applyFilter(String type) {
        updateTabUI(type);
        filteredList.clear();

        Calendar cal = Calendar.getInstance();
        int curMonth = cal.get(Calendar.MONTH);
        int curYear = cal.get(Calendar.YEAR);
        int curWeek = cal.get(Calendar.WEEK_OF_YEAR);

        for (RunRecord r : allRecords) {
            cal.setTimeInMillis(r.getTimestamp());
            boolean match = false;

            if ("ALL".equals(type)) {
                match = true;
            } else if ("MONTH".equals(type)) {
                if (cal.get(Calendar.MONTH) == curMonth && cal.get(Calendar.YEAR) == curYear) {
                    match = true;
                }
            } else if ("WEEK".equals(type)) {
                if (cal.get(Calendar.WEEK_OF_YEAR) == curWeek && cal.get(Calendar.YEAR) == curYear) {
                    match = true;
                }
            }

            if (match) filteredList.add(r);
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
        // Reset all
        tabWeek.setBackground(null);
        tabWeek.setTextColor(ContextCompat.getColor(requireContext(), R.color.muted_fg));
        tabMonth.setBackground(null);
        tabMonth.setTextColor(ContextCompat.getColor(requireContext(), R.color.muted_fg));
        tabAll.setBackground(null);
        tabAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.muted_fg));

        TextView activeTab = null;
        if ("WEEK".equals(type)) activeTab = tabWeek;
        else if ("MONTH".equals(type)) activeTab = tabMonth;
        else activeTab = tabAll;

        if (activeTab != null) {
            activeTab.setBackgroundResource(R.drawable.btn_lime);
            activeTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.bg));
            activeTab.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

    private void loadHistoryData() {
        new Thread(() -> {
            List<RunRecord> records = AppDatabase.getInstance(requireContext()).runDao().getAllRuns();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    allRecords.clear();
                    if (records != null) allRecords.addAll(records);
                    applyFilter("WEEK"); // Default
                });
            }
        }).start();
    }

    private void calculateSummary(List<RunRecord> records) {
        double totalDist = 0;
        long totalTimeSeconds = 0;
        
        for (RunRecord record : records) {
            totalDist += record.getDistance();
            totalTimeSeconds += record.getDuration();
        }

        tvTotalDistance.setText(String.format(Locale.getDefault(), "%.1f km", totalDist));
        
        long hours = totalTimeSeconds / 3600;
        long minutes = (totalTimeSeconds % 3600) / 60;
        tvTotalTime.setText(String.format(Locale.getDefault(), "%dj %dm", hours, minutes));

        if (totalDist > 0) {
            double avgPaceDecimal = (totalTimeSeconds / 60.0) / totalDist;
            int paceMins = (int) avgPaceDecimal;
            int paceSecs = (int) ((avgPaceDecimal - paceMins) * 60);
            tvAvgPace.setText(String.format(Locale.getDefault(), "%d:%02d", paceMins, paceSecs));
        } else {
            tvAvgPace.setText("0:00");
        }
    }
}
