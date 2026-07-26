package com.example.pace.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pace.R;
import com.example.pace.model.HelpTicket;
import com.example.pace.utils.LocaleHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.UUID;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        findViewById(R.id.btnShowForm).setOnClickListener(v -> showReportIssueDialog());

        findViewById(R.id.btnHelpHistory).setOnClickListener(v -> {
            startActivity(new Intent(this, HelpHistoryActivity.class));
        });
    }

    private void showReportIssueDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_report_issue, null, false);
        
        Spinner spinnerCategory = view.findViewById(R.id.spinnerCategory);
        View cvOtherCategory = view.findViewById(R.id.cvOtherCategory);
        EditText etOtherCategory = view.findViewById(R.id.etOtherCategory);
        EditText etDescription = view.findViewById(R.id.etDescription);
        View btnSubmit = view.findViewById(R.id.btnSubmitTicket);
        View btnClose = view.findViewById(R.id.btnCloseDialog);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Setup Spinner
        String[] categories = {
                getString(R.string.cat_account),
                getString(R.string.cat_bug),
                getString(R.string.cat_feature),
                getString(R.string.cat_other)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (categories[position].equals(getString(R.string.cat_other))) {
                    cvOtherCategory.setVisibility(View.VISIBLE);
                } else {
                    cvOtherCategory.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSubmit.setOnClickListener(v -> {
            String category = spinnerCategory.getSelectedItem().toString();
            String otherDetail = etOtherCategory.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (category.equals(getString(R.string.cat_other))) {
                if (otherDetail.isEmpty()) {
                    Toast.makeText(this, R.string.error_specify_category, Toast.LENGTH_SHORT).show();
                    return;
                }
                category = getString(R.string.cat_other) + ": " + otherDetail;
            }

            submitTicket(category, description, dialog);
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void submitTicket(String category, String desc, BottomSheetDialog dialog) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (desc.isEmpty()) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        String priority = "Low";
        if (category.contains(getString(R.string.cat_bug))) priority = "High";
        else if (category.contains(getString(R.string.cat_account))) priority = "Medium";

        String ticketId = UUID.randomUUID().toString();
        HelpTicket ticket = new HelpTicket(
                ticketId,
                user.getUid(),
                user.getEmail(),
                category,
                desc,
                "Open",
                priority,
                System.currentTimeMillis()
        );

        FirebaseFirestore.getInstance().collection("help_tickets").document(ticketId)
                .set(ticket)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.ticket_submitted, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
