package com.example.pace.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pace.R;
import com.example.pace.adapter.LeaderboardAdapter;
import com.example.pace.model.LeaderboardUser;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView rvRankings = findViewById(R.id.rvRankings);
        rvRankings.setLayoutManager(new LinearLayoutManager(this));

        List<LeaderboardUser> users = new ArrayList<>();
        // Mock data matching the photo
        users.add(new LeaderboardUser("#1", "Budi S.", "Best pace: 4:48/km", "52.4 km", R.drawable.ic_trophy, false));
        users.add(new LeaderboardUser("#2", "Ari W.", "Best pace: 4:55/km", "48.1 km", R.drawable.ic_trophy, false));
        users.add(new LeaderboardUser("#3", "Citra M.", "Best pace: 5:02/km", "45.7 km", R.drawable.ic_trophy, false));
        users.add(new LeaderboardUser("#4", "Kamu", "Best pace: 5:14/km", "36.2 km", R.drawable.ic_logo, true));
        users.add(new LeaderboardUser("#5", "Dina K.", "Best pace: 5:19/km", "34.8 km", R.drawable.ic_person, false));

        LeaderboardAdapter adapter = new LeaderboardAdapter(users);
        rvRankings.setAdapter(adapter);
    }
}
