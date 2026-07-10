package com.example.pace.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.pace.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class WeatherActivity extends AppCompatActivity {
    private TextView tvDetailTemp, tvWeatherDesc, tvHumidity, tvWindSpeed, tvMainWeatherIcon;
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id", "ID"));
        tvDate.setText(dateFormat.format(calendar.getTime()));

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvTime.setText(timeFormat.format(calendar.getTime()));
    }

    private void fetchWeatherData(String city) {
        String url = "https://api.openweathermap.org/data/2.5/forecast?q=" + city + "&appid=" + API_KEY + "&units=metric&lang=id";

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray list = response.getJSONArray("list");
                        
                        JSONObject current = list.getJSONObject(0);
                        updateCurrentWeather(current);

                        llHourlyContainer.removeAllViews();
                        for (int i = 0; i < 8; i++) {
                            addHourlyItem(list.getJSONObject(i));
                        }

                        llDailyContainer.removeAllViews();
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject item = list.getJSONObject(i);
                            if (item.getString("dt_txt").contains("12:00:00")) {
                                addDailyItem(item);
                            }
                        }
                        
                        startEntryAnimations();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Gagal mengambil data cuaca", Toast.LENGTH_SHORT).show());

        queue.add(jsonObjectRequest);
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

        tvDetailTemp.setText(Math.round(temp) + "°C");
        tvWeatherDesc.setText(description.substring(0, 1).toUpperCase() + description.substring(1));
        tvHumidity.setText(humidity + "%");
        tvWindSpeed.setText(speed + " km/h");
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
        tvTemp.setText(Math.round(temp) + "°");

        llHourlyContainer.addView(view);
    }

    private void addDailyItem(JSONObject dailyData) throws JSONException {
        View view = LayoutInflater.from(this).inflate(R.layout.item_weather_day, llDailyContainer, false);
        TextView tvDay = view.findViewById(R.id.tvDayName);
        TextView tvIcon = view.findViewById(R.id.tvDayIcon);
        TextView tvTemp = view.findViewById(R.id.tvDayTemp);

        long dt = dailyData.getLong("dt") * 1000;
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMM", new Locale("id", "ID"));
        tvDay.setText(sdf.format(new Date(dt)));

        String iconCode = dailyData.getJSONArray("weather").getJSONObject(0).getString("icon");
        tvIcon.setText(getWeatherEmoji(iconCode));

        JSONObject main = dailyData.getJSONObject("main");
        double min = main.getDouble("temp_min");
        double max = main.getDouble("temp_max");
        tvTemp.setText(Math.round(min) + "° / " + Math.round(max) + "°");

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
