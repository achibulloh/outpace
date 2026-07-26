package com.example.pace.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pace.R;
import com.example.pace.adapter.HelpTicketAdapter;
import com.example.pace.model.HelpTicket;
import com.example.pace.utils.LocaleHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HelpHistoryActivity extends AppCompatActivity {

    private RecyclerView rvHelpHistory;
    private HelpTicketAdapter adapter;
    private List<HelpTicket> ticketList = new ArrayList<>();
    private LinearLayout layoutEmpty;
    private ProgressBar pbLoading;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_history);

        rvHelpHistory = findViewById(R.id.rvHelpHistory);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        pbLoading = findViewById(R.id.pbLoading);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvHelpHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HelpTicketAdapter(ticketList);
        rvHelpHistory.setAdapter(adapter);

        loadTickets();
    }

    private void loadTickets() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        pbLoading.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance().collection("help_tickets")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    pbLoading.setVisibility(View.GONE);
                    ticketList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        HelpTicket ticket = doc.toObject(HelpTicket.class);
                        if (ticket != null) {
                            ticketList.add(ticket);
                        }
                    }

                    // Sort in memory to avoid needing a composite index in Firestore
                    ticketList.sort((t1, r2) -> Long.compare(r2.getTimestamp(), t1.getTimestamp()));

                    if (ticketList.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvHelpHistory.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvHelpHistory.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    pbLoading.setVisibility(View.GONE);
                    layoutEmpty.setVisibility(View.VISIBLE);
                });
    }
}
