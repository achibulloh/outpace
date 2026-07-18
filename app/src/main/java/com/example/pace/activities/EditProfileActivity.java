package com.example.pace.activities;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.pace.R;
import com.example.pace.model.User;
import com.example.pace.utils.LocaleHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPhone, etWeight, etHeight, etMonthlyTarget, etTargetWeight, etDOB;
    private AutoCompleteTextView spinnerGender, spinnerGoal, spinnerFitness;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        initUI();
        loadUserData();
    }

    private void initUI() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(v -> finish());

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        etMonthlyTarget = findViewById(R.id.etMonthlyTarget);
        etTargetWeight = findViewById(R.id.etTargetWeight);
        etDOB = findViewById(R.id.etDOB);
        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerGoal = findViewById(R.id.spinnerGoal);
        spinnerFitness = findViewById(R.id.spinnerFitness);

        String[] genders = {getString(R.string.gender_male), getString(R.string.gender_female)};
        spinnerGender.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders));

        String[] goals = {"Weight Loss", "Increase Speed", "Health & Wellness", "Marathon Training"};
        spinnerGoal.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, goals));

        String[] fitness = {"Beginner", "Intermediate", "Pro"};
        spinnerFitness.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, fitness));

        etDOB.setOnClickListener(v -> showDatePicker(etDOB));
        findViewById(R.id.btnSave).setOnClickListener(v -> saveProfile());
    }

    private void loadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    User u = doc.toObject(User.class);
                    if (u != null) {
                        etName.setText(u.getName());
                        etEmail.setText(u.getEmail());
                        etEmail.setEnabled(false); // Email cannot be changed
                        etPhone.setText(u.getPhone());
                        etWeight.setText(u.getWeight());
                        etHeight.setText(u.getHeight());
                        etMonthlyTarget.setText(u.getMonthlyTarget());
                        etTargetWeight.setText(u.getTargetWeight());
                        etDOB.setText(u.getDob());
                        spinnerGender.setText(u.getGender(), false);
                        spinnerGoal.setText(u.getGoal(), false);
                        
                        String fLevel = "Beginner";
                        if (u.getFitnessLevel() == 2) fLevel = "Intermediate";
                        else if (u.getFitnessLevel() == 3) fLevel = "Pro";
                        spinnerFitness.setText(fLevel, false);
                    }
                });
    }

    private void saveProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("name", etName.getText().toString());
        data.put("phone", etPhone.getText().toString());
        data.put("weight", etWeight.getText().toString());
        data.put("height", etHeight.getText().toString());
        data.put("monthly_target", etMonthlyTarget.getText().toString());
        data.put("targetWeight", etTargetWeight.getText().toString());
        data.put("dob", etDOB.getText().toString());
        data.put("gender", spinnerGender.getText().toString());
        data.put("goal", spinnerGoal.getText().toString());
        
        int fIdx = 1;
        String fTxt = spinnerFitness.getText().toString();
        if (fTxt.equals("Intermediate")) fIdx = 2;
        else if (fTxt.equals("Pro")) fIdx = 3;
        data.put("fitnessLevel", fIdx);

        final int finalFIdx = fIdx;
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).update(data)
                .addOnSuccessListener(aVoid -> {
                    SharedPreferences.Editor editor = getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit();
                    editor.putString("full_name", etName.getText().toString());
                    editor.putString("goal", spinnerGoal.getText().toString());
                    editor.putInt("fitnessLevel", finalFIdx);
                    editor.putInt("weight", (int) Double.parseDouble(etWeight.getText().toString()));
                    editor.apply();
                    
                    Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showDatePicker(EditText et) {
        final Calendar c = Calendar.getInstance();
        DatePickerDialog dpd = new DatePickerDialog(this, R.style.CustomDatePickerDialog, (view, year, month, day) -> {
            et.setText(day + "/" + (month + 1) + "/" + year);
        }, c.get(Calendar.YEAR) - 20, c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dpd.show();
    }
}
