package com.example.pace.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.pace.model.RunRecord;
import com.example.pace.model.User;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeminiAssistant {
    private static final String TAG = "GeminiAssistant";
    private static final int DAILY_LIMIT = 10;
    
    // API keys must NOT be hard-coded. Provide keys via secure config (BuildConfig, env, or Secrets Manager).
    // Replace the placeholder below with your runtime key injection (do NOT commit real keys).
    private static final String[] API_KEYS = {
            "REPLACE_WITH_API_KEY"
    };
    
    private static int currentKeyIndex = 0;
    private static final String MODEL_NAME = "gemini-3.5-flash";

    private static GeminiAssistant instance;
    private GenerativeModelFutures model;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final GenerationConfig config;

    public static synchronized GeminiAssistant getInstance() {
        if (instance == null) {
            instance = new GeminiAssistant();
        }
        return instance;
    }

    private GeminiAssistant() {
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.maxOutputTokens = 1000;
        configBuilder.temperature = 0.4f;
        configBuilder.topP = 0.9f;
        this.config = configBuilder.build();
        initModel();
    }

    private void initModel() {
        try {
            GenerativeModel gm = new GenerativeModel(MODEL_NAME, API_KEYS[currentKeyIndex], config);
            this.model = GenerativeModelFutures.from(gm);
        } catch (Exception e) {
            Log.e(TAG, "AI init failed", e);
        }
    }

    private void rotateKey() {
        currentKeyIndex = (currentKeyIndex + 1) % API_KEYS.length;
        Log.d(TAG, "Rotating to API Key index: " + currentKeyIndex);
        initModel();
    }

    public interface AIResponseCallback {
        void onSuccess(String response);
        void onError(String friendlyError);
    }

    public void generateRunInsights(Context context, User user, RunRecord run, String lang, AIResponseCallback callback) {
        if (!canProcessRequest(context)) {
            callback.onError("Your daily AI limit reached (10/10). Try again tomorrow.");
            return;
        }
        String name = (user.getName() != null) ? user.getName().split(" ")[0] : "Runner";
        String prompt = String.format(Locale.getDefault(),
                "Analyze this run session for %s. Goal: %s. Stats: %.2fkm, %d mins, %s pace. Fatigue: %d/10. " +
                "Provide a professional, technical, and encouraging summary. No greetings or closings. Max 3 sentences.",
                name, user.getGoal(), run.getDistance(), run.getDuration() / 60, run.getPace(), run.getFatigueLevel());
        
        chatWithRetry(context, prompt, name, true, lang, callback, 0);
    }

    public void generateWeatherAdvice(Context context, String weatherData, String goal, String userName, String lang, AIResponseCallback callback) {
        if (!canProcessRequest(context)) {
            callback.onError("Your daily AI limit reached (10/10). Try again tomorrow.");
            return;
        }
        String prompt = String.format(Locale.getDefault(),
                "Weather: %s. Running Goal: %s. Recommended hours to run today for %s. No greetings or closings. Max 2 sentences.",
                weatherData, goal, userName);
        chatWithRetry(context, prompt, userName, true, lang, callback, 0);
    }

    public void chat(Context context, String prompt, String userName, boolean technicalMode, String lang, AIResponseCallback callback) {
        if (!canProcessRequest(context)) {
            callback.onError("Your daily AI limit reached (10/10). Try again tomorrow.");
            return;
        }
        chatWithRetry(context, prompt, userName, technicalMode, lang, callback, 0);
    }

    private boolean canProcessRequest(Context context) {
        if (context == null) return true; // Safety
        SharedPreferences prefs = context.getSharedPreferences("ai_usage", Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastDate = prefs.getString("last_request_date", "");
        
        int count = prefs.getInt("request_count", 0);
        if (!today.equals(lastDate)) {
            // New day, reset counter
            prefs.edit().putString("last_request_date", today).putInt("request_count", 0).apply();
            return true;
        }
        
        return count < DAILY_LIMIT;
    }

    private void incrementUsage(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences("ai_usage", Context.MODE_PRIVATE);
        int count = prefs.getInt("request_count", 0);
        prefs.edit().putInt("request_count", count + 1).apply();
    }

    private void chatWithRetry(Context context, String prompt, String userName, boolean technicalMode, String lang, AIResponseCallback callback, int retryCount) {
        if (model == null) {
            callback.onError("AI Coach is temporarily offline.");
            return;
        }

        String targetLang = "English";
        String prefix = "Runner";
        String closing = "Is there anything else Coach can help you with, " + userName + "?";
        
        if (lang != null && (lang.equals("id") || lang.equals("in"))) {
            targetLang = "Indonesian";
            prefix = "Kak";
            closing = "Ada lagi yang bisa Coach bantu, Kak " + userName + "?";
        }

        String systemPrompt;
        if (technicalMode) {
            systemPrompt = "You are a professional technical Running Coach for the OUTPACE app. " +
                    "Provide accurate, concise, and helpful data analysis. NO GREETINGS, NO CLOSINGS. Just the facts and advice. " +
                    "Answer in " + targetLang + ". NO MARKDOWN. \n\n" +
                    "Query: " + prompt;
        } else {
            systemPrompt = "You are a friendly and encouraging Running Coach for the app 'OUTPACE'.\n" +
                    "RULES:\n" +
                    "1. Only answer topics about running, fitness, marathon, and nutrition.\n" +
                    "2. Deny other topics politely and redirect to running.\n" +
                    "3. NO MARKDOWN (no stars, hashes, etc). Clean text only.\n" +
                    "4. Be friendly and informal like a real coach, call the user '" + prefix + " " + userName + "'.\n" +
                    "5. Response must be in " + targetLang + ".\n" +
                    "6. Be concise (max 3 sentences).\n" +
                    "7. End every chat response with: '" + closing + "'\n\n" +
                    "User Query: " + prompt;
        }

        Content content = new Content.Builder().addText(systemPrompt).build();

        try {
            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    try {
                        String text = result.getText();
                        if (text != null && !text.isEmpty()) {
                            incrementUsage(context);
                            String cleanedText = text.replace("*", "").replace("#", "").trim();
                            callback.onSuccess(cleanedText);
                        } else {
                            callback.onError("AI is busy. Please try again in a few seconds.");
                        }
                    } catch (Exception e) {
                        callback.onError("AI analysis error.");
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    String msg = t.getMessage() != null ? t.getMessage() : "Unknown";
                    Log.e(TAG, "Gemini error: " + msg);
                    
                    if ((msg.contains("429") || msg.contains("quota") || msg.contains("limit")) && retryCount < API_KEYS.length - 1) {
                        rotateKey();
                        chatWithRetry(context, prompt, userName, technicalMode, lang, callback, retryCount + 1);
                    } else if (msg.contains("429") || msg.contains("quota") || msg.contains("limit")) {
                        // All keys exhausted
                        callback.onError("AI Quota Limit reached. Please try again later.");
                    } else if (msg.contains("503") || msg.contains("demand") || msg.contains("UNAVAILABLE")) {
                        if (retryCount < 3) { // Try a few times for 503 as well
                            chatWithRetry(context, prompt, userName, technicalMode, lang, callback, retryCount + 1);
                        } else {
                            callback.onError("Coach is very busy. Try again in 15 seconds.");
                        }
                    } else {
                        callback.onError("Connection Error. Check internet.");
                    }
                }
            }, executor);
        } catch (Exception e) {
            callback.onError("Communication error.");
        }
    }

    public void analyzeImage(Context context, Bitmap bitmap, String userPrompt, String userName, String lang, AIResponseCallback callback) {
        if (!canProcessRequest(context)) {
            callback.onError("Daily AI limit reached (10/10). Try again tomorrow.");
            return;
        }

        if (model == null) {
            callback.onError("AI Vision is offline.");
            return;
        }

        String targetLang = lang != null && (lang.equals("id") || lang.equals("in")) ? "Indonesian" : "English";

        String systemPrompt = "Analyze this photo for a runner in " + targetLang + ". If it is food/drink, give short health advice. " +
                "Be friendly, call user 'Kak " + userName + "', NO markdown, max 2 sentences.\n" +
                "User Query: " + (userPrompt.isEmpty() ? "Analyze this" : userPrompt);

        Content content = new Content.Builder()
                .addImage(bitmap)
                .addText(systemPrompt)
                .build();

        try {
            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    try {
                        String text = result.getText();
                        if (text != null && !text.isEmpty()) {
                            incrementUsage(context);
                            String cleanedText = text.replace("*", "").replace("#", "").trim();
                            callback.onSuccess(cleanedText);
                        } else {
                            callback.onError("AI couldn't see the image clearly. Please retry.");
                        }
                    } catch (Exception e) {
                        callback.onError("Vision processing error.");
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    String msg = t.getMessage() != null ? t.getMessage() : "Unknown vision error";
                    Log.e(TAG, "Gemini Vision Error: " + msg);
                    
                    if (msg.contains("429") || msg.contains("quota")) {
                        rotateKey();
                        callback.onError("Vision quota reached. Retrying with next key...");
                    } else {
                        callback.onError("Vision Error: " + msg);
                    }
                }
            }, executor);
        } catch (Exception e) {
            callback.onError("Critical vision failure.");
        }
    }

    public void shutdown() {
        try {
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
            }
        } catch (Exception e) {
            Log.e(TAG, "Shutdown error", e);
        }
    }
}
