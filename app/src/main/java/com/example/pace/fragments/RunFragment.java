package com.example.pace.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.pace.R;
import com.example.pace.activities.MainActivity;
import com.example.pace.database.AppDatabase;
import com.example.pace.model.RunRecord;
import com.example.pace.services.TrackingService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.gson.Gson;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.pace.views.BarChartView;

public class RunFragment extends Fragment implements android.hardware.SensorEventListener {

    private MapView map = null;
    private Polyline polyline;
    private List<GeoPoint> pathPoints = new ArrayList<>();
    private Marker userMarker;
    private FusedLocationProviderClient fusedLocationClient;
    private android.hardware.SensorManager sensorManager;
    private android.hardware.Sensor rotationSensor;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int ACTIVITY_RECOGNITION_REQUEST_CODE = 2;

    private Button btnStart, btnPause, btnStop;
    private Button btnPauseOverlay, btnStopOverlay;
    private ImageButton btnToggleStats, btnMinimize;
    private LinearLayout btnBack, layoutMapLoading, panelStatsBottom, layoutStatsOverlay;
    private TextView tvDuration, tvDistance, tvPace, tvCalories, tvSteps;
    private View viewGpsIndicator;
    private TextView tvGpsStatus;

    // Overlay Views
    private TextView tvOverlayGpsStatus, tvOverlayDuration, tvOverlayAvgPace, tvOverlayDistance, tvOverlayCalories, tvOverlaySteps, tvOverlayElevation;
    private BarChartView splitBarChart;

    private boolean isTracking = false;
    private boolean isAutoPaused = false;
    private long lastTime = 0;
    private double lastDistance = 0;
    private double lastElevation = 0;
    private double[] currentSplits, currentElevSplits;
    private int[] currentCadSplits;
    private int userWeight = 70;

    private boolean isMapReady = false;
    private boolean isLocationReady = false;
    private boolean isStatsExpanded = false;

    private LocationCallback statusLocationCallback;
    private GestureDetector gestureDetector;

