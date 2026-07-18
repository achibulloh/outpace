package com.example.pace.adapter;

import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pace.R;
import com.example.pace.model.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    private int lastAnimatedPosition = -1;

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        holder.tvText.setText(msg.getText());
        
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.layoutContainer.getLayoutParams();
        if (msg.isUser()) {
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            params.addRule(RelativeLayout.ALIGN_PARENT_START, 0);
            params.setMargins(60, 0, 0, 0);
            holder.layoutContainer.setBackgroundResource(R.drawable.bubble_user);
            holder.layoutContainer.setBackgroundTintList(null);
            holder.tvText.setTextColor(Color.BLACK);
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_START);
            params.addRule(RelativeLayout.ALIGN_PARENT_END, 0);
            params.setMargins(0, 0, 60, 0);
            holder.layoutContainer.setBackgroundResource(R.drawable.bubble_ai);
            holder.layoutContainer.setBackgroundTintList(null);
            holder.tvText.setTextColor(Color.WHITE);
        }
        holder.layoutContainer.setLayoutParams(params);

        View cardImage = holder.itemView.findViewById(R.id.cardImage);
        if (msg.getImage() != null) {
            cardImage.setVisibility(View.VISIBLE);
            holder.ivImage.setImageBitmap(msg.getImage());
        } else {
            cardImage.setVisibility(View.GONE);
        }

        setAnimation(holder.itemView, position);
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastAnimatedPosition) {
            viewToAnimate.setAlpha(0f);
            viewToAnimate.setTranslationY(50f);
            
            viewToAnimate.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(500)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                    .start();
            lastAnimatedPosition = position;
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutContainer;
        TextView tvText;
        ImageView ivImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutContainer = itemView.findViewById(R.id.layoutContainer);
            tvText = itemView.findViewById(R.id.tvMessageText);
            ivImage = itemView.findViewById(R.id.ivMessageImage);
        }
    }
}
