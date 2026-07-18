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
import com.example.pace.utils.LocaleHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CompleteProfileActivity extends AppCompatActivity {

    private TextInputEditText etName, etPhone, etWeight, etHeight, etMonthlyTarget, etDOB, etTargetWeight;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
    private AutoCompleteTextView spinnerGender, spinnerGoal, spinnerFitness;
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
        etTargetWeight = findViewById(R.id.etTargetWeight);
        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerGoal = findViewById(R.id.spinnerGoal);
        spinnerFitness = findViewById(R.id.spinnerFitness);
        etDOB = findViewById(R.id.etDOB);

        layoutStep1 = findViewById(R.id.layoutStep1);
        layoutStep2 = findViewById(R.id.layoutStep2);
        btnAction = findViewById(R.id.btnAction);
        btnBack = findViewById(R.id.btnBack);
        
        tvStepTitle = findViewById(R.id.tvStepTitle);
        tvStepSubtitle = findViewById(R.id.tvStepSubtitle);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);

        String[] genders = new String[]{getString(R.string.gender_male), getString(R.string.gender_female)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        spinnerGender.setAdapter(adapter);

        String[] goals = new String[]{"Weight Loss", "Increase Speed", "Health & Wellness", "Marathon Training"};
        ArrayAdapter<String> goalAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, goals);
        spinnerGoal.setAdapter(goalAdapter);
        spinnerGoal.setText(goals[2], false); 

        String[] fitnessLevels = new String[]{"Beginner", "Intermediate", "Pro"};
        ArrayAdapter<String> fitnessAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, fitnessLevels);
        spinnerFitness.setAdapter(fitnessAdapter);
        spinnerFitness.setText(fitnessLevels[0], false);

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
        btnAction.setText(R.string.btn_next); 
        
        tvStepTitle.setText(R.string.step_personal_info);
        tvStepSubtitle.setText(R.string.step_personal_sub);
        
        dot1.setBackgroundResource(R.drawable.rounded_lime);
        dot2.setBackgroundResource(R.color.muted);
    }

    private void showStep2() {
        currentStep = 2;
        layoutStep1.setVisibility(View.GONE);
        layoutStep2.setVisibility(View.VISIBLE);
        btnBack.setVisibility(View.VISIBLE);
        btnAction.setText(R.string.btn_finish);
        
        tvStepTitle.setText(R.string.step_physical_target);
        tvStepSubtitle.setText(R.string.step_physical_sub);

        dot1.setBackgroundResource(R.color.muted);
        dot2.setBackgroundResource(R.drawable.rounded_lime);
    }

    private boolean validateStep1() {
        if (getText(etName).isEmpty()) { toast(getString(R.string.name_empty_error)); return false; }
        if (getText(etPhone).isEmpty()) { toast(getString(R.string.all_fields_mandatory)); return false; }
        if (spinnerGender.getText().toString().isEmpty()) { toast(getString(R.string.all_fields_mandatory)); return false; }
        if (getText(etDOB).isEmpty()) { toast(getString(R.string.all_fields_mandatory)); return false; }
        return true;
    }

    private boolean validateStep2() {
        if (getText(etWeight).isEmpty()) { toast(getString(R.string.all_fields_mandatory)); return false; }
        if (getText(etHeight).isEmpty()) { toast(getString(R.string.all_fields_mandatory)); return false; }
        if (getText(etMonthlyTarget).isEmpty()) { toast(getString(R.string.all_fields_mandatory)); return false; }
        if (getText(etTargetWeight).isEmpty()) { toast(getString(R.string.all_fields_mandatory)); return false; }
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
        userData.put("goal", spinnerGoal.getText().toString());
        userData.put("targetWeight", getText(etTargetWeight));
        
        int fitnessIdx = 1;
        String fitTxt = spinnerFitness.getText().toString();
        if (fitTxt.equals("Intermediate")) fitnessIdx = 2;
        else if (fitTxt.equals("Pro")) fitnessIdx = 3;
        userData.put("fitnessLevel", fitnessIdx);
        userData.put("monthly_target", Integer.parseInt(getText(etMonthlyTarget)));

        btnAction.setEnabled(false);
        btnAction.setText(R.string.saving);

        final int finalFitnessIdx = fitnessIdx;
        FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
                .update(userData)
                .addOnSuccessListener(aVoid -> {
                    SharedPreferences.Editor editor = getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit();
                    editor.putInt("monthly_target", Integer.parseInt(getText(etMonthlyTarget)));
                    editor.putString("full_name", getText(etName));
                    editor.putString("dob", getText(etDOB));
                    editor.putString("goal", spinnerGoal.getText().toString());
                    editor.putString("targetWeight", getText(etTargetWeight));
                    editor.putInt("fitnessLevel", finalFitnessIdx);
                    editor.putInt("weight", (int) Double.parseDouble(getText(etWeight)));
                    editor.apply();

                    Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnAction.setEnabled(true);
                    btnAction.setText(R.string.btn_finish);
                    Toast.makeText(this, getString(R.string.profile_update_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
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