    private BroadcastReceiver trackingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TrackingService.TRACKING_UPDATE.equals(intent.getAction())) {
                updateUIFromService(intent);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context ctx = requireActivity().getApplicationContext();
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        Configuration.getInstance().load(ctx, requireActivity().getSharedPreferences("osmdroid", Context.MODE_PRIVATE));

        View view = inflater.inflate(R.layout.fragment_run, container, false);
        
        sensorManager = (android.hardware.SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ORIENTATION);

        loadUserWeight();
        initUI(view);
        setupMap(view);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        enableInitialLocation();
        checkActivityRecognitionPermission();

        return view;
    }

    private void checkActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, ACTIVITY_RECOGNITION_REQUEST_CODE);
            }
        }
    }

    private void loadUserWeight() {
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        userWeight = prefs.getInt("weight", 70);
    }

    private void initUI(View view) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setNavigationVisibility(false);
        }

        btnStart = view.findViewById(R.id.btnStart);
        btnPause = view.findViewById(R.id.btnPause);
        btnStop = view.findViewById(R.id.btnStop);
        btnBack = view.findViewById(R.id.btnBack);
        btnToggleStats = view.findViewById(R.id.btnToggleStats);
        btnMinimize = view.findViewById(R.id.btnMinimize);
        layoutMapLoading = view.findViewById(R.id.layoutMapLoading);
        panelStatsBottom = view.findViewById(R.id.panelStatsBottom);
        layoutStatsOverlay = view.findViewById(R.id.layoutStatsOverlay);
        
        btnPauseOverlay = view.findViewById(R.id.btnPauseOverlay);
        btnStopOverlay = view.findViewById(R.id.btnStopOverlay);

        viewGpsIndicator = view.findViewById(R.id.viewGpsIndicator);
        tvGpsStatus = view.findViewById(R.id.tvGpsStatus);

        tvDuration = view.findViewById(R.id.tvDuration);
        tvDistance = view.findViewById(R.id.tvDistance);
        tvPace = view.findViewById(R.id.tvPace);
        tvCalories = view.findViewById(R.id.tvCalories);
        tvSteps = view.findViewById(R.id.tvSteps);

        tvOverlayGpsStatus = view.findViewById(R.id.tvOverlayGpsStatus);
        tvOverlayDuration = view.findViewById(R.id.tvOverlayDuration);
        tvOverlayAvgPace = view.findViewById(R.id.tvOverlayAvgPace);
        tvOverlayDistance = view.findViewById(R.id.tvOverlayDistance);
        tvOverlayCalories = view.findViewById(R.id.tvOverlayCalories);
        tvOverlaySteps = view.findViewById(R.id.tvOverlaySteps);
        tvOverlayElevation = view.findViewById(R.id.tvOverlayElevation);
        splitBarChart = view.findViewById(R.id.splitBarChart);
        if (splitBarChart != null) splitBarChart.setShowLabels(false);

        btnPauseOverlay.setVisibility(View.GONE);
        btnStopOverlay.setVisibility(View.GONE);

        btnStart.setOnClickListener(v -> {
            sendAction(TrackingService.ACTION_START);
            if (!isStatsExpanded) toggleStatsOverlay();
        });
        
        View.OnClickListener pauseAction = v -> {
            if (isTracking && !isAutoPaused) sendAction(TrackingService.ACTION_PAUSE);
            else sendAction(TrackingService.ACTION_START);
        };
        btnPause.setOnClickListener(pauseAction);
        btnPauseOverlay.setOnClickListener(pauseAction);

        View.OnClickListener stopAction = v -> {
            saveRunBeforeStop();
            sendAction(TrackingService.ACTION_STOP);
        };
        btnStop.setOnClickListener(stopAction);
        btnStopOverlay.setOnClickListener(stopAction);

        btnBack.setOnClickListener(v -> {
            if (isTracking) {
                Toast.makeText(getContext(), "Berhentikan lari terlebih dahulu", Toast.LENGTH_SHORT).show();
            } else {
                goBack();
            }
        });

        btnToggleStats.setOnClickListener(v -> toggleStatsOverlay());
        if (btnMinimize != null) {
            btnMinimize.setOnClickListener(v -> toggleStatsOverlay());
        }
        
        panelStatsBottom.setOnClickListener(v -> toggleStatsOverlay());
        
        setupSwipeGestures();
    }

    private void setupSwipeGestures() {
        gestureDetector = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 != null && e2 != null) {
                    if (e1.getY() - e2.getY() > 100) { // Swipe Up
                        if (!isStatsExpanded) toggleStatsOverlay();
                        return true;
                    } else if (e2.getY() - e1.getY() > 100) { // Swipe Down
                        if (isStatsExpanded) toggleStatsOverlay();
                        return true;
                    }
                }
                return false;
            }
        });

        View.OnTouchListener touchListener = (v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false; 
        };

        panelStatsBottom.setOnTouchListener(touchListener);
        layoutStatsOverlay.setOnTouchListener(touchListener);
    }

    private void toggleStatsOverlay() {
        isStatsExpanded = !isStatsExpanded;
        if (isStatsExpanded) {
            layoutStatsOverlay.setVisibility(View.VISIBLE);
            panelStatsBottom.setVisibility(View.GONE);
            btnToggleStats.setVisibility(View.GONE);
        } else {
            layoutStatsOverlay.setVisibility(View.GONE);
            panelStatsBottom.setVisibility(View.VISIBLE);
            btnToggleStats.setVisibility(View.VISIBLE);
        }
    }

    private void setupMap(View view) {
        map = view.findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setBackgroundColor(Color.parseColor("#121212"));
        map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
        map.getController().setZoom(18.0);
        
        polyline = new Polyline();
        polyline.getOutlinePaint().setColor(Color.parseColor("#CDFF00"));
        polyline.getOutlinePaint().setStrokeWidth(12f);
        map.getOverlayManager().add(polyline);

        userMarker = new Marker(map);
        userMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_person_run));
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        map.getOverlays().add(userMarker);

        RotationGestureOverlay rotationGestureOverlay = new RotationGestureOverlay(map);
        rotationGestureOverlay.setEnabled(true);
        map.getOverlays().add(rotationGestureOverlay);

        map.addOnFirstLayoutListener((v, left, top, right, bottom) -> {
            isMapReady = true;
            checkLoadingFinished();
        });
    }

    private void enableInitialLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null && !isTracking) {
                        GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                        if (userMarker != null) {
                            userMarker.setPosition(point);
                            map.getController().setCenter(point);
                        }
                        updateGpsStatus(location.getAccuracy());
                    }
                    isLocationReady = true;
                    checkLoadingFinished();
                })
                .addOnFailureListener(e -> {
                    isLocationReady = true;
                    checkLoadingFinished();
                });
    }

    private void startStatusLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build();
        statusLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                android.location.Location loc = locationResult.getLastLocation();
                if (loc != null) {
                    updateGpsStatus(loc.getAccuracy());
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(request, statusLocationCallback, android.os.Looper.getMainLooper());
    }

    private void stopStatusLocationUpdates() {
        if (fusedLocationClient != null && statusLocationCallback != null) {
            fusedLocationClient.removeLocationUpdates(statusLocationCallback);
        }
    }

    private void checkLoadingFinished() {
        if (isMapReady && isLocationReady && layoutMapLoading != null) {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (layoutMapLoading != null) {
                    layoutMapLoading.animate().alpha(0).setDuration(500).withEndAction(() -> 
                        layoutMapLoading.setVisibility(View.GONE)).start();
                }
            }, 500);
        }
    }

    private void updateGpsStatus(float accuracy) {
        String statusText = (accuracy > 0 && accuracy < 20) ? "GPS Bagus" : "GPS Lemah";
        int resId = (accuracy > 0 && accuracy < 20) ? R.drawable.progress_fill : R.drawable.dot_red;
        int color = (accuracy > 0 && accuracy < 20) ? Color.parseColor("#C8F43A") : Color.RED;

        if (tvGpsStatus != null) {
            tvGpsStatus.setText(statusText);
            viewGpsIndicator.setBackgroundResource(resId);
        }
        if (tvOverlayGpsStatus != null) {
            String text = isAutoPaused ? "AUTO-PAUSED" : "Sinyal GPS " + statusText.split(" ")[1].toLowerCase();
            tvOverlayGpsStatus.setText(text);
            tvOverlayGpsStatus.setTextColor(isAutoPaused ? Color.YELLOW : color);
        }
    }

    private void updateUIFromService(Intent intent) {
        isTracking = intent.getBooleanExtra("isTracking", false);
        isAutoPaused = intent.getBooleanExtra("isAutoPaused", false);
        long time = intent.getLongExtra("time", 0);
        double distance = intent.getDoubleExtra("distance", 0.0);
        double elevation = intent.getDoubleExtra("elevation", 0.0);
        int steps = intent.getIntExtra("steps", 0);
        double lat = intent.getDoubleExtra("lat", 0);
        double lng = intent.getDoubleExtra("lng", 0);
        float accuracy = intent.getFloatExtra("accuracy", 0);
        double[] splits = intent.getDoubleArrayExtra("splits");
        currentSplits = splits;
        currentElevSplits = intent.getDoubleArrayExtra("elevSplits");
        currentCadSplits = intent.getIntArrayExtra("cadSplits");

        lastTime = time;
        lastDistance = distance;
        lastElevation = elevation;
        updateGpsStatus(accuracy);

        String durationStr = String.format(Locale.getDefault(), "%02d:%02d", time / 60, time % 60);
        String fullDurationStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", time / 3600, (time % 3600) / 60, time % 60);
        
        tvDuration.setText(durationStr);
        tvOverlayDuration.setText(fullDurationStr);
        
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f", distance));
        tvOverlayDistance.setText(String.format(Locale.getDefault(), "%.1f", distance));
        
        String caloriesStr = String.valueOf((int)(distance * userWeight * 1.036));
        if (tvCalories != null) tvCalories.setText(caloriesStr);
        if (tvOverlayCalories != null) tvOverlayCalories.setText(caloriesStr);

        String stepsStr = (steps > 0) ? String.valueOf(steps) : "--";
        if (tvSteps != null) tvSteps.setText(stepsStr);
        if (tvOverlaySteps != null) tvOverlaySteps.setText(stepsStr);
        
        if (tvOverlayElevation != null) tvOverlayElevation.setText(String.format(Locale.getDefault(), "%.0fm Up", elevation));

        if (distance > 0.001) {
            double paceDecimal = (time / 60.0) / distance;
            int paceMins = (int) paceDecimal;
            int paceSecs = (int) ((paceDecimal - paceMins) * 60);
            String paceStr = String.format(Locale.getDefault(), "%d:%02d", paceMins, paceSecs);
            tvPace.setText(paceStr);
            tvOverlayAvgPace.setText(paceStr);
        }

        updateButtons(isTracking && !isAutoPaused);

        if (lat != 0 && lng != 0 && !isAutoPaused) {
            GeoPoint point = new GeoPoint(lat, lng);
            pathPoints.add(point);
            polyline.setPoints(pathPoints);
            userMarker.setPosition(point);
            map.getController().animateTo(point);
            map.invalidate();
        }

        if (splits != null && splits.length > 0 && splitBarChart != null) {
            float[] splitPaces = new float[splits.length];
            for(int i=0; i<splits.length; i++) splitPaces[i] = (float) (splits[i] / 60.0);
            splitBarChart.setData(splitPaces);
        }
    }

    private void updateButtons(boolean tracking) {
        if (isTracking) {
            btnStart.setVisibility(View.GONE);
            btnPause.setVisibility(View.VISIBLE);
            btnPauseOverlay.setVisibility(View.VISIBLE);
            
            if (isAutoPaused) {
                btnPause.setText("▶ LANJUT");
                btnPauseOverlay.setText("▶ LANJUT");
                btnStop.setVisibility(View.GONE);
                btnStopOverlay.setVisibility(View.GONE);
            } else {
                btnPause.setText("⏸ JEDA");
                btnPauseOverlay.setText("⏸ JEDA");
                btnStop.setVisibility(View.VISIBLE);
                btnStopOverlay.setVisibility(View.VISIBLE);
            }
        } else if (lastTime > 0) {
            btnStart.setVisibility(View.GONE);
            btnPause.setVisibility(View.VISIBLE);
            btnPauseOverlay.setVisibility(View.VISIBLE);
            btnPause.setText("▶ LANJUT");
            btnPauseOverlay.setText("▶ LANJUT");
            btnStop.setVisibility(View.GONE); 
            btnStopOverlay.setVisibility(View.GONE);
        } else {
            btnStart.setVisibility(View.VISIBLE);
            btnPause.setVisibility(View.GONE);
            btnPauseOverlay.setVisibility(View.GONE);
            btnStop.setVisibility(View.GONE);
            btnStopOverlay.setVisibility(View.GONE);
        }
    }

    private void saveRunBeforeStop() {
        if (lastDistance < 0.005) { // Minimum 5 meters to save
            goBack();
            return;
        }

        long timestamp = System.currentTimeMillis();
        double distance = lastDistance;
        long duration = lastTime;
        double pace = (distance > 0) ? (duration / 60.0) / distance : 0;
        int calories = (int) (distance * userWeight * 1.036);
        double elevation = lastElevation;
        String pathJson = new Gson().toJson(pathPoints);

        new Thread(() -> {
            // Fetch Location Name (Reverse Geocoding)
            String locationName = "Lokasi Tidak Diketahui";
            if (!pathPoints.isEmpty()) {
                try {
                    Geocoder geocoder = new Geocoder(requireContext(), new Locale("id", "ID"));
                    GeoPoint firstPoint = pathPoints.get(0);
                    List<Address> addresses = geocoder.getFromLocation(firstPoint.getLatitude(), firstPoint.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        
                        // Priority for City: SubAdminArea (e.g. Kota Bandung) -> AdminArea
                        String city = addr.getSubAdminArea();
                        if (city == null) city = addr.getLocality();
                        
                        // Priority for Area: SubLocality (e.g. Sukapura) -> Locality
                        String area = addr.getSubLocality();
                        if (area == null) area = addr.getLocality();

                        if (city != null && area != null && !city.equals(area)) {
                            locationName = city + " - " + area;
                        } else if (city != null) {
                            locationName = city;
                        } else if (area != null) {
                            locationName = area;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            RunRecord record = new RunRecord(timestamp, distance, duration, pace, calories, elevation, pathJson);
            record.setSplitsJson(new Gson().toJson(currentSplits));
            record.setElevationSplitsJson(new Gson().toJson(currentElevSplits));
            record.setCadenceSplitsJson(new Gson().toJson(currentCadSplits));
            record.setLocationName(locationName);

            AppDatabase.getInstance(requireContext()).runDao().insert(record);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Aktivitas disimpan!", Toast.LENGTH_SHORT).show();
                    goBack();
                });
            }
        }).start();
    }

    private void sendAction(String action) {
        Intent intent = new Intent(getContext(), TrackingService.class);
        intent.setAction(action);
        requireActivity().startService(intent);
    }

    private void goBack() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setNavigationVisibility(true);
        }
        getParentFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new HomeFragment()).commit();
    }

    @Override
    public void onSensorChanged(android.hardware.SensorEvent event) {
        if (event.sensor.getType() == android.hardware.Sensor.TYPE_ORIENTATION) {
            if (userMarker != null) {
                userMarker.setRotation(-event.values[0]);
                if (map != null) map.invalidate();
            }
        }
    }

    @Override
    public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {}

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(trackingReceiver, new IntentFilter(TrackingService.TRACKING_UPDATE));
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(trackingReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
        if (rotationSensor != null) sensorManager.registerListener(this, rotationSensor, android.hardware.SensorManager.SENSOR_DELAY_UI);
        startStatusLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
        sensorManager.unregisterListener(this);
        stopStatusLocationUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableInitialLocation();
            }
        } else if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Izin aktivitas fisik aktif!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopStatusLocationUpdates();
    }
}
