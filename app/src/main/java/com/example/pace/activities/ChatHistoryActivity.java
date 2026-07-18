package com.example.pace.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pace.R;
import com.example.pace.model.ChatSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatHistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private final List<ChatSession> sessions = new ArrayList<>();
    private SessionAdapter adapter;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_history);

        uid = FirebaseAuth.getInstance().getUid();
        rvHistory = findViewById(R.id.rvHistory);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        findViewById(R.id.btnNewChat).setOnClickListener(v -> {
            Intent intent = new Intent(this, GeminiChatActivity.class);
            intent.putExtra("FORCE_NEW_CHAT", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SessionAdapter(sessions);
        rvHistory.setAdapter(adapter);

        loadSessions();
    }

    private void loadSessions() {
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("chat_sessions")
                .orderBy("lastTimestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    sessions.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        ChatSession s = doc.toObject(ChatSession.class);
                        if (s != null) {
                            s.setId(doc.getId());
                            sessions.add(s);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void deleteSession(int position) {
        if (uid == null) return;
        ChatSession session = sessions.get(position);
        
        new AlertDialog.Builder(this)
                .setTitle("Delete Chat?")
                .setMessage("This will permanently remove this conversation.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                            .collection("chat_sessions").document(session.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                sessions.remove(position);
                                adapter.notifyItemRemoved(position);
                                Toast.makeText(this, "Chat deleted", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.ViewHolder> {
        private final List<ChatSession> list;
        private final SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault());

        public SessionAdapter(List<ChatSession> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_session, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChatSession s = list.get(position);
            holder.tvTitle.setText(s.getTitle());
            holder.tvDate.setText(sdf.format(new Date(s.getLastTimestamp())));
            
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ChatHistoryActivity.this, GeminiChatActivity.class);
                intent.putExtra("SESSION_ID", s.getId());
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });

            holder.btnDelete.setOnClickListener(v -> deleteSession(position));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDate;
            View btnDelete;
            ViewHolder(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvSessionTitle);
                tvDate = v.findViewById(R.id.tvSessionDate);
                btnDelete = v.findViewById(R.id.btnDelete);
            }
        }
    }
}
