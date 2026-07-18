package com.example.pace.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.pace.R;
import com.example.pace.activities.EditProfileActivity;
import com.example.pace.activities.HelpActivity;
import com.example.pace.activities.LoginActivity;
import com.example.pace.activities.PrivacyActivity;
import com.example.pace.activities.SettingsActivity;
import com.example.pace.database.AppDatabase;
import com.example.pace.model.RunRecord;
import com.example.pace.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        loadUserData(view);
        loadStats(view);

        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), EditProfileActivity.class));
        });

        view.findViewById(R.id.btnSettings).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        });

        view.findViewById(R.id.btnPrivacy).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), PrivacyActivity.class));
        });

        view.findViewById(R.id.btnHelp).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), HelpActivity.class));
        });

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            Context context = getContext();
            if (context == null) return;
            
            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut();
            
            // Also sign out from Google to avoid "auto-login" or crashes on next attempt
            try {
                com.google.android.gms.auth.api.signin.GoogleSignInOptions gso = new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();
                com.google.android.gms.auth.api.signin.GoogleSignInClient mGoogleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso);
                mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                    if (isAdded()) {
                        startActivity(new Intent(getActivity(), LoginActivity.class));
                        if (getActivity() != null) getActivity().finish();
                    }
                });
            } catch (Exception e) {
                // Fallback if Google client fails
                startActivity(new Intent(getActivity(), LoginActivity.class));
                if (getActivity() != null) getActivity().finish();
            }
        });

        return view;
    }

    private void loadUserData(View view) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            String email = currentUser.getEmail();
            
            TextView tvUserName = view.findViewById(R.id.tvUserName);
            TextView tvUserEmail = view.findViewById(R.id.tvUserEmail);
            
            tvUserEmail.setText(email);

            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (isAdded() && getActivity() != null) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                tvUserName.setText(user.getName() != null ? user.getName() : currentUser.getDisplayName());
                                updateBMICard(view, user);
                            }
                        }
                    });
        }
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
                if (bmi < 18.5) { category = "Underweight"; color = Color.YELLOW; }
                else if (bmi < 25) { category = "Normal"; color = ContextCompat.getColor(getContext(), R.color.lime); }
                else if (bmi < 30) { category = "Overweight"; color = Color.parseColor("#FFA500"); }
                else { category = "Obese"; color = Color.RED; }
                
                tvCat.setText(category);
                tvCat.setTextColor(color);
                if (tvCat.getBackground() != null) {
                    tvCat.getBackground().setTint(Color.argb(40, Color.red(color), Color.green(color), Color.blue(color)));
                }
                
                String target = user.getTargetWeight();
                if (target != null && !target.equals("0") && !target.isEmpty()) {
                    tvGoal.setText("Goal: " + target + " kg");
                    tvGoal.setVisibility(View.VISIBLE);
                } else {
                    tvGoal.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            if (v.findViewById(R.id.cvBMI) != null) v.findViewById(R.id.cvBMI).setVisibility(View.GONE);
        }
    }

    private void loadStats(View view) {
        new Thread(() -> {
            Context context = getContext();
            if (context == null || !isAdded()) return;
            
            try {
                List<RunRecord> records = AppDatabase.getInstance(context).runDao().getAllRuns();
                double totalDist = 0;
                int totalCal = 0;
                
                for (RunRecord r : records) {
                    totalDist += r.getDistance();
                    totalCal += r.getCalories();
                }

                final double fDist = totalDist;
                final int fCal = totalCal;
                final int count = records.size();

                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        ((TextView)view.findViewById(R.id.tvTotalRunsValue)).setText(String.valueOf(count));
                        ((TextView)view.findViewById(R.id.tvTotalDistanceValue)).setText(String.format(Locale.getDefault(), "%.1f", fDist));
                        ((TextView)view.findViewById(R.id.tvTotalCaloriesValue)).setText(fCal > 1000 ? String.format(Locale.getDefault(), "%.1fk", fCal/1000.0) : String.valueOf(fCal));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
