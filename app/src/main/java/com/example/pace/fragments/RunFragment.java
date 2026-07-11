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
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.pace.views.BarChartView;

public class RunFragment extends Fragment implements android.hardware.SensorEventListener {

    private MapView map = null;
    private Polyline polyline;
    private List<GeoPoint> pathPoints = new ArrayList<>();
    private MyLocationNewOverlay myLocationOverlay;
    private CompassOverlay compassOverlay;
    private FusedLocationProviderClient fusedLocationClient;
    private android.hardware.SensorManager sensorManager;
    private android.hardware.Sensor rotationSensor;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int ACTIVITY_RECOGNITION_REQUEST_CODE = 2;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 3;

    private Button btnStart, btnPause, btnStop;
    private Button btnPauseOverlay, btnStopOverlay;
    private ImageButton btnMinimize, btnLayerToggle, btnMyLocation, btnCompass;
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
    private double[] currentSplits = new double[0];
    private double[] currentElevSplits = new double[0];
    private int[] currentCadSplits = new int[0];
    private int userWeight = 70;

    private boolean isMapReady = false;
    private boolean isLocationReady = false;
    private boolean isStatsExpanded = false;

    private LocationCallback statusLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            android.location.Location loc = locationResult.getLastLocation();
            if (loc != null) updateGpsStatus(loc.getAccuracy());
        }
    };
    private GestureDetector gestureDetector;

    private boolean isSaving = false;

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity().getApplicationContext());
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
        btnLayerToggle = view.findViewById(R.id.btnLayerToggle);
        btnMyLocation = view.findViewById(R.id.btnMyLocation);
        btnCompass = view.findViewById(R.id.btnCompass);
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
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    showBackgroundLocationDialog();
                    return;
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
                    return;
                }
            }
            startRunning();
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

        btnLayerToggle.setOnClickListener(v -> showLayerSelectionDialog());
        btnMyLocation.setOnClickListener(v -> {
            if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
                map.getController().animateTo(myLocationOverlay.getMyLocation());
                map.getController().setZoom(18.0);
            } else {
                Toast.makeText(requireContext(), "Mencari sinyal GPS...", Toast.LENGTH_SHORT).show();
            }
        });
        btnCompass.setOnClickListener(v -> {
            map.setMapOrientation(0);
            Toast.makeText(requireContext(), "Peta menghadap Utara", Toast.LENGTH_SHORT).show();
        });
        if (btnMinimize != null) btnMinimize.setOnClickListener(v -> toggleStatsOverlay());
        panelStatsBottom.setOnClickListener(v -> toggleStatsOverlay());
        
        setupSwipeGestures();
    }

    private void setupSwipeGestures() {
        gestureDetector = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 != null && e2 != null) {
                    if (e1.getY() - e2.getY() > 100) { if (!isStatsExpanded) toggleStatsOverlay(); return true; }
                    else if (e2.getY() - e1.getY() > 100) { if (isStatsExpanded) toggleStatsOverlay(); return true; }
                }
                return false;
            }
        });
        View.OnTouchListener touchListener = (v, event) -> { gestureDetector.onTouchEvent(event); return false; };
        panelStatsBottom.setOnTouchListener(touchListener);
        layoutStatsOverlay.setOnTouchListener(touchListener);
    }

    private void toggleStatsOverlay() {
        isStatsExpanded = !isStatsExpanded;
        if (isStatsExpanded) {
            layoutStatsOverlay.setVisibility(View.VISIBLE);
            panelStatsBottom.setVisibility(View.GONE);
            // Hide Map Control Buttons when expanded
            btnLayerToggle.setVisibility(View.GONE);
            btnMyLocation.setVisibility(View.GONE);
            btnCompass.setVisibility(View.GONE);
        } else {
            layoutStatsOverlay.setVisibility(View.GONE);
            panelStatsBottom.setVisibility(View.VISIBLE);
            // Show Map Control Buttons when minimized
            btnLayerToggle.setVisibility(View.VISIBLE);
            btnMyLocation.setVisibility(View.VISIBLE);
            btnCompass.setVisibility(View.VISIBLE);
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
        polyline.getOutlinePaint().setColor(Color.parseColor("#C8F43A")); // Warna Lime sesuai aplikasi
        polyline.getOutlinePaint().setStrokeWidth(14f);
        map.getOverlayManager().add(polyline);

        RotationGestureOverlay rotationGestureOverlay = new RotationGestureOverlay(map);
        rotationGestureOverlay.setEnabled(true);
        map.getOverlays().add(rotationGestureOverlay);

        // My Location Overlay
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), map);
        myLocationOverlay.enableMyLocation();
        map.getOverlays().add(myLocationOverlay);

        // Compass Overlay
        compassOverlay = new CompassOverlay(requireContext(), new InternalCompassOrientationProvider(requireContext()), map);
        compassOverlay.enableCompass();
        map.getOverlays().add(compassOverlay);

        map.addOnFirstLayoutListener((v, left, top, right, bottom) -> {
            isMapReady = true;
            checkLoadingFinished();
        });
    }

    private void enableInitialLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null && !isTracking) {
                        GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                        map.getController().setCenter(point);
                        updateGpsStatus(location.getAccuracy());
                    }
                    isLocationReady = true;
                    checkLoadingFinished();
                })
                .addOnFailureListener(e -> { isLocationReady = true; checkLoadingFinished(); });
    }

    private void startStatusLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build();
        fusedLocationClient.requestLocationUpdates(request, statusLocationCallback, android.os.Looper.getMainLooper());
    }

    private void stopStatusLocationUpdates() {
        if (fusedLocationClient != null && statusLocationCallback != null) fusedLocationClient.removeLocationUpdates(statusLocationCallback);
    }

    private void checkLoadingFinished() {
        if (isMapReady && isLocationReady && layoutMapLoading != null) {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (layoutMapLoading != null) {
                    layoutMapLoading.animate().alpha(0).setDuration(500).withEndAction(() -> layoutMapLoading.setVisibility(View.GONE)).start();
                }
            }, 500);
        }
    }

    private void updateGpsStatus(float accuracy) {
        String statusText = (accuracy > 0 && accuracy < 20) ? "GPS Bagus" : "GPS Lemah";
        int resId = (accuracy > 0 && accuracy < 20) ? R.drawable.progress_fill : R.drawable.dot_red;
        int color = (accuracy > 0 && accuracy < 20) ? Color.parseColor("#C8F43A") : Color.RED;

        if (tvGpsStatus != null) { tvGpsStatus.setText(statusText); viewGpsIndicator.setBackgroundResource(resId); }
        if (tvOverlayGpsStatus != null) {
            String text = isAutoPaused ? "AUTO-PAUSED" : "Sinyal GPS " + statusText.split(" ")[1].toLowerCase();
            tvOverlayGpsStatus.setText(text);
            tvOverlayGpsStatus.setTextColor(isAutoPaused ? Color.YELLOW : color);
        }
    }

    private void showLayerSelectionDialog() {
        if (getContext() == null) return;
        
        String[] layers = {"Peta Standar (OSM)", "CyclOSM (Outdoor/Rute)", "Google Earth (Satelit)"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Pilih Lapisan Peta");
        builder.setItems(layers, (dialog, which) -> {
            if (map == null) return;
            
            switch (which) {
                case 0:
                    map.setTileSource(TileSourceFactory.MAPNIK);
                    if (map.getOverlayManager().getTilesOverlay() != null) {
                        map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
                    }
                    break;
                case 1:
                    XYTileSource cyclOSM = new XYTileSource("CyclOSM", 0, 20, 256, ".png",
                            new String[]{"https://a.tile-cyclosm.openstreetmap.fr/cyclosm/",
                                    "https://b.tile-cyclosm.openstreetmap.fr/cyclosm/",
                                    "https://c.tile-cyclosm.openstreetmap.fr/cyclosm/"}, "© CyclOSM contributors");
                    map.setTileSource(cyclOSM);
                    if (map.getOverlayManager().getTilesOverlay() != null) {
                        map.getOverlayManager().getTilesOverlay().setColorFilter(null);
                    }
                    break;
                case 2:
                    XYTileSource googleSatellite = new XYTileSource("GoogleSatellite", 0, 20, 256, ".png",
                            new String[]{"https://mt0.google.com/vt/lyrs=s&x={x}&y={y}&z={z}",
                                    "https://mt1.google.com/vt/lyrs=s&x={x}&y={y}&z={z}",
                                    "https://mt2.google.com/vt/lyrs=s&x={x}&y={y}&z={z}",
                                    "https://mt3.google.com/vt/lyrs=s&x={x}&y={y}&z={z}"});
                    map.setTileSource(googleSatellite);
                    if (map.getOverlayManager().getTilesOverlay() != null) {
                        map.getOverlayManager().getTilesOverlay().setColorFilter(null);
                    }
                    break;
            }
            map.invalidate(); 
        });
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateUIFromService(Intent intent) {
        isTracking = intent.getBooleanExtra("isTracking", false);
        isAutoPaused = intent.getBooleanExtra("isAutoPaused", false);

        // Pastikan overlay lokasi tetap aktif
        if (myLocationOverlay != null && !myLocationOverlay.isMyLocationEnabled()) {
            myLocationOverlay.enableMyLocation();
        }

        long time = intent.getLongExtra("time", 0);
        double distance = intent.getDoubleExtra("distance", 0.0);
        double elevation = intent.getDoubleExtra("elevation", 0.0);
        int steps = intent.getIntExtra("steps", 0);
        double lat = intent.getDoubleExtra("lat", 0);
        double lng = intent.getDoubleExtra("lng", 0);
        float accuracy = intent.getFloatExtra("accuracy", 0);
        
        ArrayList<GeoPoint> fullPath = intent.getParcelableArrayListExtra("fullPath");
        if (fullPath != null && !fullPath.isEmpty()) {
            pathPoints = fullPath;
            polyline.setPoints(pathPoints);
            updateWaypoints(pathPoints);
        }

        lastTime = time; lastDistance = distance; lastElevation = elevation;
        updateGpsStatus(accuracy);

        double[] tempSplits = intent.getDoubleArrayExtra("splits");
        if (tempSplits != null) currentSplits = tempSplits;
        double[] tempElev = intent.getDoubleArrayExtra("elevSplits");
        if (tempElev != null) currentElevSplits = tempElev;
        int[] tempCad = intent.getIntArrayExtra("cadSplits");
        if (tempCad != null) currentCadSplits = tempCad;

        tvDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", time / 60, time % 60));
        tvOverlayDuration.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", time / 3600, (time % 3600) / 60, time % 60));
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f", distance));
        tvOverlayDistance.setText(String.format(Locale.getDefault(), "%.1f", distance));
        
        String cal = String.valueOf((int)(distance * userWeight * 1.036));
        if (tvCalories != null) tvCalories.setText(cal);
        if (tvOverlayCalories != null) tvOverlayCalories.setText(cal);

        String s = (steps > 0) ? String.valueOf(steps) : "--";
        if (tvSteps != null) tvSteps.setText(s);
        if (tvOverlaySteps != null) tvOverlaySteps.setText(s);
        if (tvOverlayElevation != null) tvOverlayElevation.setText(String.format(Locale.getDefault(), "%.0fm Up", elevation));

        if (distance > 0.001) {
            double p = (time / 60.0) / distance;
            String ps = String.format(Locale.getDefault(), "%d:%02d", (int)p, (int)((p - (int)p) * 60));
            tvPace.setText(ps); tvOverlayAvgPace.setText(ps);
        }

        updateButtons(isTracking && !isAutoPaused);

        if (lat != 0 && lng != 0) {
            GeoPoint point = new GeoPoint(lat, lng);
            if (!isAutoPaused) map.getController().animateTo(point);
            map.invalidate();
        }

        double[] splits = intent.getDoubleArrayExtra("splits");
        if (splits != null && splits.length > 0 && splitBarChart != null) {
            float[] sp = new float[splits.length];
            for(int i=0; i<splits.length; i++) sp[i] = (float) (splits[i] / 60.0);
            splitBarChart.setData(sp);
        }
    }

    private void updateWaypoints(List<GeoPoint> points) {
        if (points == null || points.isEmpty()) return;

        // Clear existing markers except user marker
        map.getOverlays().removeIf(overlay -> {
            if (overlay instanceof Marker) {
                Marker m = (Marker) overlay;
                return "start".equals(m.getTitle()) || "finish".equals(m.getTitle());
            }
            return false;
        });

        // Hapus penanda bendera agar hanya ada satu ikon (panah lokasi)

        map.invalidate();
    }

    private void pasangWaypoint(GeoPoint lokasi, String id, String judul, int iconRes) {
        Marker marker = new Marker(map);
        marker.setPosition(lokasi);
        marker.setTitle(id);
        marker.setSubDescription(judul);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(ContextCompat.getDrawable(requireContext(), iconRes));
        map.getOverlays().add(marker);
    }

    private void updateButtons(boolean tracking) {
        if (isTracking) {
            btnStart.setVisibility(View.GONE); btnPause.setVisibility(View.VISIBLE); btnPauseOverlay.setVisibility(View.VISIBLE);
            if (isAutoPaused) { btnPause.setText("▶ LANJUT"); btnPauseOverlay.setText("▶ LANJUT"); btnStop.setVisibility(View.GONE); btnStopOverlay.setVisibility(View.GONE); }
            else { btnPause.setText("⏸ JEDA"); btnPauseOverlay.setText("⏸ JEDA"); btnStop.setVisibility(View.VISIBLE); btnStopOverlay.setVisibility(View.VISIBLE); }
        } else if (lastTime > 0) {
            btnStart.setVisibility(View.GONE); btnPause.setVisibility(View.VISIBLE); btnPauseOverlay.setVisibility(View.VISIBLE);
            btnPause.setText("▶ LANJUT"); btnPauseOverlay.setText("▶ LANJUT"); btnStop.setVisibility(View.GONE); btnStopOverlay.setVisibility(View.GONE);
        } else {
            btnStart.setVisibility(View.VISIBLE); btnPause.setVisibility(View.GONE); btnPauseOverlay.setVisibility(View.GONE);
            btnStop.setVisibility(View.GONE); btnStopOverlay.setVisibility(View.GONE);
        }
    }

    private void startRunning() {
        sendAction(TrackingService.ACTION_START);
        if (!isStatsExpanded) toggleStatsOverlay();
    }

    private void showBackgroundLocationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Izin Lokasi Latar Belakang")
                .setMessage("Agar rute tetap tercatat saat layar mati atau membuka aplikasi lain, silakan pilih 'Izinkan Sepanjang Waktu' (Allow all the time) pada halaman pengaturan berikutnya.")
                .setPositiveButton("Buka Pengaturan", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 999);
                })
                .setNegativeButton("Nanti Saja", (dialog, which) -> startRunning()).show();
    }

    private void saveRunBeforeStop() {
        if (isSaving) return;
        if (lastDistance < 0.005) { 
            sendAction(TrackingService.ACTION_STOP);
            goBack(); 
            return; 
        }
        isSaving = true;
        btnStop.setEnabled(false);
        if (btnStopOverlay != null) btnStopOverlay.setEnabled(false);

        long ts = System.currentTimeMillis(); double d = lastDistance; long dur = lastTime;
        double p = (d > 0) ? (dur / 60.0) / d : 0; int c = (int) (d * userWeight * 1.036);
        double e = lastElevation; String pj = new Gson().toJson(pathPoints);

        // Capture split data
        String sJson = new Gson().toJson(currentSplits);
        String eJson = new Gson().toJson(currentElevSplits);
        String cJson = new Gson().toJson(currentCadSplits);

        new Thread(() -> {
            String ln = "Lokasi Tidak Diketahui";
            if (!pathPoints.isEmpty()) {
                try {
                    Geocoder geo = new Geocoder(requireContext(), new Locale("id", "ID"));
                    List<Address> ads = geo.getFromLocation(pathPoints.get(0).getLatitude(), pathPoints.get(0).getLongitude(), 1);
                    if (ads != null && !ads.isEmpty()) {
                        Address a = ads.get(0);
                        String city = a.getSubAdminArea() != null ? a.getSubAdminArea() : a.getLocality();
                        String area = a.getSubLocality() != null ? a.getSubLocality() : a.getLocality();
                        if (city != null && area != null && !city.equals(area)) ln = city + " - " + area;
                        else if (city != null) ln = city; else if (area != null) ln = area;
                    }
                } catch (Exception ex) {}
            }
            RunRecord r = new RunRecord(ts, d, dur, p, c, e, pj);
            r.setLocationName(ln);
            r.setSplitsJson(sJson);
            r.setElevationSplitsJson(eJson);
            r.setCadenceSplitsJson(cJson);

            AppDatabase.getInstance(requireContext()).runDao().insert(r);
            if (getActivity() != null) getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Aktivitas disimpan!", Toast.LENGTH_SHORT).show();
                isSaving = false;
                goBack();
            });
        }).start();
    }

    private void sendAction(String a) {
        Intent i = new Intent(getContext(), TrackingService.class); i.setAction(a);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) requireContext().startForegroundService(i);
        else requireContext().startService(i);
    }

    private void goBack() {
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).setNavigationVisibility(true);
        getParentFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new HomeFragment()).commit();
    }

    @Override
    public void onSensorChanged(android.hardware.SensorEvent event) {
    }

    @Override
    public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {}

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setNavigationVisibility(false);
        }
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(trackingReceiver, new IntentFilter(TrackingService.TRACKING_UPDATE));
    }

    @Override
    public void onStop() { super.onStop(); LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(trackingReceiver); }

    @Override
    public void onResume() { 
        super.onResume(); 
        if (map != null) map.onResume(); 
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
        if (compassOverlay != null) compassOverlay.enableCompass();
        if (rotationSensor != null) sensorManager.registerListener(this, rotationSensor, android.hardware.SensorManager.SENSOR_DELAY_UI); 
        startStatusLocationUpdates(); 
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(trackingReceiver, new IntentFilter(TrackingService.TRACKING_UPDATE));
    }

    @Override
    public void onPause() { 
        super.onPause(); 
        if (map != null) map.onPause(); 
        if (myLocationOverlay != null) myLocationOverlay.disableMyLocation();
        if (compassOverlay != null) compassOverlay.disableCompass();
        sensorManager.unregisterListener(this); 
        stopStatusLocationUpdates(); 
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(trackingReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int rc, @NonNull String[] p, @NonNull int[] gr) {
        super.onRequestPermissionsResult(rc, p, gr);
        if (rc == LOCATION_PERMISSION_REQUEST_CODE) {
            if (gr.length > 0 && gr[0] == PackageManager.PERMISSION_GRANTED) enableInitialLocation();
        } else if (rc == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            if (gr.length > 0 && gr[0] == PackageManager.PERMISSION_GRANTED) Toast.makeText(getContext(), "Izin aktivitas fisik aktif!", Toast.LENGTH_SHORT).show();
        } else if (rc == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (gr.length > 0 && gr[0] == PackageManager.PERMISSION_GRANTED) startRunning();
        } else if (rc == 999) {
            startRunning();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setNavigationVisibility(true);
        }
        stopStatusLocationUpdates();
    }
}
