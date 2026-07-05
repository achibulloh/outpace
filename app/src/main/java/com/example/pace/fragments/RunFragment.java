package com.example.pace.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.pace.R;

public class RunFragment extends Fragment {

    private boolean isRunning = false;
    private int runSeconds = 0;
    private double runDistKm = 0.0;
    private final Handler handler = new Handler();
    private TextView tvDuration, tvDistance, tvPace, tvCalories, tvSteps;
    private Button btnPlayPause;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                runSeconds++;
                runDistKm += 0.00083;
                updateUI();
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_run, container, false);

        tvDuration  = view.findViewById(R.id.tvDuration);
        tvDistance  = view.findViewById(R.id.tvDistance);
        tvPace      = view.findViewById(R.id.tvPace);
        tvCalories  = view.findViewById(R.id.tvCalories);
        tvSteps     = view.findViewById(R.id.tvSteps);
        btnPlayPause= view.findViewById(R.id.btnPlayPause);

        btnPlayPause.setOnClickListener(v -> toggleRun());
        return view;
    }

    private void toggleRun() {
        isRunning = !isRunning;
        if (isRunning) {
            btnPlayPause.setText("⏸  Pause");
            handler.postDelayed(timerRunnable, 1000);
        } else {
            btnPlayPause.setText("▶  Mulai");
            handler.removeCallbacks(timerRunnable);
        }
    }

    private void updateUI() {
        tvDuration.setText(String.format("%02d:%02d", runSeconds/60, runSeconds%60));
        tvDistance.setText(String.format("%.2f", runDistKm));

        if (runDistKm > 0) {
            double minPerKm = (runSeconds / 60.0) / runDistKm;
            int pm = (int) minPerKm;
            int ps = (int) Math.round((minPerKm - pm) * 60);
            tvPace.setText(String.format("%d:%02d", pm, ps));
        }

        tvCalories.setText(String.valueOf((int)(runSeconds * 0.12)));
        tvSteps.setText(String.format("%,d", (int)(runSeconds * 2.8)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(timerRunnable);
    }
}