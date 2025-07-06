package com.projectpivot.app;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class ProgressActivity extends AppCompatActivity {

    private TextView titleText;
    private LinearLayout statsContainer;
    private CardView weeklyProgressCard;
    private CardView accuracyCard;
    private CardView streakCard;
    private ProgressBar stanceProgress;
    private ProgressBar executionProgress;
    private TextView stancePercentage;
    private TextView executionPercentage;
    private TextView backButton;
    
    // Real data components
    private SessionTracker sessionTracker;
    private TextView weeklySessionsValue;
    private TextView overallAccuracyValue;
    private TextView streakValue;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);
        
        // Initialize session tracker
        sessionTracker = SessionTracker.getInstance(this);
        
        initViews();
        setupClickListeners();
        loadRealData();
        startAnimations();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to progress page
        loadRealData();
    }
    
    private void initViews() {
        titleText = findViewById(R.id.titleText);
        statsContainer = findViewById(R.id.statsContainer);
        weeklyProgressCard = findViewById(R.id.weeklyProgressCard);
        accuracyCard = findViewById(R.id.accuracyCard);
        streakCard = findViewById(R.id.streakCard);
        stanceProgress = findViewById(R.id.stanceProgress);
        executionProgress = findViewById(R.id.executionProgress);
        stancePercentage = findViewById(R.id.stancePercentage);
        executionPercentage = findViewById(R.id.executionPercentage);
        backButton = findViewById(R.id.backButton);
        
        // Find text views for real data (we'll need to update the layout)
        weeklySessionsValue = findViewById(R.id.weeklySessionsValue);
        overallAccuracyValue = findViewById(R.id.overallAccuracyValue);
        streakValue = findViewById(R.id.streakValue);
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            finish();
            // Removed annoying transition animation
        });
    }
    
    private void startAnimations() {
        // Animate title text
        titleText.setAlpha(0f);
        titleText.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(300)
            .start();
        
        // Animate cards
        animateCards();
        animateProgressBars();
    }
    
    private void animateCards() {
        // Animate weekly progress card
        weeklyProgressCard.setAlpha(0f);
        weeklyProgressCard.setTranslationY(100f);
        weeklyProgressCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(400)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
        
        // Animate accuracy card
        accuracyCard.setAlpha(0f);
        accuracyCard.setTranslationY(100f);
        accuracyCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(600)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
        
        // Animate streak card
        streakCard.setAlpha(0f);
        streakCard.setTranslationY(100f);
        streakCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(800)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }
    
    private void loadRealData() {
        // Load real stance and execution accuracy
        float stanceAccuracy = sessionTracker.getOverallStanceAccuracy() * 100; // Convert to percentage
        float executionAccuracy = sessionTracker.getOverallExecutionAccuracy() * 100;
        
        // Load real session data
        int weeklySessions = sessionTracker.getWeeklySessions();
        int currentStreak = sessionTracker.getCurrentStreak();
        float overallAccuracy = (stanceAccuracy + executionAccuracy) / 2f;
        
        // Update UI with real data (but don't animate yet)
        if (weeklySessionsValue != null) {
            weeklySessionsValue.setText(String.valueOf(weeklySessions));
        }
        if (overallAccuracyValue != null) {
            overallAccuracyValue.setText(String.format("%.1f%%", overallAccuracy));
        }
        if (streakValue != null) {
            streakValue.setText(String.valueOf(currentStreak));
        }
        

    }
    
    private void animateProgressBars() {
        // Get REAL accuracy values
        float realStanceAccuracy = sessionTracker.getOverallStanceAccuracy() * 100;
        float realExecutionAccuracy = sessionTracker.getOverallExecutionAccuracy() * 100;
        
        // Use real values or minimum 5% for animation
        int stanceTarget = Math.max((int) realStanceAccuracy, 5);
        int executionTarget = Math.max((int) realExecutionAccuracy, 5);
        
        // Animate stance progress to REAL value
        ValueAnimator stanceAnimator = ValueAnimator.ofInt(0, stanceTarget);
        stanceAnimator.setDuration(1500);
        stanceAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        stanceAnimator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            stanceProgress.setProgress(progress);
            stancePercentage.setText(progress + "%");
        });
        
        // Animate execution progress to REAL value
        ValueAnimator executionAnimator = ValueAnimator.ofInt(0, executionTarget);
        executionAnimator.setDuration(1500);
        executionAnimator.setStartDelay(200);
        executionAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        executionAnimator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            executionProgress.setProgress(progress);
            executionPercentage.setText(progress + "%");
        });
        
        stanceAnimator.start();
        executionAnimator.start();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
} 