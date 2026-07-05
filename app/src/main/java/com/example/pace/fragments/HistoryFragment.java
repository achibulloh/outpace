package com.example.pace.fragments;
import android.os.Bundle;
import android.view.*;
import androidx.fragment.app.Fragment;
import com.example.pace.R;
public class HistoryFragment extends Fragment {
    @Override public View onCreateView(LayoutInflater i, ViewGroup c, Bundle b) {
        return i.inflate(R.layout.fragment_home, c, false); // pakai home dulu
    }
}