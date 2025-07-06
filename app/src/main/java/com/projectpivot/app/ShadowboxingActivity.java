package com.projectpivot.app;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class ShadowboxingActivity extends AppCompatActivity {

    private TextView titleText;
    private TextView backButton;
    private TextView timerDisplay;
    private TextView roundDisplay;
    private TextView phaseDisplay;
    private ProgressBar roundProgress;
    private TextView startPauseButton;
    private TextView resetButton;
    private LinearLayout settingsContainer;
    private TextView roundsText;
    private TextView workTimeText;
    private TextView restTimeText;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private CountDownTimer countDownTimer;
    private SoundManager soundManager;
    private ComboManager comboManager;
    private Runnable comboAnnouncementRunnable;
    
    // Timer settings
    private int totalRounds = 3;
    private int workTimeSeconds = 180; // 3 minutes
    private int restTimeSeconds = 60; // 1 minute
    
    // Current state
    private int currentRound = 1;
    private int timeRemaining;
    private boolean isWorkPhase = true;
    private boolean isRunning = false;
    private boolean isPaused = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shadowboxing);
        
        initViews();
        setupClickListeners();
        resetTimer();
        // Production build - startup animations removed
        
        // Initialize sound manager and combo manager
        soundManager = SoundManager.getInstance(this);
        comboManager = ComboManager.getInstance(this);
        
        // Load settings from SharedPreferences
        loadSettings();
    }
    
    private void initViews() {
        titleText = findViewById(R.id.titleText);
        backButton = findViewById(R.id.backButton);
        timerDisplay = findViewById(R.id.timerDisplay);
        roundDisplay = findViewById(R.id.roundDisplay);
        phaseDisplay = findViewById(R.id.phaseDisplay);
        roundProgress = findViewById(R.id.roundProgress);
        startPauseButton = findViewById(R.id.startPauseButton);
        resetButton = findViewById(R.id.resetButton);
        settingsContainer = findViewById(R.id.settingsContainer);
        roundsText = findViewById(R.id.roundsText);
        workTimeText = findViewById(R.id.workTimeText);
        restTimeText = findViewById(R.id.restTimeText);
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            finish();
            // Removed annoying transition animation
        });
        
        startPauseButton.setOnClickListener(v -> {
            SoundManager soundManager = SoundManager.getInstance(this);
            if (isRunning) {
                soundManager.hapticSelect();
                pauseTimer();
            } else {
                soundManager.hapticSelect();
                startTimer();
            }
        });
        
        resetButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            resetTimer();
        });
    }
    
    // Production build - startup animations removed
    
    private void startTimer() {
        isRunning = true;
        isPaused = false;
        startPauseButton.setText("PAUSE");
        startPauseButton.setTextColor(getColor(R.color.error_red));
        
        // Play start sound
        soundManager.playStartSound();
        
        // Start combo announcements if in work phase
        if (isWorkPhase) {
            startComboAnnouncements();
        }
        
        int totalTime = isWorkPhase ? workTimeSeconds : restTimeSeconds;
        if (timeRemaining == 0) {
            timeRemaining = totalTime;
        }
        
        countDownTimer = new CountDownTimer(timeRemaining * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemaining = (int) (millisUntilFinished / 1000);
                updateDisplay();
                updateProgress();
            }
            
            @Override
            public void onFinish() {
                onPhaseComplete();
            }
        };
        
        countDownTimer.start();
    }
    
    private void pauseTimer() {
        isRunning = false;
        isPaused = true;
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        startPauseButton.setText("RESUME");
        startPauseButton.setTextColor(getColor(R.color.accent_green));
    }
    
    private void resetTimer() {
        isRunning = false;
        isPaused = false;
        currentRound = 1;
        isWorkPhase = true;
        timeRemaining = workTimeSeconds;
        
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        
        startPauseButton.setText("START");
        startPauseButton.setTextColor(getColor(R.color.accent_green));
        
        // Stop combo announcements
        stopComboAnnouncements();
        
        updateDisplay();
        updateProgress();
    }
    
    private void loadSettings() {
        android.content.SharedPreferences preferences = getSharedPreferences("PROJECT_PIVOT_SETTINGS", MODE_PRIVATE);
        
        // Load shadowboxing settings
        totalRounds = Math.max(1, preferences.getInt("total_rounds", 3));
        workTimeSeconds = Math.max(30, preferences.getInt("round_duration_minutes", 3) * 60);
        restTimeSeconds = Math.max(15, preferences.getInt("rest_duration_seconds", 60));
        
        // Update display
        roundsText.setText(totalRounds + " rounds");
        workTimeText.setText((workTimeSeconds / 60) + " min work");
        restTimeText.setText(restTimeSeconds + "s rest");
    }
    
    private void startComboAnnouncements() {
        if (!comboManager.isAnnouncementEnabled()) {
            return;
        }
        
        stopComboAnnouncements(); // Stop any existing announcements
        
        int frequencySeconds = comboManager.getComboFrequencySeconds();
        
        comboAnnouncementRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning && isWorkPhase) { // Only announce during work phases
                    comboManager.announceRandomCombo();
                    
                    // Schedule next announcement
                    handler.postDelayed(this, frequencySeconds * 1000L);
                }
            }
        };
        
        // Start first announcement after initial delay
        handler.postDelayed(comboAnnouncementRunnable, frequencySeconds * 1000L);
    }
    
    private void stopComboAnnouncements() {
        if (comboAnnouncementRunnable != null) {
            handler.removeCallbacks(comboAnnouncementRunnable);
            comboAnnouncementRunnable = null;
        }
    }
    
    private void onPhaseComplete() {
        // Play bell sound and enhanced haptic feedback
        soundManager.playBellSound();
        soundManager.hapticSelect(); // Phase transition haptic
        
        if (isWorkPhase) {
            // Work phase complete, start rest
            isWorkPhase = false;
            timeRemaining = restTimeSeconds;
            
            // Production build - phase animations removed
            
            if (currentRound < totalRounds) {
                // Continue to rest phase
                startTimer();
            } else {
                // Workout complete
                onWorkoutComplete();
                return;
            }
        } else {
            // Rest phase complete, start next round
            currentRound++;
            isWorkPhase = true;
            timeRemaining = workTimeSeconds;
            
            // Production build - round animations removed
            
            // Continue to next work phase
            startTimer();
        }
        
        updateDisplay();
    }
    
    private void onWorkoutComplete() {
        isRunning = false;
        startPauseButton.setText("COMPLETE!");
        startPauseButton.setTextColor(getColor(R.color.accent_green));
        
        // Play victory sound and enhanced success haptic
        soundManager.playWorkoutCompleteSound();
        soundManager.hapticSuccess(); // Double pulse for workout completion
        
        // Production build - completion animations removed
    }
    
    // Production build - animation methods removed
    
    private void updateDisplay() {
        // Update timer
        int minutes = timeRemaining / 60;
        int seconds = timeRemaining % 60;
        timerDisplay.setText(String.format("%02d:%02d", minutes, seconds));
        
        // Update round
        roundDisplay.setText("ROUND_" + currentRound + "/" + totalRounds);
        
        // Update phase
        phaseDisplay.setText(isWorkPhase ? "WORK" : "REST");
        phaseDisplay.setTextColor(isWorkPhase ? getColor(R.color.accent_green) : getColor(R.color.text_secondary));
        
        // Update settings display
        roundsText.setText(totalRounds + "_rounds");
        workTimeText.setText((workTimeSeconds / 60) + "min_work");
        restTimeText.setText((restTimeSeconds / 60) + "min_rest");
    }
    
    private void updateProgress() {
        int totalTime = isWorkPhase ? workTimeSeconds : restTimeSeconds;
        int progress = (int) (((float) (totalTime - timeRemaining) / totalTime) * 100);
        roundProgress.setProgress(progress);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        
        // Stop combo announcements
        stopComboAnnouncements();
        
        // Release resources
        if (comboManager != null) {
            comboManager.release();
        }
    }
} 