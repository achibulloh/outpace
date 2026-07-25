package com.example.pace.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.pace.R;
import com.example.pace.activities.EditProfileActivity;
import com.example.pace.activities.HelpActivity;
import com.example.pace.activities.LoginActivity;
import com.example.pace.activities.PrivacyActivity;
import com.example.pace.activities.SettingsActivity;
import com.example.pace.model.RunRecord;
import com.example.pace.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Locale;

public class ProfileFragment extends Fragment {

    private ListenerRegistration userListener, statsListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        setupClickListeners(view);
        startRealtimeUpdates(view);

        return view;
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> startActivity(new Intent(getActivity(), EditProfileActivity.class)));
        view.findViewById(R.id.btnSettings).setOnClickListener(v -> startActivity(new Intent(getActivity(), SettingsActivity.class)));
        view.findViewById(R.id.btnPrivacy).setOnClickListener(v -> startActivity(new Intent(getActivity(), PrivacyActivity.class)));
        view.findViewById(R.id.btnHelp).setOnClickListener(v -> startActivity(new Intent(getActivity(), HelpActivity.class)));
        
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) getActivity().finish();
        });
    }

    private void startRealtimeUpdates(View view) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String uid = currentUser.getUid();

        // 1. Listen to User Profile & BMI data
        userListener = FirebaseFirestore.getInstance().collection("users").document(uid)
                .addSnapshotListener((doc, error) -> {
                    if (error != null || doc == null || !doc.exists()) return;
                    User user = doc.toObject(User.class);
                    if (user != null && isAdded()) {
                        ((TextView) view.findViewById(R.id.tvUserName)).setText(user.getName());
                        ((TextView) view.findViewById(R.id.tvUserEmail)).setText(user.getEmail());
                        updateBMICard(view, user);
                    }
                });

        // 2. Listen to ALL Runs to calculate Total Stats (Real-time)
        statsListener = FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("runs")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null || !isAdded()) return;

                    double totalDist = 0;
                    int totalCal = 0;
                    int count = snapshots.size();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        RunRecord r = doc.toObject(RunRecord.class);
                        if (r != null) {
                            totalDist += r.getDistance();
                            totalCal += r.getCalories();
                        }
                    }

                    TextView tvRuns = view.findViewById(R.id.tvTotalRunsValue);
                    TextView tvDist = view.findViewById(R.id.tvTotalDistanceValue);
                    TextView tvCal = view.findViewById(R.id.tvTotalCaloriesValue);

                    if (tvRuns != null) tvRuns.setText(String.valueOf(count));
                    if (tvDist != null) tvDist.setText(String.format(Locale.getDefault(), "%.1f", totalDist));
                    if (tvCal != null) {
                        tvCal.setText(totalCal > 1000 ? String.format(Locale.getDefault(), "%.1fk", totalCal / 1000.0) : String.valueOf(totalCal));
                    }
                });
    }

    private void updateBMICard(View v, User user) {
        if (!isAdded() || getContext() == null) return;
        try {
            double weight = Double.parseDouble(user.getWeight());
            double heightCm = Double.parseDouble(user.getHeight());
            double heightM = heightCm / 100.0;
            
            if (heightM > 0) {
                double bmi = weight / (heightM * heightM);
                TextView tvBMI = v.findViewById(R.id.tvBMIValue);
                TextView tvCat = v.findViewById(R.id.tvBMICategory);
                TextView tvGoal = v.findViewById(R.id.tvWeightGoal);
                
                tvBMI.setText(String.format(Locale.getDefault(), "%.1f", bmi));
                
                String category;
                int color;
                if (bmi < 18.5) { category = getString(R.string.bmi_underweight); color = Color.YELLOW; }
                else if (bmi < 25) { category = getString(R.string.bmi_normal); color = ContextCompat.getColor(getContext(), R.color.lime); }
                else if (bmi < 30) { category = getString(R.string.bmi_overweight); color = Color.parseColor("#FFA500"); }
                else { category = getString(R.string.bmi_obese); color = Color.RED; }
                
                tvCat.setText(category);
                tvCat.setTextColor(color);
                if (tvCat.getBackground() != null) {
                    tvCat.getBackground().setTint(Color.argb(40, Color.red(color), Color.green(color), Color.blue(color)));
                }
                
                String target = user.getTargetWeight();
                if (target != null && !target.equals("0") && !target.isEmpty()) {
                    tvGoal.setText(getString(R.string.bmi_goal_prefix, target));
                    tvGoal.setVisibility(View.VISIBLE);
                } else {
                    tvGoal.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            if (v.findViewById(R.id.cvBMI) != null) v.findViewById(R.id.cvBMI).setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) userListener.remove();
        if (statsListener != null) statsListener.remove();
    }
}
