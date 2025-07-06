package com.projectpivot.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

public class SoundManager {
    
    private static SoundManager instance;
    private Context context;
    private SharedPreferences preferences;
    private ToneGenerator toneGenerator;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    private SoundManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences("PROJECT_PIVOT_SETTINGS", Context.MODE_PRIVATE);
        initializeToneGenerator();
    }
    
    public static synchronized SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context);
        }
        return instance;
    }
    
    private void initializeToneGenerator() {
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 80); // 80% volume
        } catch (RuntimeException e) {
            toneGenerator = null;
        }
    }
    
    public void playBellSound() {
        if (!isSoundEnabled()) return;
        
        try {
            if (toneGenerator != null) {
                // Play a boxing bell-like sound (multiple short tones)
                playBellSequence();
            }
        } catch (Exception e) {
            // Fallback to system notification sound
            playSystemSound();
        }
    }
    
    public void playStartSound() {
        if (!isSoundEnabled()) return;
        
        try {
            if (toneGenerator != null) {
                // High pitch for start
                toneGenerator.startTone(ToneGenerator.TONE_DTMF_1, 200);
            }
        } catch (Exception e) {
            playSystemSound();
        }
    }
    
    public void playEndSound() {
        if (!isSoundEnabled()) return;
        
        try {
            if (toneGenerator != null) {
                // Lower pitch for end
                toneGenerator.startTone(ToneGenerator.TONE_DTMF_0, 500);
            }
        } catch (Exception e) {
            playSystemSound();
        }
    }
    
    public void playWorkoutCompleteSound() {
        if (!isSoundEnabled()) return;
        
        try {
            if (toneGenerator != null) {
                // Victory sound sequence
                playVictorySequence();
            }
        } catch (Exception e) {
            playSystemSound();
        }
    }
    
    private void playBellSequence() {
        // Simulate boxing bell: 3 quick high tones
        toneGenerator.startTone(ToneGenerator.TONE_DTMF_9, 150);
        
        handler.postDelayed(() -> {
            if (toneGenerator != null) {
                toneGenerator.startTone(ToneGenerator.TONE_DTMF_9, 150);
            }
        }, 200);
        
        handler.postDelayed(() -> {
            if (toneGenerator != null) {
                toneGenerator.startTone(ToneGenerator.TONE_DTMF_9, 150);
            }
        }, 400);
    }
    
    private void playVictorySequence() {
        // Victory sound: ascending tones
        toneGenerator.startTone(ToneGenerator.TONE_DTMF_1, 200);
        
        handler.postDelayed(() -> {
            if (toneGenerator != null) {
                toneGenerator.startTone(ToneGenerator.TONE_DTMF_3, 200);
            }
        }, 250);
        
        handler.postDelayed(() -> {
            if (toneGenerator != null) {
                toneGenerator.startTone(ToneGenerator.TONE_DTMF_5, 300);
            }
        }, 500);
    }
    
    private void playSystemSound() {
        // Fallback to system notification sound
        try {
            android.media.RingtoneManager.getRingtone(
                context, 
                android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            ).play();
        } catch (Exception e) {
            // Silent fallback
        }
    }
    
    public void playMotivationalSound() {
        if (!isSoundEnabled()) return;
        
        try {
            if (toneGenerator != null) {
                // Quick encouraging beep
                toneGenerator.startTone(ToneGenerator.TONE_DTMF_2, 100);
            }
        } catch (Exception e) {
            // Silent fallback
        }
    }
    
    private boolean isSoundEnabled() {
        return preferences.getBoolean("sound_effects_enabled", true);
    }
    
    public void release() {
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
    
    // Enhanced Haptic Feedback Support
    public void vibrate(long duration) {
        if (!isVibrationEnabled()) return;
        
        try {
            android.os.Vibrator vibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(duration, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(duration);
                }
            }
        } catch (Exception e) {
            // Silent fallback
        }
    }
    
    // Light haptic feedback for button presses
    public void hapticClick() {
        if (!isVibrationEnabled()) return;
        
        try {
            android.os.Vibrator vibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            }
        } catch (Exception e) {
            // Silent fallback
        }
    }
    
    // Medium haptic feedback for selections/confirmations
    public void hapticSelect() {
        if (!isVibrationEnabled()) return;
        
        try {
            android.os.Vibrator vibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
        } catch (Exception e) {
            // Silent fallback
        }
    }
    
    // Strong haptic feedback for achievements/successes
    public void hapticSuccess() {
        if (!isVibrationEnabled()) return;
        
        try {
            android.os.Vibrator vibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    // Double pulse for success
                    long[] pattern = {0, 100, 50, 100};
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1));
                } else {
                    long[] pattern = {0, 100, 50, 100};
                    vibrator.vibrate(pattern, -1);
                }
            }
        } catch (Exception e) {
            // Silent fallback
        }
    }
    
    // Error haptic feedback
    public void hapticError() {
        if (!isVibrationEnabled()) return;
        
        try {
            android.os.Vibrator vibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    // Triple short pulses for error
                    long[] pattern = {0, 50, 30, 50, 30, 50};
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1));
                } else {
                    long[] pattern = {0, 50, 30, 50, 30, 50};
                    vibrator.vibrate(pattern, -1);
                }
            }
        } catch (Exception e) {
            // Silent fallback
        }
    }
    
    private boolean isVibrationEnabled() {
        return preferences.getBoolean("vibration_enabled", true); // Changed default to true
    }
} 