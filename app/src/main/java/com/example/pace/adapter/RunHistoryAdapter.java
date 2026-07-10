package com.example.pace.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pace.R;
import com.example.pace.activities.ActivityDetailActivity;
import com.example.pace.model.RunRecord;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RunHistoryAdapter extends RecyclerView.Adapter<RunHistoryAdapter.ViewHolder> {
    private List<RunRecord> runList;

    public RunHistoryAdapter(List<RunRecord> runList) {
        this.runList = runList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_run_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RunRecord run = runList.get(position);
        
        holder.tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", run.getDistance()));
        
        int mins = (int) (run.getDuration() / 60);
        int secs = (int) (run.getDuration() % 60);
        holder.tvDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
        
        int paceMins = (int) run.getPace();
        int paceSecs = (int) ((run.getPace() - paceMins) * 60);
        holder.tvPace.setText(String.format(Locale.getDefault(), "%d:%02d/km", paceMins, paceSecs));
        
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMM · HH:mm", new Locale("id", "ID"));
        String dateStr = sdf.format(new Date(run.getTimestamp()));
        if (run.getLocationName() != null) {
            String loc = run.getLocationName().replace(" - ", "\n");
            dateStr += "\n" + loc;
        }
        holder.tvDate.setText(dateStr);
        
        // Logic for title based on time of day
        int hour = Integer.parseInt(new SimpleDateFormat("H", Locale.getDefault()).format(new Date(run.getTimestamp())));
        String title = "Run";
        if (hour >= 5 && hour < 11) title = "Morning Run";
        else if (hour >= 11 && hour < 15) title = "Afternoon Run";
        else if (hour >= 15 && hour < 19) title = "Evening Run";
        else title = "Night Run";
        
        holder.tvTitle.setText(title);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ActivityDetailActivity.class);
            intent.putExtra("RUN_ID", run.getId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return runList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvDistance, tvDuration, tvPace;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvRunTitle);
            tvDate = itemView.findViewById(R.id.tvRunDate);
            tvDistance = itemView.findViewById(R.id.tvHistoryDistance);
            tvDuration = itemView.findViewById(R.id.tvHistoryDuration);
            tvPace = itemView.findViewById(R.id.tvHistoryPace);
        }
    }
}
