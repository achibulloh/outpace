package com.example.pace.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.pace.R;
import com.example.pace.database.AppDatabase;
import com.example.pace.model.RunRecord;
import com.example.pace.views.LineChartView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.TilesOverlay;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActivityDetailActivity extends AppCompatActivity {

    private MapView map;
    private View btnRingkasan, btnSplit;
    private View layoutRingkasan, layoutSplit;
    private LinearLayout llSplitContainer;
    
    private TextView tvDistanceTop, tvTitle, tvDateTime, tvLocation, tvDistance, tvAvgPace, tvDuration;
    private TextView tvCalories, tvSteps, tvElevation, tvMaxElev;
    private TextView tvPaceAvg, tvPaceTotal, tvPaceFast, tvCadenceAvg, tvCadenceMax, tvElevGain, tvElevMax;
    
    private LineChartView chartPace, chartCadence, chartElevation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE));
        
        setContentView(R.layout.activity_detail);

        initViews();
        
        int runId = getIntent().getIntExtra("RUN_ID", -1);
        if (runId != -1) {
            loadRunData(runId);
        }

        btnRingkasan.setOnClickListener(v -> showRingkasan());
        btnSplit.setOnClickListener(v -> showSplit());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        findViewById(R.id.btnShare).setOnClickListener(v -> {
            Intent intent = new Intent(this, ShareActivity.class);
            intent.putExtra("RUN_ID", runId);
            startActivity(intent);
        });

        findViewById(R.id.btnShareTop).setOnClickListener(v -> {
            Intent intent = new Intent(this, ShareActivity.class);
            intent.putExtra("RUN_ID", runId);
            startActivity(intent);
        });

        showRingkasan();
    }

    private void initViews() {
        map = findViewById(R.id.mapDetail);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
        map.getController().setZoom(16.0);

        btnRingkasan = findViewById(R.id.btnTabRingkasan);
        btnSplit = findViewById(R.id.btnTabSplit);
        layoutRingkasan = findViewById(R.id.layoutRingkasan);
        layoutSplit = findViewById(R.id.layoutSplit);
        llSplitContainer = findViewById(R.id.llSplitContainer);

        tvDistanceTop = findViewById(R.id.tvDetailDistanceTop);
        tvTitle = findViewById(R.id.tvDetailRunTitle);
        tvDateTime = findViewById(R.id.tvDetailDateTime);
        tvLocation = findViewById(R.id.tvDetailLocation);
        tvDistance = findViewById(R.id.tvDetailDistance);
        tvAvgPace = findViewById(R.id.tvDetailAvgPace);
        tvDuration = findViewById(R.id.tvDetailDuration);
        tvCalories = findViewById(R.id.tvDetailCalories);
        tvSteps = findViewById(R.id.tvDetailSteps);
        tvElevation = findViewById(R.id.tvDetailElevation);
        tvMaxElev = findViewById(R.id.tvDetailMaxElev);

        tvPaceAvg = findViewById(R.id.tvDetailPaceAvg);
        tvPaceTotal = findViewById(R.id.tvDetailPaceTotal);
        tvPaceFast = findViewById(R.id.tvDetailPaceFast);
        tvCadenceAvg = findViewById(R.id.tvDetailCadenceAvg);
        tvCadenceMax = findViewById(R.id.tvDetailCadenceMax);
        tvElevGain = findViewById(R.id.tvDetailElevGain);
        tvElevMax = findViewById(R.id.tvDetailElevMax);
        
        chartPace = findViewById(R.id.chartPace);
        chartCadence = findViewById(R.id.chartKadens);
        chartElevation = findViewById(R.id.chartElevasi);
    }

    private void loadRunData(int id) {
        new Thread(() -> {
            RunRecord run = AppDatabase.getInstance(this).runDao().getRunById(id);
            if (run != null) {
                runOnUiThread(() -> populateData(run));
            }
        }).start();
    }

    private void populateData(RunRecord run) {
        tvDistanceTop.setText(String.format(Locale.getDefault(), "%.2f km", run.getDistance()));
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f", run.getDistance()));

        long time = run.getDuration();
        tvDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", time / 60, time % 60));

        double pace = run.getPace();
        tvAvgPace.setText(String.format(Locale.getDefault(), "%d:%02d", (int)pace, (int)((pace - (int)pace) * 60)));

        if (run.getDate() != null && run.getStartTime() != null && run.getEndTime() != null) {
            tvDateTime.setText(String.format("%s · %s - %s", run.getDate(), run.getStartTime(), run.getEndTime()));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMM · HH:mm", new Locale("id", "ID"));
            tvDateTime.setText(sdf.format(new Date(run.getTimestamp())));
        }
        
        String loc = run.getLocationName();
        if (loc != null) {
            if (loc.contains(" - ")) {
                loc = loc.replace(" - ", "\n");
            }
            tvLocation.setText(loc);
            tvLocation.setVisibility(View.VISIBLE);
        } else {
            tvLocation.setVisibility(View.GONE);
        }
        
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(run.getTimestamp());
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String title = "Berlari";
        if (hour >= 5 && hour < 11) title = "Berlari Pagi";
        else if (hour >= 11 && hour < 15) title = "Berlari Siang";
        else if (hour >= 15 && hour < 19) title = "Berlari Sore";
        else title = "Berlari Malam";
        tvTitle.setText(title);

        tvCalories.setText(String.valueOf(run.getCalories()));
        tvSteps.setText(String.valueOf((int)(run.getDistance() * 1350))); 
        tvElevation.setText(String.format(Locale.getDefault(), "%.0f m", run.getElevationGain()));
        tvMaxElev.setText(String.format(Locale.getDefault(), "%.0f m", run.getElevationGain() + 3));

        drawRoute(run.getPathJson());
        setupDetailedCharts(run);
        setupSplits(run);
    }

    private void setupDetailedCharts(RunRecord run) {
        if (run.getSplitsJson() == null || run.getSplitsJson().equals("null")) return;
        Gson gson = new Gson();
        try {
            double[] paceSplits = gson.fromJson(run.getSplitsJson(), double[].class);
            double[] elevSplits = gson.fromJson(run.getElevationSplitsJson(), double[].class);
            int[] cadSplits = gson.fromJson(run.getCadenceSplitsJson(), int[].class);

            if (paceSplits != null && paceSplits.length > 0) {
                float[] paceData = new float[paceSplits.length];
                String[] paceInfo = new String[paceSplits.length];
                long accumulatedTime = 0;
                double totalPaceSecs = 0;
                double minPaceSecs = 999999;
                SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                
                for (int i = 0; i < paceSplits.length; i++) {
                    double p = paceSplits[i] / 60.0; // split duration in mins
                    paceData[i] = (float) p;
                    totalPaceSecs += paceSplits[i];
                    if (paceSplits[i] < minPaceSecs) minPaceSecs = paceSplits[i];
                    
                    accumulatedTime += (long)paceSplits[i];
                    String timeAtKm = timeSdf.format(new Date(run.getTimestamp() + accumulatedTime * 1000));
                    
                    int mins = (int) p;
                    int secs = (int) ((p - mins) * 60);
                    paceInfo[i] = String.format(Locale.getDefault(), "%d:%02d /km @ %s", mins, secs, timeAtKm);
                }
                chartPace.setDetailedData(paceData, paceInfo);

                // Update Pace Summary
                double avgPace = (totalPaceSecs / paceSplits.length) / 60.0;
                tvPaceAvg.setText(String.format(Locale.getDefault(), "%d:%02d /km", (int)avgPace, (int)((avgPace - (int)avgPace) * 60)));
                
                double totPace = run.getPace();
                tvPaceTotal.setText(String.format(Locale.getDefault(), "%d:%02d /km", (int)totPace, (int)((totPace - (int)totPace) * 60)));
                
                double fastPace = minPaceSecs / 60.0;
                tvPaceFast.setText(String.format(Locale.getDefault(), "%d:%02d /km", (int)fastPace, (int)((fastPace - (int)fastPace) * 60)));
            }

            if (elevSplits != null && elevSplits.length > 0) {
                float[] elevData = new float[elevSplits.length];
                String[] elevInfo = new String[elevSplits.length];
                double maxElev = -9999;
                for (int i = 0; i < elevSplits.length; i++) {
                    elevData[i] = (float) elevSplits[i];
                    elevInfo[i] = String.format(Locale.getDefault(), "KM %d: +%.0fm", i+1, elevSplits[i]);
                    if (elevSplits[i] > maxElev) maxElev = elevSplits[i];
                }
                chartElevation.setDetailedData(elevData, elevInfo);
                tvElevGain.setText(String.format(Locale.getDefault(), "%.0f m", run.getElevationGain()));
                tvElevMax.setText(String.format(Locale.getDefault(), "%.0f m", Math.max(maxElev, run.getElevationGain())));
            }

            if (cadSplits != null && cadSplits.length > 0) {
                float[] cadData = new float[cadSplits.length];
                String[] cadInfo = new String[cadSplits.length];
                long totalCad = 0;
                int maxCad = 0;
                for (int i = 0; i < cadSplits.length; i++) {
                    cadData[i] = (float) cadSplits[i];
                    cadInfo[i] = String.format(Locale.getDefault(), "KM %d: %d spm", i+1, cadSplits[i]);
                    totalCad += cadSplits[i];
                    if (cadSplits[i] > maxCad) maxCad = cadSplits[i];
                }
                chartCadence.setDetailedData(cadData, cadInfo);
                tvCadenceAvg.setText(String.format(Locale.getDefault(), "%d spm", totalCad / cadSplits.length));
                tvCadenceMax.setText(String.format(Locale.getDefault(), "%d spm", maxCad));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void drawRoute(String pathJson) {
        if (pathJson == null || pathJson.isEmpty()) return;

        try {
            List<GeoPoint> path = new Gson().fromJson(pathJson, new TypeToken<List<GeoPoint>>(){}.getType());
            if (path != null && !path.isEmpty()) {
                Polyline polyline = new Polyline();
                polyline.setPoints(path);
                polyline.getOutlinePaint().setColor(Color.parseColor("#CDFF00"));
                polyline.getOutlinePaint().setStrokeWidth(10f);
                map.getOverlayManager().add(polyline);

                // Zoom to fit
                map.postDelayed(() -> {
                    BoundingBox box = BoundingBox.fromGeoPoints(path);
                    map.zoomToBoundingBox(box, true, 100);
                }, 500);
                
                map.invalidate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupSplits(RunRecord run) {
        llSplitContainer.removeAllViews();
        
        Gson gson = new Gson();
        double[] paceSplits = gson.fromJson(run.getSplitsJson(), double[].class);
        double[] elevSplits = gson.fromJson(run.getElevationSplitsJson(), double[].class);

        if (paceSplits == null || paceSplits.length == 0) return;

        double maxPace = 0, minPace = 999;
        for (double p : paceSplits) {
            if (p > maxPace) maxPace = p;
            if (p < minPace) minPace = p;
        }

        for (int i = 0; i < paceSplits.length; i++) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_split_row, llSplitContainer, false);
            TextView tvKm = row.findViewById(R.id.tvSplitKm);
            TextView tvPace = row.findViewById(R.id.tvSplitPace);
            TextView tvElev = row.findViewById(R.id.tvSplitElev);
            ProgressBar progress = row.findViewById(R.id.progressSplit);

            tvKm.setText(String.valueOf(i + 1));
            
            double pSecs = paceSplits[i];
            int mins = (int) (pSecs / 60);
            int secs = (int) (pSecs % 60);
            tvPace.setText(String.format(Locale.getDefault(), "%d:%02d", mins, secs));
            
            double e = (elevSplits != null && i < elevSplits.length) ? elevSplits[i] : 0;
            tvElev.setText(String.format(Locale.getDefault(), "%.0f", e));
            
            int progressVal;
            if (maxPace == minPace) progressVal = 80;
            else progressVal = (int) (40 + (maxPace - pSecs) / (maxPace - minPace) * 60);
            progress.setProgress(progressVal);
            
            llSplitContainer.addView(row);
        }
    }

    private void showRingkasan() {
        btnRingkasan.setBackgroundResource(R.drawable.btn_lime);
        ((TextView)findViewById(R.id.tvTabRingkasan)).setTextColor(ContextCompat.getColor(this, R.color.bg));
        btnSplit.setBackground(null);
        ((TextView)findViewById(R.id.tvTabSplit)).setTextColor(ContextCompat.getColor(this, R.color.muted_fg));
        layoutRingkasan.setVisibility(View.VISIBLE);
        layoutSplit.setVisibility(View.GONE);
    }

    private void showSplit() {
        btnSplit.setBackgroundResource(R.drawable.btn_lime);
        ((TextView)findViewById(R.id.tvTabSplit)).setTextColor(ContextCompat.getColor(this, R.color.bg));
        btnRingkasan.setBackground(null);
        ((TextView)findViewById(R.id.tvTabRingkasan)).setTextColor(ContextCompat.getColor(this, R.color.muted_fg));
        layoutRingkasan.setVisibility(View.GONE);
        layoutSplit.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}
