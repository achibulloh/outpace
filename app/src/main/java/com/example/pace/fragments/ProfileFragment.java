package com.example.pace.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.pace.R;
import com.example.pace.activities.EditProfileActivity;
import com.example.pace.activities.HelpActivity;
import com.example.pace.activities.LoginActivity;
import com.example.pace.activities.PrivacyActivity;
import com.example.pace.activities.SettingsActivity;

public class ProfileFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        loadUserData(view);

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
            SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
            
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            Toast.makeText(getActivity(), "Logout Berhasil", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void loadUserData(View view) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String name = prefs.getString("full_name", "User Name");
        String email = prefs.getString("email", "user@email.com");

        ((TextView) view.findViewById(R.id.tvUserName)).setText(name);
        ((TextView) view.findViewById(R.id.tvUserEmail)).setText(email);
    }
}
