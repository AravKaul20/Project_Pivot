package com.projectpivot.app;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class DashboardActivity extends AppCompatActivity {

    private TextView statusIndicator;
    private CardView stanceAnalysisButton;
    private CardView punchAnalysisButton;
    private LinearLayout homeTab;
    private LinearLayout trainTab;
    private LinearLayout progressTab;
    private LinearLayout profileTab;
    private androidx.cardview.widget.CardView settingsButton;
    private androidx.cardview.widget.CardView achievementsButton;
    private androidx.cardview.widget.CardView shadowboxingButton;
    private androidx.cardview.widget.CardView comboLibraryButton;
    
    // Real data components
    private SessionTracker sessionTracker;
    private SoundManager soundManager;
    private TextView accuracyValue;
    private TextView sessionsValue;
    private TextView streakValue;
    
    // Recent sessions
    private androidx.recyclerview.widget.RecyclerView recentSessionsRecyclerView;
    private TextView noSessionsMessage;
    private RecentSessionsAdapter sessionsAdapter;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isStatusAnimating = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        // Initialize session tracker and sound manager
        sessionTracker = SessionTracker.getInstance(this);
        soundManager = SoundManager.getInstance(this);
        // Refresh session data for current user
        sessionTracker.refreshUserSession();
        
        initViews();
        setupClickListeners();
        loadRealData();
        startStatusAnimation();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh session data when returning to dashboard
        sessionTracker.refreshUserSession();
        loadRealData();
        
        // Reset status to ready when returning to dashboard
        handler.postDelayed(() -> {
            updateStatusIndicator("● AI_READY");
        }, 500);
    }
    
    private void initViews() {
        statusIndicator = findViewById(R.id.statusIndicator);
        stanceAnalysisButton = findViewById(R.id.stanceAnalysisButton);
        punchAnalysisButton = findViewById(R.id.punchAnalysisButton);
        homeTab = findViewById(R.id.homeTab);
        trainTab = findViewById(R.id.trainTab);
        progressTab = findViewById(R.id.progressTab);
        profileTab = findViewById(R.id.profileTab);
        settingsButton = findViewById(R.id.settingsButton);
        achievementsButton = findViewById(R.id.achievementsButton);
        shadowboxingButton = findViewById(R.id.shadowboxingButton);
        comboLibraryButton = findViewById(R.id.comboLibraryButton);
        
        // Find real data text views
        accuracyValue = findViewById(R.id.accuracyValue);
        sessionsValue = findViewById(R.id.sessionsValue);
        streakValue = findViewById(R.id.streakValue);
        
        // Find recent sessions views
        recentSessionsRecyclerView = findViewById(R.id.recentSessionsRecyclerView);
        noSessionsMessage = findViewById(R.id.noSessionsMessage);
        
        // Setup RecyclerView
        setupRecentSessions();
    }
    
    private void setupClickListeners() {
        stanceAnalysisButton.setOnClickListener(v -> {
            soundManager.hapticClick();
            animateButtonPress(stanceAnalysisButton, () -> {
                updateStatusIndicator("● STANCE_MODE");
                // Navigate to stance analysis
                Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
                intent.putExtra("analysis_mode", "stance");
                startActivity(intent);
                // Removed annoying transition animation
            });
        });
        
        punchAnalysisButton.setOnClickListener(v -> {
            soundManager.hapticClick();
            animateButtonPress(punchAnalysisButton, () -> {
                updateStatusIndicator("● EXECUTION_MODE");
                // Navigate to punch analysis
                Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
                intent.putExtra("analysis_mode", "execution");
                startActivity(intent);
                // Removed annoying transition animation
            });
        });
        
        // Bottom navigation
        homeTab.setOnClickListener(v -> {
            soundManager.hapticClick();
            selectTab(homeTab, "home");
        });
        trainTab.setOnClickListener(v -> {
            soundManager.hapticClick();
            selectTab(trainTab, "train");
        });
        progressTab.setOnClickListener(v -> {
            soundManager.hapticClick();
            selectTab(progressTab, "progress");
        });
        profileTab.setOnClickListener(v -> {
            soundManager.hapticClick();
            selectTab(profileTab, "profile");
        });
        
        // Settings button
        settingsButton.setOnClickListener(v -> {
            soundManager.hapticClick();
            animateButtonPress(settingsButton, () -> {
                Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
                startActivity(intent);
                // Removed annoying transition animation
            });
        });
        
        // View all sessions button
        findViewById(R.id.viewAllSessions).setOnClickListener(v -> viewAllSessions());
        
        // Achievements button
        achievementsButton.setOnClickListener(v -> {
            soundManager.hapticClick();
            animateButtonPress(achievementsButton, () -> {
                Intent intent = new Intent(DashboardActivity.this, AchievementsActivity.class);
                startActivity(intent);
                // Removed annoying transition animation
            });
        });
        
        // Shadowboxing button
        shadowboxingButton.setOnClickListener(v -> {
            soundManager.hapticClick();
            animateButtonPress(shadowboxingButton, () -> {
                updateStatusIndicator("● SHADOWBOX_MODE");
                Intent intent = new Intent(DashboardActivity.this, ShadowboxingActivity.class);
                startActivity(intent);
                // Removed annoying transition animation
            });
        });
        
        // Combo Library button
        comboLibraryButton.setOnClickListener(v -> {
            soundManager.hapticClick();
            animateButtonPress(comboLibraryButton, () -> {
                Intent intent = new Intent(DashboardActivity.this, ComboLibraryActivity.class);
                startActivity(intent);
                // Removed annoying transition animation
            });
        });
    }
    
    private void viewAllSessions() {
        soundManager.hapticClick();
        // Create and launch a detailed sessions activity
        Intent intent = new Intent(DashboardActivity.this, SessionHistoryActivity.class);
        startActivity(intent);
        // Removed annoying transition animation
    }
    
    private void loadRealData() {
        // Real data logging removed for production
        
        // Load real accuracy (average of stance and execution)
        float stanceAccuracy = sessionTracker.getOverallStanceAccuracy();
        float executionAccuracy = sessionTracker.getOverallExecutionAccuracy();
        float overallAccuracy = (stanceAccuracy + executionAccuracy) / 2f * 100f;
        
        // Load real session count
        int totalSessions = sessionTracker.getTotalSessions();
        
        // Load real streak
        int currentStreak = sessionTracker.getCurrentStreak();
        
        // Update UI with real data
        if (accuracyValue != null) {
            accuracyValue.setText(String.format("%.0f%%", overallAccuracy));
        }
        if (sessionsValue != null) {
            sessionsValue.setText(String.valueOf(totalSessions));
        }
        if (streakValue != null) {
            streakValue.setText(String.valueOf(currentStreak));
        }
        
        // Update recent sessions
        updateRecentSessions();
    }
    
    private void setupRecentSessions() {
        // Setup LinearLayoutManager
        androidx.recyclerview.widget.LinearLayoutManager layoutManager = 
            new androidx.recyclerview.widget.LinearLayoutManager(this);
        recentSessionsRecyclerView.setLayoutManager(layoutManager);
        
        // Initialize adapter with empty list
        sessionsAdapter = new RecentSessionsAdapter(new java.util.ArrayList<>());
        recentSessionsRecyclerView.setAdapter(sessionsAdapter);
    }
    
    private void updateRecentSessions() {
        java.util.List<SessionTracker.WorkoutSession> recentSessions = 
            sessionTracker.getRecentSessions(5); // Get last 5 sessions
        
        if (recentSessions.isEmpty()) {
            recentSessionsRecyclerView.setVisibility(android.view.View.GONE);
            noSessionsMessage.setVisibility(android.view.View.VISIBLE);
        } else {
            recentSessionsRecyclerView.setVisibility(android.view.View.VISIBLE);
            noSessionsMessage.setVisibility(android.view.View.GONE);
            sessionsAdapter.updateSessions(recentSessions);
        }
    }
    
    private void startStatusAnimation() {
        if (isStatusAnimating) return;
        isStatusAnimating = true;
        
        // Animate the status indicator with a subtle pulse
        ObjectAnimator pulse = ObjectAnimator.ofFloat(statusIndicator, "alpha", 1f, 0.7f, 1f);
        pulse.setDuration(2000);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setInterpolator(new AccelerateDecelerateInterpolator());
        pulse.start();
    }
    
    private void updateStatusIndicator(String status) {
        // Fade out current status
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(statusIndicator, "alpha", 1f, 0f);
        fadeOut.setDuration(200);
        fadeOut.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // Update text and fade back in
                statusIndicator.setText(status);
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(statusIndicator, "alpha", 0f, 1f);
                fadeIn.setDuration(200);
                fadeIn.start();
            }
        });
        fadeOut.start();
    }
    
    private void animateButtonPress(View button, Runnable onComplete) {
        // Scale animation with overshoot
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.95f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.95f);
        scaleDownX.setDuration(100);
        scaleDownY.setDuration(100);
        
        scaleDownX.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(button, "scaleX", 0.95f, 1.05f, 1f);
                ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(button, "scaleY", 0.95f, 1.05f, 1f);
                scaleUpX.setDuration(200);
                scaleUpY.setDuration(200);
                scaleUpX.setInterpolator(new AccelerateDecelerateInterpolator());
                scaleUpY.setInterpolator(new AccelerateDecelerateInterpolator());
                
                scaleUpX.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        handler.postDelayed(() -> {
                            if (onComplete != null) {
                                onComplete.run();
                            }
                        }, 100);
                    }
                });
                
                scaleUpX.start();
                scaleUpY.start();
            }
        });
        
        scaleDownX.start();
        scaleDownY.start();
    }
    
    private void selectTab(LinearLayout selectedTab, String tabName) {
        // Reset all tabs
        resetTabColors();
        
        // Highlight selected tab
        TextView selectedText = (TextView) selectedTab.getChildAt(1);
        selectedText.setTextColor(getColor(R.color.accent_green));
        
        // Add selection animation
        ObjectAnimator scaleUp = ObjectAnimator.ofFloat(selectedTab, "scaleY", 1f, 1.1f, 1f);
        scaleUp.setDuration(200);
        scaleUp.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleUp.start();
        
        // Handle tab navigation
        switch (tabName) {
            case "home":
                // Already on home
                break;
            case "train":
                // Navigate to training selection (MainActivity with both modes)
                Intent trainIntent = new Intent(DashboardActivity.this, MainActivity.class);
                trainIntent.putExtra("analysis_mode", "both");
                startActivity(trainIntent);
                // Removed annoying transition animation
                break;
            case "progress":
                // Navigate to progress screen
                Intent progressIntent = new Intent(DashboardActivity.this, ProgressActivity.class);
                startActivity(progressIntent);
                // Removed annoying transition animation
                break;
            case "profile":
                // Navigate to profile screen
                Intent profileIntent = new Intent(DashboardActivity.this, ProfileActivity.class);
                startActivity(profileIntent);
                // Removed annoying transition animation
                break;

        }
    }
    
    private void resetTabColors() {
        // Reset all tab text colors to inactive
        ((TextView) homeTab.getChildAt(1)).setTextColor(getColor(R.color.text_secondary));
        ((TextView) trainTab.getChildAt(1)).setTextColor(getColor(R.color.text_secondary));
        ((TextView) progressTab.getChildAt(1)).setTextColor(getColor(R.color.text_secondary));
        ((TextView) profileTab.getChildAt(1)).setTextColor(getColor(R.color.text_secondary));
        
        // Set home as active by default
        ((TextView) homeTab.getChildAt(1)).setTextColor(getColor(R.color.accent_green));
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        isStatusAnimating = false;
    }
} 