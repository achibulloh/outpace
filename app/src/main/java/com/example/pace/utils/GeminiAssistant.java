package com.example.pace.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.pace.model.GeminiKey;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeminiAssistant {
    private static final String TAG = "GeminiAssistant";
    private static final int DAILY_LIMIT = 10;
    private static final String MODEL_NAME = "gemini-3.1-flash-lite";

    private static GeminiAssistant instance;
    private GenerativeModelFutures model;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final GenerationConfig config;
    
    private GeminiKey currentKey;
    private boolean isFetchingKey = false;

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
    }

    private void fetchActiveKey(Context context, Runnable onReady, AIResponseCallback callback) {
        if (isFetchingKey) return;
        isFetchingKey = true;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Sederhanakan query agar tidak butuh Composite Index manual di Firebase
        db.collection("gemini_keys")
                .whereEqualTo("status", "active")
                .limit(10) 
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    isFetchingKey = false;
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Load Balancing: Pick one at random from the active keys
                        int randomIndex = (int) (Math.random() * queryDocumentSnapshots.size());
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(randomIndex);
                        
                        currentKey = doc.toObject(GeminiKey.class);
                        if (currentKey != null && currentKey.getKey() != null && !currentKey.getKey().isEmpty()) {
                            currentKey.setId(doc.getId());
                            Log.d(TAG, "Key selected: " + doc.getId() + " by " + currentKey.getOwner());
                            initModel(currentKey.getKey());
                            if (onReady != null) onReady.run();
                        } else {
                            Log.e(TAG, "Selected key is null or empty. ID: " + doc.getId());
                            if (callback != null) callback.onError("Failed to process AI credentials.");
                        }
                    } else {
                        if (callback != null) callback.onError("AI Coaching limit reached globally. Try again later.");
                    }
                })
                .addOnFailureListener(e -> {
                    isFetchingKey = false;
                    Log.e(TAG, "Firestore Error: " + e.getMessage());
                    if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                        callback.onError("Firestore access denied. Please check Firebase Rules.");
                    } else {
                        callback.onError("AI Connection Error: " + e.getLocalizedMessage());
                    }
                });
    }

    private void initModel(String apiKey) {
        try {
            GenerativeModel gm = new GenerativeModel(MODEL_NAME, apiKey, config);
            this.model = GenerativeModelFutures.from(gm);
        } catch (Exception e) {
            Log.e(TAG, "AI init failed", e);
        }
    }

    private void updateKeyStatus(String status, String errorMessage) {
        if (currentKey == null) return;
        FirebaseFirestore.getInstance().collection("gemini_keys")
                .document(currentKey.getId())
                .update("status", status, "error_message", errorMessage);
        currentKey = null; 
    }

    private void incrementKeyUsage() {
        if (currentKey == null) return;
        FirebaseFirestore.getInstance().collection("gemini_keys")
                .document(currentKey.getId())
                .update("usage_count", FieldValue.increment(1), 
                        "last_used", FieldValue.serverTimestamp());
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
        
        executeChat(context, prompt, name, true, lang, callback);
    }

    public void generateWeatherAdvice(Context context, String weatherData, String goal, String userName, String lang, AIResponseCallback callback) {
        if (!canProcessRequest(context)) {
            callback.onError("Your daily AI limit reached (10/10). Try again tomorrow.");
            return;
        }
        String prompt = String.format(Locale.getDefault(),
                "Weather: %s. Running Goal: %s. Recommended hours to run today for %s. No greetings or closings. Max 2 sentences.",
                weatherData, goal, userName);
        executeChat(context, prompt, userName, true, lang, callback);
    }

    public void chat(Context context, String prompt, String userName, boolean technicalMode, String lang, AIResponseCallback callback) {
        if (!canProcessRequest(context)) {
            callback.onError("Your daily AI limit reached (10/10). Try again tomorrow.");
            return;
        }
        executeChat(context, prompt, userName, technicalMode, lang, callback);
    }

    private void executeChat(Context context, String prompt, String userName, boolean technicalMode, String lang, AIResponseCallback callback) {
        if (currentKey == null) {
            fetchActiveKey(context, () -> chatWithRetry(context, prompt, userName, technicalMode, lang, callback, 0), callback);
        } else {
            chatWithRetry(context, prompt, userName, technicalMode, lang, callback, 0);
        }
    }

    private boolean canProcessRequest(Context context) {
        if (context == null) return true;
        SharedPreferences prefs = context.getSharedPreferences("ai_usage", Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastDate = prefs.getString("last_request_date", "");
        
        int count = prefs.getInt("request_count", 0);
        if (!today.equals(lastDate)) {
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
                            incrementKeyUsage();
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
                    
                    if (msg.contains("429") || msg.contains("quota") || msg.contains("limit")) {
                        updateKeyStatus("limit", "Quota limit reached at " + new Date());
                        if (retryCount < 3) {
                            fetchActiveKey(context, () -> chatWithRetry(context, prompt, userName, technicalMode, lang, callback, retryCount + 1), callback);
                        } else {
                            callback.onError("AI Quota Limit reached. Please try again later.");
                        }
                    } else if (msg.contains("API_KEY_INVALID") || msg.contains("invalid")) {
                        updateKeyStatus("blocked", "Invalid API Key: " + msg);
                        fetchActiveKey(context, () -> chatWithRetry(context, prompt, userName, technicalMode, lang, callback, retryCount + 1), callback);
                    } else if (msg.contains("503") || msg.contains("UNAVAILABLE") || msg.contains("demand")) {
                        if (retryCount < 3) {
                            chatWithRetry(context, prompt, userName, technicalMode, lang, callback, retryCount + 1);
                        } else {
                            callback.onError("AI Coach is very busy (High Demand). Please try again in a few moments.");
                        }
                    } else {
                        // Log the full error to logcat for developer
                        Log.e(TAG, "Critical AI Error: " + msg, t);
                        // Provide more descriptive fallback
                        if (msg.contains("Connection") || msg.contains("Unable to resolve host")) {
                            callback.onError("Network error. Please check your internet connection.");
                        } else {
                            callback.onError("AI Service Error: " + msg);
                        }
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

        if (currentKey == null) {
            fetchActiveKey(context, () -> executeAnalyzeImage(context, bitmap, userPrompt, userName, lang, callback), callback);
        } else {
            executeAnalyzeImage(context, bitmap, userPrompt, userName, lang, callback);
        }
    }

    private void executeAnalyzeImage(Context context, Bitmap bitmap, String userPrompt, String userName, String lang, AIResponseCallback callback) {
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
                            incrementKeyUsage();
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
                    Log.e(TAG, "Gemini Vision Error: " + msg, t);
                    
                    if (msg.contains("429") || msg.contains("quota")) {
                        updateKeyStatus("limit", "Vision quota reached");
                        fetchActiveKey(context, () -> executeAnalyzeImage(context, bitmap, userPrompt, userName, lang, callback), callback);
                    } else if (msg.contains("503") || msg.contains("UNAVAILABLE") || msg.contains("demand")) {
                        callback.onError("AI Server is very busy. Please try again later.");
                    } else if (msg.contains("Connection") || msg.contains("Unable to resolve host")) {
                        callback.onError("Network error. Please check your internet connection.");
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
