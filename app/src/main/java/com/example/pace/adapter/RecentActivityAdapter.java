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

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {
    private List<RunRecord> runList;

    public RecentActivityAdapter(List<RunRecord> runList) {
        this.runList = runList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RunRecord run = runList.get(position);
        
        holder.tvDistance.setText(holder.itemView.getContext().getString(R.string.distance_km_val, run.getDistance()));
        
        int paceMins = (int) run.getPace();
        int paceSecs = (int) ((run.getPace() - paceMins) * 60);
        holder.tvPace.setText(holder.itemView.getContext().getString(R.string.pace_val, paceMins, paceSecs));
        
        if (run.getDate() != null && run.getStartTime() != null && run.getEndTime() != null) {
            holder.tvDate.setText(String.format("%s · %s", run.getDate(), run.getStartTime()));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE · HH:mm", Locale.getDefault());
            holder.tvDate.setText(sdf.format(new Date(run.getTimestamp())));
        }
        
        int hour = Integer.parseInt(new SimpleDateFormat("H", Locale.getDefault()).format(new Date(run.getTimestamp())));
        String title;
        if (hour >= 5 && hour < 11) title = holder.itemView.getContext().getString(R.string.morning_run);
        else if (hour >= 11 && hour < 15) title = holder.itemView.getContext().getString(R.string.afternoon_run);
        else if (hour >= 15 && hour < 19) title = holder.itemView.getContext().getString(R.string.evening_run);
        else title = holder.itemView.getContext().getString(R.string.night_run);
        
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
        TextView tvTitle, tvDate, tvDistance, tvPace;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvPace = itemView.findViewById(R.id.tvPace);
        }
    }
}
