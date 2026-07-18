package com.example.pace.utils;

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

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiAssistant {
    private static final String TAG = "GeminiAssistant";
    
    // Rotation of 9 API Keys to handle strict rate limits
    // Note: Add your API keys here or use environment variables
    private static final String[] API_KEYS = {
            "YOUR_API_KEY_1", // Replace with actual key
            "YOUR_API_KEY_2", // Replace with actual key
            "YOUR_API_KEY_3", // Replace with actual key
            "YOUR_API_KEY_4", // Replace with actual key
            "YOUR_API_KEY_5", // Replace with actual key
            "YOUR_API_KEY_6", // Replace with actual key
            "YOUR_API_KEY_7", // Replace with actual key
            "YOUR_API_KEY_8", // Replace with actual key
            "YOUR_API_KEY_9"  // Replace with actual key
    };
    
    private static int currentKeyIndex = 0;
    private static final String MODEL_NAME = "gemini-3.5-flash"; // Recommended stable model

    private GenerativeModelFutures model;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final GenerationConfig config;

    public GeminiAssistant() {
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

    public void generateRunInsights(User user, RunRecord run, AIResponseCallback callback) {
        String name = (user.getName() != null) ? user.getName().split(" ")[0] : "Runner";
        String prompt = String.format(Locale.getDefault(),
                "Analyze this run session for %s. Goal: %s. Stats: %.2fkm, %d mins, %s pace. Fatigue: %d/10. " +
                "Provide a professional, technical, and encouraging summary. No greetings or closings. Max 3 sentences.",
                name, user.getGoal(), run.getDistance(), run.getDuration() / 60, run.getPace(), run.getFatigueLevel());
        
        chat(prompt, name, true, callback);
    }

    public void generateWeatherAdvice(String weatherData, String goal, String userName, AIResponseCallback callback) {
        String prompt = String.format(Locale.getDefault(),
                "Weather: %s. Running Goal: %s. Recommended hours to run today for %s. No greetings or closings. Max 2 sentences.",
                weatherData, goal, userName);
        chat(prompt, userName, true, callback);
    }

    public void chat(String prompt, String userName, boolean technicalMode, AIResponseCallback callback) {
        chatWithRetry(prompt, userName, technicalMode, callback, 0);
    }

    private void chatWithRetry(String prompt, String userName, boolean technicalMode, AIResponseCallback callback, int retryCount) {
        if (model == null) {
            callback.onError("AI Coach is temporarily offline.");
            return;
        }

        String systemPrompt;
        if (technicalMode) {
            systemPrompt = "You are a professional technical Running Coach for the OUTPACE app. " +
                    "Provide accurate, concise, and helpful data analysis. NO GREETINGS, NO CLOSINGS. Just the facts and advice. " +
                    "Answer in English. NO MARKDOWN. \n\n" +
                    "Query: " + prompt;
        } else {
            systemPrompt = "You are a friendly and encouraging Running Coach for the app 'OUTPACE'.\n" +
                    "RULES:\n" +
                    "1. Only answer topics about running, fitness, marathon, and nutrition.\n" +
                    "2. Deny other topics politely and redirect to running.\n" +
                    "3. NO MARKDOWN (no stars, hashes, etc). Clean text only.\n" +
                    "4. Be friendly and informal like a real coach, call the user 'Kak " + userName + "'.\n" +
                    "5. Response must be in English.\n" +
                    "6. Be concise (max 3 sentences).\n" +
                    "7. End every chat response with: 'Anything else Coach can help you with, Kak " + userName + "?'\n\n" +
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
                    
                    if ((msg.contains("429") || msg.contains("quota")) && retryCount < API_KEYS.length - 1) {
                        rotateKey();
                        chatWithRetry(prompt, userName, technicalMode, callback, retryCount + 1);
                    } else if (msg.contains("503") || msg.contains("demand") || msg.contains("UNAVAILABLE")) {
                        callback.onError("Coach is very busy. Try again in 15 seconds.");
                    } else {
                        callback.onError("Connection Error. Check internet.");
                    }
                }
            }, executor);
        } catch (Exception e) {
            callback.onError("Communication error.");
        }
    }

    public void analyzeImage(Bitmap bitmap, String userPrompt, String userName, AIResponseCallback callback) {
        if (model == null) return;
        String systemPrompt = "Analyze this food/drink photo for a runner in English.\n" +
                "RULES: Friendly, call user 'Kak " + userName + "', NO markdown, max 2 sentences.\n" +
                "User Query: " + userPrompt;
        Content content = new Content.Builder().addImage(bitmap).addText(systemPrompt).build();
        try {
            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    try {
                        String text = result.getText();
                        if (text != null) {
                            String cleanedText = text.replace("*", "").replace("#", "").trim();
                            callback.onSuccess(cleanedText);
                        } else {
                            callback.onError("Failed to read image.");
                        }
                    } catch (Exception e) {
                        callback.onError("AI Vision busy.");
                    }
                }
                @Override
                public void onFailure(Throwable t) {
                    callback.onError("AI Vision is busy.");
                }
            }, executor);
        } catch (Exception e) {
            callback.onError("Image processing failed.");
        }
    }
}
