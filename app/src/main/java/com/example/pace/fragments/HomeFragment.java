package com.example.pace.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import com.example.pace.R;
import com.example.pace.activities.MainActivity;

public class HomeFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        Button btnStart = view.findViewById(R.id.btnStartRun);
        btnStart.setOnClickListener(v -> {
            // Pindah ke tab Run
            ((MainActivity) requireActivity())
                    .getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new RunFragment()).commit();
        });

        return view;
    }
}