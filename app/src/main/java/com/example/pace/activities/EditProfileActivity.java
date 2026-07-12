package com.example.pace.activities;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.pace.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {
    
    private TextInputEditText etName, etEmail, etPhone, etWeight, etHeight, etMonthlyTarget;
    private AutoCompleteTextView spinnerGender;
    private TextInputEditText etDOB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initUI();
        loadUserData();

        findViewById(R.id.btnSave).setOnClickListener(v -> saveProfile());
    }

    private void initUI() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        etMonthlyTarget = findViewById(R.id.etMonthlyTarget);
        spinnerGender = findViewById(R.id.spinnerGender);
        etDOB = findViewById(R.id.etDOB);

        // Setup Gender Dropdown
        String[] genders = new String[]{"Laki-laki", "Perempuan"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        spinnerGender.setAdapter(adapter);

        // Setup Date of Birth Picker
        etDOB.setOnClickListener(v -> showDatePicker(etDOB));
    }

    private void loadUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            etEmail.setText(currentUser.getEmail());
            etEmail.setEnabled(false); // Email usually not editable directly

            FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            etName.setText(documentSnapshot.getString("name"));
                            etPhone.setText(documentSnapshot.getString("phone"));
                            etWeight.setText(documentSnapshot.getString("weight"));
                            etHeight.setText(documentSnapshot.getString("height"));
                            etDOB.setText(documentSnapshot.getString("dob"));
                            spinnerGender.setText(documentSnapshot.getString("gender"), false);
                            
                            // Load monthly target
                            Object targetObj = documentSnapshot.get("monthly_target");
                            int monthlyTarget = 50;
                            if (targetObj != null) {
                                try {
                                    if (targetObj instanceof Number) {
                                        monthlyTarget = ((Number) targetObj).intValue();
                                    } else {
                                        monthlyTarget = (int) Double.parseDouble(String.valueOf(targetObj));
                                    }
                                } catch (Exception ignored) {}
                            }
                            etMonthlyTarget.setText(String.valueOf(monthlyTarget));
                        } else {
                            etMonthlyTarget.setText("50");
                        }
                    })
                    .addOnFailureListener(e -> etMonthlyTarget.setText("50"));
        }
    }

    private void saveProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String weight = etWeight.getText().toString().trim();
        String height = etHeight.getText().toString().trim();
        String monthlyTargetStr = etMonthlyTarget.getText().toString().trim();
        String dob = etDOB.getText().toString().trim();
        String gender = spinnerGender.getText().toString();

        int monthlyTargetInt = 50;
        if (!monthlyTargetStr.isEmpty()) {
            try {
                monthlyTargetInt = Integer.parseInt(monthlyTargetStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Target bulanan harus berupa angka", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (name.isEmpty()) {
            Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("phone", phone);
        userData.put("weight", weight);
        userData.put("height", height);
        userData.put("monthly_target", monthlyTargetInt); // Simpan sebagai Number, bukan String
        userData.put("dob", dob);
        userData.put("gender", gender);

        int finalMonthlyTargetInt = monthlyTargetInt;
        FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
                .update(userData)
                .addOnSuccessListener(aVoid -> {
                    // Update SharedPreferences
                    SharedPreferences.Editor editor = getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit();
                    editor.putInt("monthly_target", finalMonthlyTargetInt);
                    editor.apply();

                    Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal memperbarui profil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDatePicker(EditText et) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String date = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                    et.setText(date);
                }, year, month, day);
        datePickerDialog.show();
    }
}
