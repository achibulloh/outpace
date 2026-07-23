package com.example.pace.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.pace.R;
import com.example.pace.utils.GeminiAssistant;
import com.example.pace.utils.LocaleHelper;
import com.example.pace.utils.VolleySingleton;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class WeatherActivity extends AppCompatActivity {
    private TextView tvDetailTemp, tvWeatherDesc, tvHumidity, tvWindSpeed, tvMainWeatherIcon, tvAIWeatherInsights;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
    private LinearLayout llHourlyContainer, llDailyContainer;
    private final String API_KEY = "565c5268e3209aa0aa5599c00cc0c232";
    private View layoutMainWeather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvCurrentDayDate = findViewById(R.id.tvCurrentDayDate);
        TextView tvCurrentTime = findViewById(R.id.tvCurrentTime);
        
        tvDetailTemp = findViewById(R.id.tvDetailTemp);
        tvWeatherDesc = findViewById(R.id.tvWeatherDesc);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvWindSpeed = findViewById(R.id.tvWindSpeed);
        tvMainWeatherIcon = findViewById(R.id.tvMainWeatherIcon);
        tvAIWeatherInsights = findViewById(R.id.tvAIWeatherInsights);
        llHourlyContainer = findViewById(R.id.llHourlyContainer);
        llDailyContainer = findViewById(R.id.llDailyContainer);
        layoutMainWeather = findViewById(R.id.layoutMainWeather); // Ensure this exists in XML

        btnBack.setOnClickListener(v -> finish());

        setCurrentDateTime(tvCurrentDayDate, tvCurrentTime);
        fetchWeatherData("Jakarta");
        
        // Initial hidden state for animations
        if (layoutMainWeather != null) layoutMainWeather.setAlpha(0);
        llHourlyContainer.setAlpha(0);
        llDailyContainer.setAlpha(0);
    }

    private void setCurrentDateTime(TextView tvDate, TextView tvTime) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault());
        tvDate.setText(dateFormat.format(calendar.getTime()));

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvTime.setText(timeFormat.format(calendar.getTime()));
    }

    private void fetchWeatherData(String city) {
        String langCode = LocaleHelper.getLanguage(this);
        if (langCode.equals("in")) langCode = "id";
        
        String url = "https://api.openweathermap.org/data/2.5/forecast?q=" + city + "&appid=" + API_KEY + "&units=metric&lang=" + langCode;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray list = response.getJSONArray("list");
                        if (list.length() > 0) {
                            JSONObject current = list.getJSONObject(0);
                            updateCurrentWeather(current);

                            llHourlyContainer.removeAllViews();
                            StringBuilder forecastSummary = new StringBuilder();
                            int hourlyCount = Math.min(8, list.length());
                            for (int i = 0; i < hourlyCount; i++) {
                                JSONObject hourlyData = list.getJSONObject(i);
                                addHourlyItem(hourlyData);
                                
                                // Build summary for AI
                                String time = hourlyData.getString("dt_txt").split(" ")[1].substring(0, 5);
                                double temp = hourlyData.getJSONObject("main").getDouble("temp");
                                String desc = hourlyData.getJSONArray("weather").getJSONObject(0).getString("description");
                                forecastSummary.append(String.format("%s:%.1fC,%s; ", time, temp, desc));
                            }
                            
                            // Send to AI for advice
                            fetchAIWeatherAdviceDetailed(forecastSummary.toString());

                            llDailyContainer.removeAllViews();
                            boolean firstDaily = true;
                            for (int i = 0; i < list.length(); i++) {
                                JSONObject item = list.getJSONObject(i);
                                if (item.getString("dt_txt").contains("12:00:00")) {
                                    if (!firstDaily) {
                                        View divider = new View(this);
                                        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
                                        divider.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.muted));
                                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) divider.getLayoutParams();
                                        params.setMargins(0, 16, 0, 16);
                                        llDailyContainer.addView(divider);
                                    }
                                    addDailyItem(item);
                                    firstDaily = false;
                                }
                            }
                            
                            startEntryAnimations();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Gagal mengambil data cuaca", Toast.LENGTH_SHORT).show());

        VolleySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }

    private void fetchAIWeatherAdviceDetailed(String forecastSummary) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // SMART CACHE (LOCAL + CLOUD)
        android.content.SharedPreferences prefs = getSharedPreferences("ai_cache", Context.MODE_PRIVATE);
        String hourKey = new java.text.SimpleDateFormat("yyyy-MM-dd_HH", Locale.getDefault()).format(new java.util.Date());
        String weatherKey = "weather_advice_" + hourKey;
        String cachedAdvice = prefs.getString(weatherKey, null);

        // Don't use cache if it was an error message
        if (cachedAdvice != null && !cachedAdvice.contains("Connection Error") && !cachedAdvice.contains("Coach is very busy")) {
            tvAIWeatherInsights.setText(cachedAdvice);
            return;
        }

        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String cloudKey = documentSnapshot.getString("cachedWeatherKey");
                        if (hourKey.equals(cloudKey)) {
                            String cloudAdvice = documentSnapshot.getString("cachedWeatherAdvice");
                            if (cloudAdvice != null && !cloudAdvice.contains("Connection Error")) {
                                prefs.edit().putString(weatherKey, cloudAdvice).apply();
                                runOnUiThread(() -> tvAIWeatherInsights.setText(cloudAdvice));
                                return;
                            }
                        }
                    }
                    
                    // CALL AI IF NO CACHE OR CACHE IS INVALID
                    generateNewWeatherAdvice(forecastSummary, user.getUid(), hourKey, prefs);
                });
    }

    private void generateNewWeatherAdvice(String forecastSummary, String uid, String hourKey, android.content.SharedPreferences prefs) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    com.example.pace.model.User u = documentSnapshot.toObject(com.example.pace.model.User.class);
                    if (u == null) return;

                    String firstName = u.getName().split(" ")[0];
                    String lang = LocaleHelper.getLanguage(WeatherActivity.this);
                    GeminiAssistant.getInstance().generateWeatherAdvice(WeatherActivity.this, forecastSummary, u.getGoal(), firstName, lang,
                            new GeminiAssistant.AIResponseCallback() {
                        @Override
                        public void onSuccess(String response) {
                            runOnUiThread(() -> {
                                if (!response.contains("retry") && !response.contains("busy") && !response.contains("{") && !response.contains("Unexpected")) {
                                    prefs.edit().putString("weather_advice_" + hourKey, response).apply();
                                    // Save to Firestore
                                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                                    data.put("cachedWeatherKey", hourKey);
                                    data.put("cachedWeatherAdvice", response);
                                    FirebaseFirestore.getInstance().collection("users").document(uid).update(data);
                                }
                                tvAIWeatherInsights.setText(response);
                            });
                        }
                        @Override
                        public void onError(String friendlyError) {
                            runOnUiThread(() -> {
                                // If it's a connection error, show a more helpful message and don't cache it
                                if (friendlyError.contains("Connection Error")) {
                                    tvAIWeatherInsights.setText(R.string.ai_coach_connection_error);
                                } else {
                                    tvAIWeatherInsights.setText(friendlyError);
                                }
                            });
                        }
                    });
                });
    }

    private void startEntryAnimations() {
        if (layoutMainWeather != null) {
            layoutMainWeather.setTranslationY(50);
            layoutMainWeather.animate().alpha(1).translationY(0).setDuration(600).setInterpolator(new DecelerateInterpolator()).start();
        }
        
        llHourlyContainer.setTranslationY(50);
        llHourlyContainer.animate().alpha(1).translationY(0).setDuration(600).setStartDelay(200).setInterpolator(new DecelerateInterpolator()).start();
        
        llDailyContainer.setTranslationY(50);
        llDailyContainer.animate().alpha(1).translationY(0).setDuration(600).setStartDelay(400).setInterpolator(new DecelerateInterpolator()).start();
    }

    private void updateCurrentWeather(JSONObject current) throws JSONException {
        JSONObject main = current.getJSONObject("main");
        double temp = main.getDouble("temp");
        int humidity = main.getInt("humidity");
        
        JSONObject wind = current.getJSONObject("wind");
        double speed = wind.getDouble("speed");
        
        JSONObject weather = current.getJSONArray("weather").getJSONObject(0);
        String description = weather.getString("description");
        String iconCode = weather.getString("icon");

        tvDetailTemp.setText(getString(R.string.stat_temp_val, (int) Math.round(temp)));
        if (description != null && !description.isEmpty()) {
            tvWeatherDesc.setText(description.substring(0, 1).toUpperCase() + description.substring(1));
        } else {
            tvWeatherDesc.setText("");
        }
        tvHumidity.setText(getString(R.string.humidity_val, humidity));
        tvWindSpeed.setText(getString(R.string.wind_speed_val, speed));
        tvMainWeatherIcon.setText(getWeatherEmoji(iconCode));
    }

    private void addHourlyItem(JSONObject hourData) throws JSONException {
        View view = LayoutInflater.from(this).inflate(R.layout.item_weather_hour, llHourlyContainer, false);
        TextView tvTime = view.findViewById(R.id.tvHourTime);
        TextView tvIcon = view.findViewById(R.id.tvHourIcon);
        TextView tvTemp = view.findViewById(R.id.tvHourTemp);

        long dt = hourData.getLong("dt") * 1000;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvTime.setText(sdf.format(new Date(dt)));

        String iconCode = hourData.getJSONArray("weather").getJSONObject(0).getString("icon");
        tvIcon.setText(getWeatherEmoji(iconCode));

        double temp = hourData.getJSONObject("main").getDouble("temp");
        tvTemp.setText(getString(R.string.temp_val, (int) Math.round(temp)));

        llHourlyContainer.addView(view);
    }

    private void addDailyItem(JSONObject dailyData) throws JSONException {
        View view = LayoutInflater.from(this).inflate(R.layout.item_weather_day, llDailyContainer, false);
        TextView tvDay = view.findViewById(R.id.tvDayName);
        TextView tvIcon = view.findViewById(R.id.tvDayIcon);
        TextView tvTemp = view.findViewById(R.id.tvDayTemp);

        long dt = dailyData.getLong("dt") * 1000;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dt);
        
        Calendar today = Calendar.getInstance();
        if (cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) + 1 &&
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
            tvDay.setText(R.string.tomorrow);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMM", Locale.getDefault());
            tvDay.setText(sdf.format(new Date(dt)));
        }

        String iconCode = dailyData.getJSONArray("weather").getJSONObject(0).getString("icon");
        tvIcon.setText(getWeatherEmoji(iconCode));

        JSONObject main = dailyData.getJSONObject("main");
        double min = main.getDouble("temp_min");
        double max = main.getDouble("temp_max");
        tvTemp.setText(getString(R.string.temp_range_val, (int) Math.round(min), (int) Math.round(max)));

        llDailyContainer.addView(view);
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
}
