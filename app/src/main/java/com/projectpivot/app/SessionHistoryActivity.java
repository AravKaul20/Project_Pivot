package com.projectpivot.app;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SessionHistoryActivity extends AppCompatActivity {
    
    private TextView backButton;
    private TextView titleText;
    private RecyclerView sessionsRecyclerView;
    private TextView noSessionsMessage;
    private TextView totalSessionsText;
    private TextView totalTimeText;
    private TextView averageAccuracyText;
    
    private SessionTracker sessionTracker;
    private RecentSessionsAdapter sessionsAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_history);
        
        sessionTracker = SessionTracker.getInstance(this);
        
        initViews();
        setupClickListeners();
        loadSessionData();
    }
    
    private void initViews() {
        backButton = findViewById(R.id.backButton);
        titleText = findViewById(R.id.titleText);
        sessionsRecyclerView = findViewById(R.id.sessionsRecyclerView);
        noSessionsMessage = findViewById(R.id.noSessionsMessage);
        totalSessionsText = findViewById(R.id.totalSessionsText);
        totalTimeText = findViewById(R.id.totalTimeText);
        averageAccuracyText = findViewById(R.id.averageAccuracyText);
        
        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        sessionsRecyclerView.setLayoutManager(layoutManager);
        
        // Initialize adapter with empty list
        sessionsAdapter = new RecentSessionsAdapter(new java.util.ArrayList<>());
        sessionsRecyclerView.setAdapter(sessionsAdapter);
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            finish();
            // Removed annoying transition animation
        });
    }
    
    private void loadSessionData() {
        List<SessionTracker.WorkoutSession> allSessions = sessionTracker.getAllSessions();
        
        if (allSessions.isEmpty()) {
            sessionsRecyclerView.setVisibility(View.GONE);
            noSessionsMessage.setVisibility(View.VISIBLE);
            
            // Hide stats when no sessions
            totalSessionsText.setText("0 sessions");
            totalTimeText.setText("0m total");
            averageAccuracyText.setText("0% avg accuracy");
        } else {
            sessionsRecyclerView.setVisibility(View.VISIBLE);
            noSessionsMessage.setVisibility(View.GONE);
            
            // Update adapter with all sessions
            sessionsAdapter.updateSessions(allSessions);
            
            // Calculate and display summary stats
            updateSummaryStats(allSessions);
        }
    }
    
    private void updateSummaryStats(List<SessionTracker.WorkoutSession> sessions) {
        int totalSessions = sessions.size();
        long totalTime = 0;
        float totalAccuracy = 0f;
        int validAccuracySessions = 0;
        
        for (SessionTracker.WorkoutSession session : sessions) {
            totalTime += session.workoutDuration;
            
            // Calculate session accuracy
            float sessionAccuracy = 0f;
            if (session.mode.equals("stance")) {
                sessionAccuracy = session.stanceAccuracy;
            } else if (session.mode.equals("execution")) {
                sessionAccuracy = session.executionAccuracy;
            } else {
                sessionAccuracy = (session.stanceAccuracy + session.executionAccuracy) / 2f;
            }
            
            if (sessionAccuracy > 0f) {
                totalAccuracy += sessionAccuracy;
                validAccuracySessions++;
            }
        }
        
        // Format total time
        long totalMinutes = totalTime / (1000 * 60);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        
        String timeText;
        if (hours > 0) {
            timeText = String.format("%dh %dm total", hours, minutes);
        } else {
            timeText = String.format("%dm total", minutes);
        }
        
        // Calculate average accuracy
        float avgAccuracy = validAccuracySessions > 0 ? (totalAccuracy / validAccuracySessions) * 100f : 0f;
        
        // Update UI
        totalSessionsText.setText(totalSessions + " sessions");
        totalTimeText.setText(timeText);
        averageAccuracyText.setText(String.format("%.1f%% avg accuracy", avgAccuracy));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this screen
        loadSessionData();
    }
} 