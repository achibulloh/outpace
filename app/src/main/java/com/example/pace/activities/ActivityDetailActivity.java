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
import com.example.pace.model.User;
import com.example.pace.utils.GeminiAssistant;
import com.example.pace.utils.LocaleHelper;
import com.example.pace.views.LineChartView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
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
    private TextView tvAIInsights;
    private ProgressBar pbAI;
    
    private LineChartView chartPace, chartCadence, chartElevation;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // RE-CONFIRM User-Agent before layout inflation to prevent 403 Access Blocked
        String userAgent = "OutpaceTracker/1.0 (" + getPackageName() + "; contact@outpace.app)";
        org.osmdroid.config.Configuration.getInstance().setUserAgentValue(userAgent);

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
        
        // Use the same server as RunFragment for consistency and to avoid 403 errors
        map.setTileSource(new org.osmdroid.tileprovider.tilesource.XYTileSource("OSMHot", 0, 19, 256, ".png", 
                new String[] {
                    "https://a.tile.openstreetmap.fr/hot/",
                    "https://b.tile.openstreetmap.fr/hot/",
                    "https://c.tile.openstreetmap.fr/hot/" 
                }));
                
        map.setMultiTouchControls(true);
        map.getController().setZoom(16.0);
        map.setBackgroundColor(Color.parseColor("#121212"));

        // Apply Dark Mode Filter to the map tiles using negative color matrix
        float[] negative = {
                -1.0f, 0, 0, 0, 255, // red
                0, -1.0f, 0, 0, 255, // green
                0, 0, -1.0f, 0, 255, // blue
                0, 0, 0, 1.0f, 0     // alpha
        };
        map.getOverlayManager().getTilesOverlay().setColorFilter(new android.graphics.ColorMatrixColorFilter(negative));
        
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
        tvAIInsights = findViewById(R.id.tvAIInsights);
        pbAI = findViewById(R.id.pbAI);
        
        chartPace = findViewById(R.id.chartPace);
        chartCadence = findViewById(R.id.chartKadens);
        chartElevation = findViewById(R.id.chartElevasi);
    }

    private void loadRunData(int id) {
        new Thread(() -> {
            RunRecord run = AppDatabase.getInstance(this).runDao().getRunById(id);
            if (run != null) {
                // PARSE HEAVY DATA IN BACKGROUND
                Gson gson = new Gson();
                List<GeoPoint> path = new ArrayList<>();
                try {
                    path = gson.fromJson(run.getPathJson(), new TypeToken<List<GeoPoint>>(){}.getType());
                } catch (Exception ignored) {}
                
                double[] paceSplits = null;
                double[] elevSplits = null;
                int[] cadSplits = null;
                try {
                    paceSplits = gson.fromJson(run.getSplitsJson(), double[].class);
                    elevSplits = gson.fromJson(run.getElevationSplitsJson(), double[].class);
                    cadSplits = gson.fromJson(run.getCadenceSplitsJson(), int[].class);
                } catch (Exception ignored) {}

                final List<GeoPoint> finalPath = path;
                final double[] finalPaceSplits = paceSplits;
                final double[] finalElevSplits = elevSplits;
                final int[] finalCadSplits = cadSplits;

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    populateData(run, finalPath, finalPaceSplits, finalElevSplits, finalCadSplits);
                });
            }
        }).start();
    }

    private void populateData(RunRecord run, List<GeoPoint> path, double[] paceSplits, double[] elevSplits, int[] cadSplits) {
        tvDistanceTop.setText(getString(R.string.distance_km_val, run.getDistance()));
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f", run.getDistance()));

        long time = run.getDuration();
        tvDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", time / 60, time % 60));

        double pace = run.getPace();
        tvAvgPace.setText(String.format(Locale.getDefault(), "%d:%02d", (int)pace, (int)((pace - (int)pace) * 60)));

        if (run.getDate() != null && run.getStartTime() != null && run.getEndTime() != null) {
            tvDateTime.setText(String.format("%s · %s - %s", run.getDate(), run.getStartTime(), run.getEndTime()));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMM · HH:mm", Locale.getDefault());
            tvDateTime.setText(sdf.format(new Date(run.getTimestamp())));
        }
        
        String loc = run.getLocationName();
        if (loc != null) {
            if (loc.contains(" - ")) { loc = loc.replace(" - ", "\n"); }
            tvLocation.setText(loc);
            tvLocation.setVisibility(View.VISIBLE);
        } else {
            tvLocation.setVisibility(View.GONE);
        }
        
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(run.getTimestamp());
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String title;
        if (hour >= 5 && hour < 11) title = getString(R.string.morning_run);
        else if (hour >= 11 && hour < 15) title = getString(R.string.afternoon_run);
        else if (hour >= 15 && hour < 19) title = getString(R.string.evening_run);
        else title = getString(R.string.night_run);
        tvTitle.setText(title);

        tvCalories.setText(String.valueOf(run.getCalories()));
        int steps = (int)(run.getDistance() * 1350);
        tvSteps.setText(steps > 1000 ? String.format(Locale.getDefault(), "%.1fk", steps/1000.0) : String.valueOf(steps));
        tvElevation.setText(String.format(Locale.getDefault(), "%.0fm", run.getElevationGain()));
        tvMaxElev.setText(String.format(Locale.getDefault(), "%.0fm", run.getElevationGain() + 1));

        drawRoute(path);
        setupCharts(run, paceSplits, elevSplits, cadSplits);
        setupSplits(paceSplits, elevSplits);
        fetchAIInsights(run);
    }

    private void drawRoute(List<GeoPoint> path) {
        if (path == null || path.isEmpty()) return;

        Polyline polyline = new Polyline();
        polyline.setPoints(path);
        // Use a color that stands out in inverted dark mode
        polyline.getOutlinePaint().setColor(Color.parseColor("#C8F43A"));
        polyline.getOutlinePaint().setStrokeWidth(12f);
        
        map.getOverlayManager().add(polyline);

        // Add Start Marker (White)
        Marker startMarker = new Marker(map);
        startMarker.setPosition(path.get(0));
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.marker_start));
        startMarker.setTitle("Start");
        // Ensure marker is not inverted by the map filter if possible, 
        // but since it's an overlay it might be. Let's keep it simple first.
        map.getOverlays().add(startMarker);

        // Add Finish Marker (Lime)
        Marker endMarker = new Marker(map);
        endMarker.setPosition(path.get(path.size() - 1));
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        endMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.marker_finish));
        endMarker.setTitle("Finish");
        map.getOverlays().add(endMarker);

        map.addOnFirstLayoutListener((v, left, top, right, bottom) -> {
            try {
                BoundingBox box = BoundingBox.fromGeoPoints(path);
                map.zoomToBoundingBox(box, false, 150);
            } catch (Exception ignored) {}
        });
        
        // If map is already laid out, zoom immediately
        if (map.isLayoutOccurred()) {
            try {
                BoundingBox box = BoundingBox.fromGeoPoints(path);
                map.zoomToBoundingBox(box, false, 150);
            } catch (Exception ignored) {}
        }

        map.invalidate();
    }

    private void setupCharts(RunRecord run, double[] paceSplits, double[] elevSplits, int[] cadSplits) {
        if (paceSplits == null || paceSplits.length == 0) {
            if (run.getDistance() > 0.001) {
                double p = run.getPace();
                chartPace.setDetailedData(new float[]{(float) p}, new String[]{String.format(Locale.getDefault(), "%d:%02d", (int)p, (int)((p-(int)p)*60))});
                chartElevation.setDetailedData(new float[]{(float)run.getElevationGain()}, new String[]{String.format(Locale.getDefault(), "%.0fm", run.getElevationGain())});
                chartCadence.setDetailedData(new float[]{160f}, new String[]{"160 spm"});
                tvPaceAvg.setText(String.format(Locale.getDefault(), "%d:%02d", (int)p, (int)((p-(int)p)*60)));
                tvPaceTotal.setText(String.format(Locale.getDefault(), "%d:%02d", (int)p, (int)((p-(int)p)*60)));
                tvPaceFast.setText(String.format(Locale.getDefault(), "%d:%02d", (int)p, (int)((p-(int)p)*60)));
                tvCadenceAvg.setText("160 spm");
                tvCadenceMax.setText("160 spm");
                tvElevGain.setText(String.format(Locale.getDefault(), "%.0fm", run.getElevationGain()));
                tvElevMax.setText(String.format(Locale.getDefault(), "%.0fm", run.getElevationGain()));
            }
            return;
        }

        // Charts data processing already happened or uses passed splits
        float[] paceData = new float[paceSplits.length];
        String[] paceInfo = new String[paceSplits.length];
        double totalPaceSecs = 0, minPaceSecs = 999999;
        
        for (int i = 0; i < paceSplits.length; i++) {
            double p = paceSplits[i] / 60.0;
            paceData[i] = (float) p;
            totalPaceSecs += paceSplits[i];
            if (paceSplits[i] < minPaceSecs) minPaceSecs = paceSplits[i];
            int mins = (int) p;
            int secs = (int) ((p - mins) * 60);
            paceInfo[i] = String.format(Locale.getDefault(), "KM %d: %d:%02d", i + 1, mins, secs);
        }
        chartPace.setDetailedData(paceData, paceInfo);

        double avgPace = (totalPaceSecs / paceSplits.length) / 60.0;
        tvPaceAvg.setText(String.format(Locale.getDefault(), "%d:%02d", (int)avgPace, (int)((avgPace - (int)avgPace) * 60)));
        tvPaceTotal.setText(String.format(Locale.getDefault(), "%d:%02d", (int)run.getPace(), (int)((run.getPace() - (int)run.getPace()) * 60)));
        double fastPace = minPaceSecs / 60.0;
        tvPaceFast.setText(String.format(Locale.getDefault(), "%d:%02d", (int)fastPace, (int)((fastPace - (int)fastPace) * 60)));

        if (elevSplits != null && elevSplits.length > 0) {
            float[] elevData = new float[elevSplits.length];
            String[] elevInfo = new String[elevSplits.length];
            double maxElev = -9999;
            for (int i = 0; i < elevSplits.length; i++) {
                elevData[i] = (float) elevSplits[i];
                elevInfo[i] = String.format(Locale.getDefault(), "KM %d: +%.0fm", i + 1, elevSplits[i]);
                if (elevSplits[i] > maxElev) maxElev = elevSplits[i];
            }
            chartElevation.setDetailedData(elevData, elevInfo);
            tvElevGain.setText(String.format(Locale.getDefault(), "%.0fm", run.getElevationGain()));
            tvElevMax.setText(String.format(Locale.getDefault(), "%.0fm", Math.max(maxElev, run.getElevationGain())));
        }

        if (cadSplits != null && cadSplits.length > 0) {
            float[] cadData = new float[cadSplits.length];
            String[] cadInfo = new String[cadSplits.length];
            long totalCad = 0; int maxCad = 0;
            for (int i = 0; i < cadSplits.length; i++) {
                cadData[i] = (float) cadSplits[i];
                cadInfo[i] = String.format(Locale.getDefault(), "KM %d: %d spm", i + 1, cadSplits[i]);
                totalCad += cadSplits[i];
                if (cadSplits[i] > maxCad) maxCad = cadSplits[i];
            }
            chartCadence.setDetailedData(cadData, cadInfo);
            tvCadenceAvg.setText(String.format(Locale.getDefault(), "%d spm", totalCad / cadSplits.length));
            tvCadenceMax.setText(String.format(Locale.getDefault(), "%d spm", maxCad));
        }
    }

    private void setupSplits(double[] paceSplits, double[] elevSplits) {
        llSplitContainer.removeAllViews();
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
            int mins = (int) (paceSplits[i] / 60);
            int secs = (int) (paceSplits[i] % 60);
            tvPace.setText(String.format(Locale.getDefault(), "%d:%02d", mins, secs));
            double e = (elevSplits != null && i < elevSplits.length) ? elevSplits[i] : 0;
            tvElev.setText(String.format(Locale.getDefault(), "%.0f", e));
            
            int progressVal = (maxPace == minPace) ? 80 : (int) (40 + (maxPace - paceSplits[i]) / (maxPace - minPace) * 60);
            progress.setProgress(progressVal);
            llSplitContainer.addView(row);
        }
    }

    private void fetchAIInsights(RunRecord run) {
        if (run.getAiInsights() != null && !run.getAiInsights().isEmpty()) {
            tvAIInsights.setText(run.getAiInsights());
            pbAI.setVisibility(View.GONE);
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        pbAI.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance().collection("users").document(uid).collection("runs").document(run.getFirebaseId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (documentSnapshot.exists() && documentSnapshot.contains("aiInsights")) {
                        String cloudInsights = documentSnapshot.getString("aiInsights");
                        if (cloudInsights != null && !cloudInsights.isEmpty()) {
                            runOnUiThread(() -> {
                                pbAI.setVisibility(View.GONE);
                                tvAIInsights.setText(cloudInsights);
                                run.setAiInsights(cloudInsights);
                                new Thread(() -> { try { AppDatabase.getInstance(ActivityDetailActivity.this).runDao().insert(run); } catch (Exception ignored) {} }).start();
                            });
                            return;
                        }
                    }
                    generateNewAIInsights(run, uid);
                })
                .addOnFailureListener(e -> { if (!isFinishing() && !isDestroyed()) generateNewAIInsights(run, uid); });
    }

    private void generateNewAIInsights(RunRecord run, String uid) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user == null) { pbAI.setVisibility(View.GONE); return; }
                    GeminiAssistant ai = GeminiAssistant.getInstance();
                    String lang = LocaleHelper.getLanguage(this);
                    ai.generateRunInsights(this, user, run, lang, new GeminiAssistant.AIResponseCallback() {
                        @Override
                        public void onSuccess(String response) {
                            runOnUiThread(() -> {
                                pbAI.setVisibility(View.GONE);
                                if (response.contains("{") || response.contains("Unexpected")) {
                                    tvAIInsights.setText("Coach is temporarily busy. Please try again.");
                                    return;
                                }
                                tvAIInsights.setText(response);
                                run.setAiInsights(response);
                                new Thread(() -> AppDatabase.getInstance(ActivityDetailActivity.this).runDao().insert(run)).start();
                                FirebaseFirestore.getInstance().collection("users").document(uid).collection("runs").document(run.getFirebaseId()).update("aiInsights", response);
                            });
                        }
                        @Override
                        public void onError(String errorMsg) { runOnUiThread(() -> { pbAI.setVisibility(View.GONE); tvAIInsights.setText(errorMsg); }); }
                    });
                });
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
    public void onResume() { super.onResume(); if (map != null) map.onResume(); }
    @Override
    public void onPause() { super.onPause(); if (map != null) map.onPause(); }
}
