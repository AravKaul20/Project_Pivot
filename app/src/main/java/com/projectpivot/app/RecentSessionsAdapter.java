package com.projectpivot.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RecentSessionsAdapter extends RecyclerView.Adapter<RecentSessionsAdapter.SessionViewHolder> {
    
    private List<SessionTracker.WorkoutSession> sessions;
    
    public RecentSessionsAdapter(List<SessionTracker.WorkoutSession> sessions) {
        this.sessions = sessions;
    }
    
    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_session, parent, false);
        return new SessionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        SessionTracker.WorkoutSession session = sessions.get(position);
        holder.bind(session);
    }
    
    @Override
    public int getItemCount() {
        return sessions.size();
    }
    
    public void updateSessions(List<SessionTracker.WorkoutSession> newSessions) {
        this.sessions = newSessions;
        notifyDataSetChanged();
    }
    
    static class SessionViewHolder extends RecyclerView.ViewHolder {
        
        private CardView sessionCard;
        private TextView sessionDate;
        private TextView sessionMode;
        private TextView sessionDuration;
        private TextView sessionAccuracy;
        private TextView sessionPunches;
        
        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            sessionCard = itemView.findViewById(R.id.sessionCard);
            sessionDate = itemView.findViewById(R.id.sessionDate);
            sessionMode = itemView.findViewById(R.id.sessionMode);
            sessionDuration = itemView.findViewById(R.id.sessionDuration);
            sessionAccuracy = itemView.findViewById(R.id.sessionAccuracy);
            sessionPunches = itemView.findViewById(R.id.sessionPunches);
        }
        
        public void bind(SessionTracker.WorkoutSession session) {
            sessionDate.setText(session.date);
            
            // Format mode
            String modeText = session.mode.toUpperCase();
            if (session.mode.equals("both")) {
                modeText = "FULL_ANALYSIS";
            }
            sessionMode.setText(modeText);
            
            // Format duration
            sessionDuration.setText(session.getFormattedDuration());
            
            // Format accuracy
            sessionAccuracy.setText(session.getFormattedAccuracy());
            
            // Format punches
            if (session.totalPunches > 0) {
                sessionPunches.setText(session.totalPunches + " punches");
                sessionPunches.setVisibility(View.VISIBLE);
            } else {
                sessionPunches.setVisibility(View.GONE);
            }
            
            // Set card colors based on accuracy
            float accuracy = 0f;
            if (session.mode.equals("stance")) {
                accuracy = session.stanceAccuracy;
            } else if (session.mode.equals("execution")) {
                accuracy = session.executionAccuracy;
            } else {
                accuracy = (session.stanceAccuracy + session.executionAccuracy) / 2f;
            }
            
            // Color coding based on performance
            if (accuracy >= 0.8f) {
                // Excellent performance - green accent
                sessionAccuracy.setTextColor(itemView.getContext().getColor(R.color.accent_green));
            } else if (accuracy >= 0.6f) {
                // Good performance - white
                sessionAccuracy.setTextColor(itemView.getContext().getColor(R.color.text_primary));
            } else {
                // Needs improvement - red
                sessionAccuracy.setTextColor(itemView.getContext().getColor(R.color.error_red));
            }
        }
    }
} 