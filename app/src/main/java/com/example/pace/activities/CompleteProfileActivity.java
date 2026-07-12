package com.example.pace.activities;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pace.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CompleteProfileActivity extends AppCompatActivity {

    private TextInputEditText etName, etPhone, etWeight, etHeight, etMonthlyTarget, etDOB;
    private AutoCompleteTextView spinnerGender;
    private LinearLayout layoutStep1, layoutStep2;
    private Button btnAction, btnBack;
    private TextView tvStepTitle, tvStepSubtitle;
    private View dot1, dot2;

    private int currentStep = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);

        initUI();
    }

    private void initUI() {
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        etMonthlyTarget = findViewById(R.id.etMonthlyTarget);
        spinnerGender = findViewById(R.id.spinnerGender);
        etDOB = findViewById(R.id.etDOB);

        layoutStep1 = findViewById(R.id.layoutStep1);
        layoutStep2 = findViewById(R.id.layoutStep2);
        btnAction = findViewById(R.id.btnAction);
        btnBack = findViewById(R.id.btnBack);
        
        tvStepTitle = findViewById(R.id.tvStepTitle);
        tvStepSubtitle = findViewById(R.id.tvStepSubtitle);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);

        String[] genders = new String[]{"Laki-laki", "Perempuan"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        spinnerGender.setAdapter(adapter);

        etDOB.setOnClickListener(v -> showDatePicker(etDOB));

        btnAction.setOnClickListener(v -> {
            if (currentStep == 1) {
                if (validateStep1()) {
                    showStep2();
                }
            } else {
                if (validateStep2()) {
                    submitProfile();
                }
            }
        });

        btnBack.setOnClickListener(v -> showStep1());
        
        // Pre-fill name if available from Firebase (e.g. Google Sign In)
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getDisplayName() != null) {
            etName.setText(user.getDisplayName());
        }
    }

    private void showStep1() {
        currentStep = 1;
        layoutStep1.setVisibility(View.VISIBLE);
        layoutStep2.setVisibility(View.GONE);
        btnBack.setVisibility(View.GONE);
        btnAction.setText("Lanjut");
        
        tvStepTitle.setText("Data Diri");
        tvStepSubtitle.setText("Lengkapi informasi dasar Anda");
        
        dot1.setBackgroundResource(R.drawable.rounded_lime);
        dot2.setBackgroundResource(R.color.muted);
    }

    private void showStep2() {
        currentStep = 2;
        layoutStep1.setVisibility(View.GONE);
        layoutStep2.setVisibility(View.VISIBLE);
        btnBack.setVisibility(View.VISIBLE);
        btnAction.setText("Selesai");
        
        tvStepTitle.setText("Fisik & Target");
        tvStepSubtitle.setText("Data ini digunakan untuk kalori & progress");

        dot1.setBackgroundResource(R.color.muted);
        dot2.setBackgroundResource(R.drawable.rounded_lime);
    }

    private boolean validateStep1() {
        if (getText(etName).isEmpty()) { toast("Nama wajib diisi"); return false; }
        if (getText(etPhone).isEmpty()) { toast("Nomor telepon wajib diisi"); return false; }
        if (spinnerGender.getText().toString().isEmpty()) { toast("Jenis kelamin wajib dipilih"); return false; }
        if (getText(etDOB).isEmpty()) { toast("Tanggal lahir wajib diisi"); return false; }
        return true;
    }

    private boolean validateStep2() {
        if (getText(etWeight).isEmpty()) { toast("Berat badan wajib diisi"); return false; }
        if (getText(etHeight).isEmpty()) { toast("Tinggi badan wajib diisi"); return false; }
        if (getText(etMonthlyTarget).isEmpty()) { toast("Target bulanan wajib diisi"); return false; }
        return true;
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void submitProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", getText(etName));
        userData.put("phone", getText(etPhone));
        userData.put("gender", spinnerGender.getText().toString());
        userData.put("dob", getText(etDOB));
        userData.put("weight", getText(etWeight));
        userData.put("height", getText(etHeight));
        userData.put("monthly_target", Integer.parseInt(getText(etMonthlyTarget)));

        btnAction.setEnabled(false);
        btnAction.setText("Menyimpan...");

        FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
                .update(userData)
                .addOnSuccessListener(aVoid -> {
                    SharedPreferences.Editor editor = getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit();
                    editor.putInt("monthly_target", Integer.parseInt(getText(etMonthlyTarget)));
                    editor.putString("full_name", getText(etName));
                    editor.putString("dob", getText(etDOB));
                    editor.putInt("weight", (int) Double.parseDouble(getText(etWeight)));
                    editor.apply();

                    Toast.makeText(this, "Profil berhasil disimpan", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnAction.setEnabled(true);
                    btnAction.setText("Selesai");
                    Toast.makeText(this, "Gagal menyimpan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDatePicker(EditText et) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR) - 20;
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                R.style.CustomDatePickerDialog,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String date = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                    et.setText(date);
                }, year, month, day);
        datePickerDialog.show();
    }
}
