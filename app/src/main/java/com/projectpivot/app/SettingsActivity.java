package com.projectpivot.app;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;
import android.app.TimePickerDialog;
import android.widget.TimePicker;

public class SettingsActivity extends AppCompatActivity {

    private TextView titleText;
    private TextView backButton;
    private LinearLayout settingsContainer;
    
    // Settings switches
    private Switch liveTipsSwitch;
    private Switch soundEffectsSwitch;
    private Switch vibrationSwitch;
    private Switch autoStartSwitch;
    private Switch announceCombosSwitch;
    private Switch punchCounterSwitch;

    
    // Settings text views
    private TextView confidenceThresholdText;
    private TextView tipFrequencyText;
    private TextView cameraResolutionText;
    private TextView comboFrequencyText;
    private TextView voicePitchText;
    private TextView roundDurationText;
    private TextView restDurationText;

    
    // Cards for click handling
    private CardView cloudSyncCard;
    private CardView resetSettingsCard;
    private CardView aboutCard;

    
    private SharedPreferences preferences;
    private FirestoreManager firestoreManager;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    // Managers
    private AchievementManager achievementManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // Initialize SharedPreferences
        preferences = getSharedPreferences("PROJECT_PIVOT_SETTINGS", MODE_PRIVATE);
        firestoreManager = FirestoreManager.getInstance(this);
        
        // Initialize managers
        achievementManager = AchievementManager.getInstance(this);
        
        initViews();
        setupClickListeners();
        loadSettings();
        // Production build - startup animations removed
        
