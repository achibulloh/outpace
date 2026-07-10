package com.example.pace.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.pace.R;
import java.util.Calendar;

public class EditProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Setup Gender Dropdown
        String[] genders = new String[]{"Laki-laki", "Perempuan"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        AutoCompleteTextView spinnerGender = findViewById(R.id.spinnerGender);
        spinnerGender.setAdapter(adapter);

        // Setup Date of Birth Picker
        EditText etDOB = findViewById(R.id.etDOB);
        etDOB.setOnClickListener(v -> showDatePicker(etDOB));

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show();
            finish();
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
