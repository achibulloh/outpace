package com.example.pace.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pace.R;
import com.example.pace.model.HelpTicket;
import com.example.pace.model.TicketReply;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HelpTicketAdapter extends RecyclerView.Adapter<HelpTicketAdapter.ViewHolder> {

    private List<HelpTicket> ticketList;

    public HelpTicketAdapter(List<HelpTicket> ticketList) {
        this.ticketList = ticketList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_help_ticket, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HelpTicket ticket = ticketList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvCategory.setText(ticket.getCategory());
        holder.tvDescription.setText(ticket.getDescription());
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(ticket.getTimestamp())));

        // Status UI
        holder.tvStatus.setText(ticket.getStatus());
        int statusColor = ContextCompat.getColor(context, R.color.lime);
        if (ticket.getStatus().equalsIgnoreCase("Pending")) statusColor = Color.parseColor("#FFA500");
        else if (ticket.getStatus().equalsIgnoreCase("Resolved")) statusColor = Color.parseColor("#4CAF50");
        
        holder.tvStatus.setTextColor(statusColor);
        holder.tvStatus.getBackground().setTint(Color.argb(40, Color.red(statusColor), Color.green(statusColor), Color.blue(statusColor)));

        // Priority UI
        holder.tvPriority.setText("Priority: " + ticket.getPriority());
        if (ticket.getPriority().equalsIgnoreCase("High")) {
            holder.tvPriority.setTextColor(Color.RED);
        } else if (ticket.getPriority().equalsIgnoreCase("Medium")) {
            holder.tvPriority.setTextColor(Color.parseColor("#FFA500"));
        } else {
            holder.tvPriority.setTextColor(ContextCompat.getColor(context, R.color.muted_fg));
        }

        // Admin Reply UI
        String displayReply = ticket.getAdminReply();
        String displayDate = (ticket.getReplyTimestamp() > 0) ? sdf.format(new Date(ticket.getReplyTimestamp())) : "";

        // Check if there are replies in the list ( Newer array structure )
        if (ticket.getReplies() != null && !ticket.getReplies().isEmpty()) {
            // Find the latest reply from admin
            for (int i = ticket.getReplies().size() - 1; i >= 0; i--) {
                TicketReply r = ticket.getReplies().get(i);
                if (r != null && "admin".equalsIgnoreCase(r.getFrom())) {
                    displayReply = r.getText();
                    displayDate = r.getTime();
                    break;
                }
            }
        }

        if (displayReply != null && !displayReply.isEmpty()) {
            holder.layoutAdminReply.setVisibility(View.VISIBLE);
            holder.tvAdminReply.setText(displayReply);
            if (displayDate != null && !displayDate.isEmpty()) {
                holder.tvReplyDate.setText(displayDate);
                holder.tvReplyDate.setVisibility(View.VISIBLE);
            } else {
                holder.tvReplyDate.setVisibility(View.GONE);
            }
        } else {
            holder.layoutAdminReply.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return ticketList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvDescription, tvStatus, tvPriority, tvDate;
        View layoutAdminReply;
        TextView tvAdminReply, tvReplyDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvTicketCategory);
            tvDescription = itemView.findViewById(R.id.tvTicketDescription);
            tvStatus = itemView.findViewById(R.id.tvTicketStatus);
            tvPriority = itemView.findViewById(R.id.tvTicketPriority);
            tvDate = itemView.findViewById(R.id.tvTicketDate);
            layoutAdminReply = itemView.findViewById(R.id.layoutAdminReply);
            tvAdminReply = itemView.findViewById(R.id.tvAdminReply);
            tvReplyDate = itemView.findViewById(R.id.tvReplyDate);
        }
    }
}
