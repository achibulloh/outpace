package com.example.pace.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pace.R;
import com.example.pace.model.LeaderboardUser;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
    private List<LeaderboardUser> users;
    private int startDelay = 0;
    private boolean isSequentialEnabled = false;

    public LeaderboardAdapter(List<LeaderboardUser> users) {
        this.users = users;
    }

    public void enableSequentialAnimation(int delay) {
        this.isSequentialEnabled = true;
        this.startDelay = delay;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardUser user = users.get(position);
        holder.tvRank.setText(user.getRank());
        holder.tvName.setText(user.getName());
        holder.tvPace.setText(user.getPace());
        holder.tvDistance.setText(user.getDistance());
        holder.ivIcon.setImageResource(user.getIconResId());

        if (user.isYou()) {
            holder.itemView.setBackgroundResource(R.drawable.podium_1);
            holder.tvName.setTextColor(Color.parseColor("#CDFF00"));
            holder.tvDistance.setTextColor(Color.parseColor("#CDFF00"));
        } else {
            holder.itemView.setBackgroundResource(R.drawable.rounded_card);
            holder.tvName.setTextColor(Color.WHITE);
            holder.tvDistance.setTextColor(Color.WHITE);
        }

        if (isSequentialEnabled) {
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationX(-50f);
            holder.itemView.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(400)
                    .setStartDelay(startDelay + (position * 100))
                    .start();
        }
    }

    @Override
    public int getItemCount() { return users.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvName, tvPace, tvDistance;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvName = itemView.findViewById(R.id.tvName);
            tvPace = itemView.findViewById(R.id.tvPace);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            ivIcon = itemView.findViewById(R.id.ivIcon);
        }
    }
}