        // Check for new achievements when settings are opened
        achievementManager.checkForNewAchievements();
    }
    
    private void initViews() {
        titleText = findViewById(R.id.titleText);
        backButton = findViewById(R.id.backButton);
        settingsContainer = findViewById(R.id.settingsContainer);
        
        liveTipsSwitch = findViewById(R.id.liveTipsSwitch);
        soundEffectsSwitch = findViewById(R.id.soundEffectsSwitch);
        vibrationSwitch = findViewById(R.id.vibrationSwitch);
        autoStartSwitch = findViewById(R.id.autoStartSwitch);
        announceCombosSwitch = findViewById(R.id.announceCombosSwitch);
        punchCounterSwitch = findViewById(R.id.punchCounterSwitch);
        
        confidenceThresholdText = findViewById(R.id.confidenceThresholdText);
        tipFrequencyText = findViewById(R.id.tipFrequencyText);
        cameraResolutionText = findViewById(R.id.cameraResolutionText);
        comboFrequencyText = findViewById(R.id.comboFrequencyText);
        voicePitchText = findViewById(R.id.voicePitchText);
        roundDurationText = findViewById(R.id.roundDurationText);
        restDurationText = findViewById(R.id.restDurationText);
        cloudSyncCard = findViewById(R.id.cloudSyncCard);
        resetSettingsCard = findViewById(R.id.resetSettingsCard);
        aboutCard = findViewById(R.id.aboutCard);
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            saveSettings();
            finish();
            // Removed annoying transition animation
        });
        
        // Live Tips Toggle
        liveTipsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SoundManager.getInstance(this).hapticClick();
            preferences.edit().putBoolean("live_tips_enabled", isChecked).apply();
            showToast(isChecked ? "Live tips enabled" : "Live tips disabled");
        });
        
        // Sound Effects Toggle
        soundEffectsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SoundManager.getInstance(this).hapticClick();
            preferences.edit().putBoolean("sound_effects_enabled", isChecked).apply();
            showToast(isChecked ? "Sound effects enabled" : "Sound effects disabled");
        });
        
        // Vibration Toggle
        vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SoundManager.getInstance(this).hapticClick();
            preferences.edit().putBoolean("vibration_enabled", isChecked).apply();
            showToast(isChecked ? "Vibration enabled" : "Vibration disabled");
        });
        
        // Auto Start Toggle
        autoStartSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("auto_start_analysis", isChecked).apply();
            showToast(isChecked ? "Auto-start analysis enabled" : "Auto-start analysis disabled");
        });
        
        // Announce Combos Toggle
        announceCombosSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("announce_combos_enabled", isChecked).apply();
            showToast(isChecked ? "Combo announcements enabled" : "Combo announcements disabled");
        });
        
        // Punch Counter Toggle
        punchCounterSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("punch_counter_enabled", isChecked).apply();
            showToast(isChecked ? "Punch counter enabled" : "Punch counter disabled");
        });
        

        
        // Confidence Threshold Adjustment
        findViewById(R.id.confidenceCard).setOnClickListener(v -> {
            adjustConfidenceThreshold();
        });
        
        // Tip Frequency Adjustment
        findViewById(R.id.tipFrequencyCard).setOnClickListener(v -> {
            adjustTipFrequency();
        });
        
        // Camera Resolution Adjustment
        findViewById(R.id.cameraResolutionCard).setOnClickListener(v -> {
            adjustCameraResolution();
        });
        
        // Combo Frequency Adjustment
        findViewById(R.id.comboFrequencyCard).setOnClickListener(v -> {
            adjustComboFrequency();
        });
        
        // Voice Pitch Adjustment
        findViewById(R.id.voicePitchCard).setOnClickListener(v -> {
            adjustVoicePitch();
        });
        
        // Round Duration Adjustment
        findViewById(R.id.roundDurationCard).setOnClickListener(v -> {
            adjustRoundDuration();
        });
        
        // Rest Duration Adjustment
        findViewById(R.id.restDurationCard).setOnClickListener(v -> {
            adjustRestDuration();
        });
        
        // Reset Settings
        resetSettingsCard.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            resetAllSettings();
        });
        
        // About Section
        aboutCard.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            Intent intent = new Intent(SettingsActivity.this, PrivacyPolicyActivity.class);
            startActivity(intent);
            // Removed annoying transition animation
        });
        
        // Cloud Sync
        cloudSyncCard.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            syncToCloud();
        });
        

    }
    
    private void loadSettings() {
        // Load saved settings or use defaults
        liveTipsSwitch.setChecked(preferences.getBoolean("live_tips_enabled", true));
        soundEffectsSwitch.setChecked(preferences.getBoolean("sound_effects_enabled", true));
        vibrationSwitch.setChecked(preferences.getBoolean("vibration_enabled", false));
        autoStartSwitch.setChecked(preferences.getBoolean("auto_start_analysis", false));
        announceCombosSwitch.setChecked(preferences.getBoolean("announce_combos_enabled", false));
        punchCounterSwitch.setChecked(preferences.getBoolean("punch_counter_enabled", true));

        
        // Load threshold values
        float confidenceThreshold = preferences.getFloat("confidence_threshold", 0.7f);
        int tipFrequency = preferences.getInt("tip_frequency_seconds", 20);
        String cameraResolution = preferences.getString("camera_resolution", "720p");
        int comboFrequency = preferences.getInt("combo_frequency_seconds", 15);
        float voicePitch = preferences.getFloat("voice_pitch", 0.8f);
        int roundDuration = preferences.getInt("round_duration_minutes", 3);
        int restDuration = preferences.getInt("rest_duration_seconds", 60);
        
        confidenceThresholdText.setText(String.format("%.1f", confidenceThreshold));
        tipFrequencyText.setText(tipFrequency + "s");
        cameraResolutionText.setText(cameraResolution);
        comboFrequencyText.setText(comboFrequency + "s");
        voicePitchText.setText(String.format("%.1f", voicePitch));
        roundDurationText.setText(roundDuration + "m");
        restDurationText.setText(restDuration + "s");
        


    }
    
    private void saveSettings() {
        // Settings are saved automatically via listeners
        showToast("Settings saved!");
    }
    
    private void adjustConfidenceThreshold() {
        float currentThreshold = preferences.getFloat("confidence_threshold", 0.7f);
        float newThreshold = currentThreshold + 0.1f;
        if (newThreshold > 0.9f) newThreshold = 0.5f; // Cycle back to 0.5
        
        preferences.edit().putFloat("confidence_threshold", newThreshold).apply();
        confidenceThresholdText.setText(String.format("%.1f", newThreshold));
        showToast("Confidence threshold: " + String.format("%.1f", newThreshold));
    }
    
    private void adjustTipFrequency() {
        int currentFrequency = preferences.getInt("tip_frequency_seconds", 20);
        int newFrequency = currentFrequency + 10;
        if (newFrequency > 60) newFrequency = 10; // Cycle back to 10s
        
        preferences.edit().putInt("tip_frequency_seconds", newFrequency).apply();
        tipFrequencyText.setText(newFrequency + "s");
        showToast("Tip frequency: " + newFrequency + " seconds");
    }
    
    private void adjustCameraResolution() {
        String currentResolution = preferences.getString("camera_resolution", "720p");
        String newResolution;
        
        switch (currentResolution) {
            case "480p":
                newResolution = "720p";
                break;
            case "720p":
                newResolution = "1080p";
                break;
            case "1080p":
                newResolution = "480p";
                break;
            default:
                newResolution = "720p";
                break;
        }
        
        preferences.edit().putString("camera_resolution", newResolution).apply();
        cameraResolutionText.setText(newResolution);
        showToast("Camera resolution: " + newResolution);
    }
    
    private void adjustComboFrequency() {
        int currentFrequency = preferences.getInt("combo_frequency_seconds", 15);
        int newFrequency = currentFrequency + 5;
        if (newFrequency > 60) newFrequency = 5; // Cycle back to 5s
        
        preferences.edit().putInt("combo_frequency_seconds", newFrequency).apply();
        comboFrequencyText.setText(newFrequency + "s");
        showToast("Combo frequency: " + newFrequency + " seconds");
    }
    
    private void adjustVoicePitch() {
        float currentPitch = preferences.getFloat("voice_pitch", 0.8f);
        float newPitch = currentPitch + 0.1f;
        if (newPitch > 1.2f) newPitch = 0.6f; // Cycle from 0.6 (very deep) to 1.2 (higher)
        
        preferences.edit().putFloat("voice_pitch", newPitch).apply();
        voicePitchText.setText(String.format("%.1f", newPitch));
        
        String pitchDescription;
        if (newPitch <= 0.7f) pitchDescription = "Very Deep 💪";
        else if (newPitch <= 0.8f) pitchDescription = "Deep 🥊";
        else if (newPitch <= 0.9f) pitchDescription = "Normal 🎯";
        else if (newPitch <= 1.0f) pitchDescription = "Higher 📢";
        else pitchDescription = "High 🔊";
        
        showToast("Voice: " + pitchDescription);
        
        // Update the ComboManager with new pitch
        ComboManager comboManager = ComboManager.getInstance(this);
        comboManager.updateVoiceSettings();
        
        // Test the new voice immediately
        handler.postDelayed(() -> {
            comboManager.announceRandomCombo();
        }, 500);
    }
    
    private void adjustRoundDuration() {
        int currentDuration = preferences.getInt("round_duration_minutes", 3);
        int newDuration = currentDuration + 1;
        if (newDuration > 10) newDuration = 1; // Cycle back to 1 minute
        
        preferences.edit().putInt("round_duration_minutes", newDuration).apply();
        roundDurationText.setText(newDuration + "m");
        showToast("Round duration: " + newDuration + " minutes");
    }
    
    private void adjustRestDuration() {
        int currentDuration = preferences.getInt("rest_duration_seconds", 60);
        int newDuration = currentDuration + 15;
        if (newDuration > 120) newDuration = 15; // Cycle back to 15s
        
        preferences.edit().putInt("rest_duration_seconds", newDuration).apply();
        restDurationText.setText(newDuration + "s");
        showToast("Rest duration: " + newDuration + " seconds");
    }

    private void resetAllSettings() {
        preferences.edit().clear().apply();
        loadSettings();
        showToast("All settings reset to defaults!");
        
        // Production build - reset animations removed
    }
    
    private void testTTSFunctionality() {
        ComboManager comboManager = ComboManager.getInstance(this);
        
        if (!comboManager.isTTSReady()) {
            // Try to reinitialize TTS
            comboManager.retryTTSInitialization();
            
            // Wait a moment and check again
            handler.postDelayed(() -> {
                if (comboManager.isTTSReady()) {
                    showToast("✅ TTS initialized successfully!");
                    comboManager.announceRandomCombo();
                } else {
                    showToast("❌ TTS initialization failed. Check device settings.");
                    // Open device TTS settings
                    openDeviceTTSSettings();
                }
            }, 2000);
        } else {
            showToast("🔊 Testing TTS...");
            comboManager.announceRandomCombo();
        }
    }
    
    private void openDeviceTTSSettings() {
        try {
            // Open Android TTS settings
            Intent intent = new Intent();
            intent.setAction("com.android.settings.TTS_SETTINGS");
            startActivity(intent);
        } catch (Exception e) {
            try {
                // Fallback to general settings
                Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                startActivity(intent);
                showToast("Navigate to: Settings > Accessibility > Text-to-speech");
            } catch (Exception ex) {
                showToast("Please check your device's Text-to-Speech settings manually");
            }
        }
    }
    
    // Production build - startup animations removed
    
    // Production build - card animations removed
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void syncToCloud() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            showToast("❌ Please sign in to sync data to cloud");
            return;
        }
        
        showToast("☁️ Syncing workout data to cloud...");
        
        // Sync local sessions to Firestore
        firestoreManager.syncLocalSessionsToFirestore();
        
        // Show success message after a delay
        handler.postDelayed(() -> {
            showToast("✅ Workout data synced to Firebase cloud!");
        }, 2000);
    }
    

    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
} 