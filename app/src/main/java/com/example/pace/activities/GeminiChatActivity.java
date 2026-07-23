package com.example.pace.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pace.R;
import com.example.pace.adapter.ChatAdapter;
import com.example.pace.model.ChatMessage;
import com.example.pace.utils.GeminiAssistant;
import com.example.pace.utils.LocaleHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeminiChatActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    private RecyclerView rvChat;
    private ChatAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();
    private EditText etMessage;
    private ImageView btnSend, ivPreview;
    private RelativeLayout layoutImagePreview;
    private LinearLayout layoutAITypingIndicator, layoutEmptyState;
    private ChipGroup chipGroupSuggestions;
    private TextView tvGreeting;
    
    private GeminiAssistant ai;
    private Bitmap selectedBitmap;
    private String userGoal = "General Fitness";
    private String userName = "Runner";
    private String uid, currentSessionId;

    private static final long SESSION_TIMEOUT = 2 * 60 * 60 * 1000; // 2 hours

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gemini_chat);

        uid = FirebaseAuth.getInstance().getUid();
        ai = GeminiAssistant.getInstance();
        
        initViews();
        loadUserInfo();
        
        currentSessionId = getIntent().getStringExtra("SESSION_ID");
        if (currentSessionId != null) {
            loadSessionMessages(currentSessionId);
        } else {
            checkAndStartSession();
        }
        
        setupSuggestions();
    }

    private void loadUserInfo() {
        android.content.SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userGoal = prefs.getString("goal", "Health & Wellness");
        String fullName = prefs.getString("full_name", "Runner");
        userName = fullName.split(" ")[0];
        
        tvGreeting.setText(getString(R.string.where_start, userName));
    }

    private void initViews() {
        rvChat = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        ivPreview = findViewById(R.id.ivPreview);
        layoutImagePreview = findViewById(R.id.layoutImagePreview);
        layoutAITypingIndicator = findViewById(R.id.layoutAITypingIndicator);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        chipGroupSuggestions = findViewById(R.id.chipGroupSuggestions);
        tvGreeting = findViewById(R.id.tvGreeting);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRemoveImage).setOnClickListener(v -> removeImage());
        findViewById(R.id.btnHistory).setOnClickListener(v -> {
            startActivity(new Intent(this, ChatHistoryActivity.class));
        });

        rvChat.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(messages);
        rvChat.setAdapter(adapter);

        findViewById(R.id.btnPickImage).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 100);
        });

        btnSend.setOnClickListener(v -> sendMessage());
        
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void checkAndStartSession() {
        boolean forceNew = getIntent().getBooleanExtra("FORCE_NEW_CHAT", false);
        
        if (forceNew) {
            // New chat requested: do not create Firebase doc yet
            currentSessionId = null;
            messages.clear();
            adapter.notifyDataSetChanged();
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvChat.setVisibility(View.GONE);
            return;
        }

        android.content.SharedPreferences prefs = getSharedPreferences("chat_prefs", MODE_PRIVATE);
        long lastActive = prefs.getLong("last_active_" + uid, 0);
        String lastSessionId = prefs.getString("last_session_" + uid, null);

        if (lastSessionId != null && (System.currentTimeMillis() - lastActive < SESSION_TIMEOUT)) {
            currentSessionId = lastSessionId;
            loadSessionMessages(currentSessionId);
        } else {
            // New session, but wait for first message to save to Firebase
            currentSessionId = null;
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvChat.setVisibility(View.GONE);
        }
    }

    private void createSessionAndSend(String firstMsg, Bitmap image) {
        if (uid == null) return;
        
        Map<String, Object> session = new HashMap<>();
        // Use the first message as title
        session.put("title", firstMsg.length() > 30 ? firstMsg.substring(0, 27) + "..." : firstMsg);
        session.put("lastTimestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("chat_sessions")
                .add(session)
                .addOnSuccessListener(documentReference -> {
                    currentSessionId = documentReference.getId();
                    getSharedPreferences("chat_prefs", MODE_PRIVATE).edit()
                            .putString("last_session_" + uid, currentSessionId)
                            .apply();
                    
                    // Now save the pending message and its AI response
                    saveMessageToFirestore(firstMsg, true);
                    // The AI response will be saved in onAiResponse which calls saveMessageToFirestore
                    callAI(firstMsg, image);
                });
    }

    private void loadSessionMessages(String sessionId) {
        if (uid == null || sessionId == null) return;

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("chat_sessions").document(sessionId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        layoutEmptyState.setVisibility(View.GONE);
                        rvChat.setVisibility(View.VISIBLE);
                        messages.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String text = doc.getString("text");
                            Boolean isUser = doc.getBoolean("user");
                            if (text != null && isUser != null) {
                                messages.add(new ChatMessage(text, isUser));
                            }
                        }
                        adapter.notifyDataSetChanged();
                        rvChat.scrollToPosition(messages.size() - 1);
                    }
                });
    }

    private void saveMessageToFirestore(String text, boolean isUser) {
        if (uid == null || currentSessionId == null) return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("text", text);
        msg.put("user", isUser);
        msg.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference sessionRef = db.collection("users").document(uid)
                .collection("chat_sessions").document(currentSessionId);

        sessionRef.collection("messages").add(msg);
        
        // Update session title on first user message if it's still default
        if (isUser && messages.size() <= 1) {
            sessionRef.update("title", text.length() > 30 ? text.substring(0, 27) + "..." : text);
        }
        sessionRef.update("lastTimestamp", System.currentTimeMillis());
        
        // Update last activity
        getSharedPreferences("chat_prefs", MODE_PRIVATE).edit()
                .putLong("last_active_" + uid, System.currentTimeMillis())
                .apply();
    }

    private void setupSuggestions() {
        String[] suggestions = {
            getString(R.string.ai_suggestion_1),
            getString(R.string.ai_suggestion_2),
            getString(R.string.ai_suggestion_3),
            getString(R.string.ai_suggestion_4),
            getString(R.string.ai_suggestion_5)
        };

        for (String s : suggestions) {
            Chip chip = new Chip(this);
            chip.setText(s);
            chip.setChipBackgroundColorResource(R.color.card);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            chip.setChipStrokeColorResource(R.color.muted);
            chip.setChipStrokeWidth(2f);
            chip.setOnClickListener(v -> {
                etMessage.setText(s);
                sendMessage();
            });
            chipGroupSuggestions.addView(chip);
        }
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty() && selectedBitmap == null) return;

        if (layoutEmptyState.getVisibility() == View.VISIBLE) {
            layoutEmptyState.setVisibility(View.GONE);
            rvChat.setVisibility(View.VISIBLE);
        }

        Bitmap currentBitmap = null;
        if (selectedBitmap != null) {
            currentBitmap = resizeBitmap(selectedBitmap, 800);
        }
        
        final Bitmap finalBitmap = currentBitmap;
        addMessage(text, true, finalBitmap);
        
        etMessage.setText("");
        removeImage();
        setTyping(true);

        if (currentSessionId == null) {
            // Create session in Firebase ONLY on first message
            createSessionAndSend(text, finalBitmap);
        } else {
            saveMessageToFirestore(text, true);
            callAI(text, finalBitmap);
        }
    }

    private void callAI(String text, Bitmap image) {
        String lang = LocaleHelper.getLanguage(this);
        if (image != null) {
            ai.analyzeImage(this, image, text, userName, lang, new GeminiAssistant.AIResponseCallback() {
                @Override
                public void onSuccess(String response) {
                    onAiResponse(response);
                }
                @Override
                public void onError(String errorMsg) {
                    onAiError(errorMsg);
                }
            });
        } else {
            ai.chat(this, text, userName, false, lang, new GeminiAssistant.AIResponseCallback() {
                @Override
                public void onSuccess(String response) {
                    onAiResponse(response);
                }
                @Override
                public void onError(String errorMsg) {
                    onAiError(errorMsg);
                }
            });
        }
    }

    private Bitmap resizeBitmap(Bitmap original, int maxSize) {
        int width = original.getWidth();
        int height = original.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(original, width, height, true);
    }

    private void onAiResponse(String response) {
        runOnUiThread(() -> {
            setTyping(false);
            addMessage(response, false, null);
            saveMessageToFirestore(response, false);
        });
    }

    private void onAiError(String error) {
        runOnUiThread(() -> {
            setTyping(false);
            addMessage(getString(R.string.ai_error_prefix, error), false, null);
        });
    }

    private void setTyping(boolean typing) {
        layoutAITypingIndicator.setVisibility(typing ? View.VISIBLE : View.GONE);
        if (typing) {
            rvChat.postDelayed(() -> rvChat.smoothScrollToPosition(messages.size() - 1), 100);
        }
    }

    private void addMessage(String text, boolean isUser, Bitmap image) {
        messages.add(new ChatMessage(text, isUser, image));
        int pos = messages.size() - 1;
        adapter.notifyItemInserted(pos);
        rvChat.postDelayed(() -> rvChat.smoothScrollToPosition(pos), 100);
    }

    private void removeImage() {
        selectedBitmap = null;
        layoutImagePreview.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                InputStream is = getContentResolver().openInputStream(imageUri);
                selectedBitmap = BitmapFactory.decodeStream(is);
                ivPreview.setImageBitmap(selectedBitmap);
                layoutImagePreview.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
