package com.projectpivot.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ComboManager {
    
    private static final String TAG = "ComboManager";
    private static ComboManager instance;
    
    private Context context;
    private SharedPreferences preferences;
    private TextToSpeech textToSpeech;
    private List<BoxingCombo> combos;
    private Random random;
    private boolean ttsInitialized = false;
    private boolean ttsInitializing = false;
    private int initializationAttempts = 0;
    private static final int MAX_INIT_ATTEMPTS = 3;
    
    public static class BoxingCombo {
        public String id;
        public String sequence;
        
        public BoxingCombo(String id, String sequence) {
            this.id = id;
            this.sequence = sequence;
        }
    }
    
    private ComboManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences("PROJECT_PIVOT_SETTINGS", Context.MODE_PRIVATE);
        this.combos = new ArrayList<>();
        this.random = new Random();
        
        Log.d(TAG, "ComboManager created, initializing...");
        loadCombosFromCSV();
        checkTTSDataAndInitialize();
    }
    
    public static synchronized ComboManager getInstance(Context context) {
        if (instance == null) {
            instance = new ComboManager(context);
        }
        return instance;
    }
    
    private void checkTTSDataAndInitialize() {
        Log.d(TAG, "Checking TTS data availability...");
        
        // First check if TTS data is available
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        
        // Since we can't startActivityForResult from a service context,
        // we'll proceed with initialization and handle errors gracefully
        initializeTTS();
    }
    
    private void initializeTTS() {
        if (ttsInitializing) {
            Log.d(TAG, "TTS initialization already in progress");
            return;
        }
        
        if (initializationAttempts >= MAX_INIT_ATTEMPTS) {
            Log.e(TAG, "Max TTS initialization attempts reached");
            return;
        }
        
        ttsInitializing = true;
        initializationAttempts++;
        
        Log.d(TAG, "Initializing TTS (attempt " + initializationAttempts + ")");
        
        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                ttsInitializing = false;
                
                Log.d(TAG, "TTS onInit called with status: " + status);
                
                if (status == TextToSpeech.SUCCESS) {
                    Log.d(TAG, "TTS initialization successful");
                    
                    // Check available languages
                    logAvailableLanguages();
                    
                    // Try to set language to US English
                    int langResult = textToSpeech.setLanguage(Locale.US);
                    Log.d(TAG, "setLanguage(Locale.US) result: " + langResult);
                    
                    if (langResult == TextToSpeech.LANG_MISSING_DATA) {
                        Log.e(TAG, "TTS Language data missing for US English");
                        // Try UK English as fallback
                        langResult = textToSpeech.setLanguage(Locale.UK);
                        Log.d(TAG, "setLanguage(Locale.UK) result: " + langResult);
                    }
                    
                    if (langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "TTS Language not supported");
                        // Try default locale
                        langResult = textToSpeech.setLanguage(Locale.getDefault());
                        Log.d(TAG, "setLanguage(Locale.getDefault()) result: " + langResult);
                    }
                    
                    if (langResult != TextToSpeech.LANG_NOT_SUPPORTED && 
                        langResult != TextToSpeech.LANG_MISSING_DATA) {
                        
                        // Configure TTS settings for powerful boxing announcements
                        configureMaleVoice();
                        
                        ttsInitialized = true;
                        Log.d(TAG, "TTS fully configured and ready!");
                        
                                // Production ready
                        
                    } else {
                        Log.e(TAG, "No suitable language found for TTS");
                        ttsInitialized = false;
                    }
                    
                } else {
                    Log.e(TAG, "TTS initialization failed with status: " + status);
                    ttsInitialized = false;
                    
                    // Retry initialization after a delay
                    if (initializationAttempts < MAX_INIT_ATTEMPTS) {
                        Log.d(TAG, "Retrying TTS initialization in 2 seconds...");
                        new android.os.Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                initializeTTS();
                            }
                        }, 2000);
                    }
                }
            }
        });
    }
    
    private void configureMaleVoice() {
        if (textToSpeech == null) return;
        
        Log.d(TAG, "Configuring male voice for boxing announcements...");
        
        // Try to find and set a male voice
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            try {
                java.util.Set<android.speech.tts.Voice> voices = textToSpeech.getVoices();
                android.speech.tts.Voice selectedVoice = null;
                
                Log.d(TAG, "Available voices:");
                for (android.speech.tts.Voice voice : voices) {
                    String voiceName = voice.getName();
                    String gender = "Unknown";
                    
                    // Check if voice name contains male indicators
                    if (voiceName.toLowerCase().contains("male") || 
                        voiceName.toLowerCase().contains("man") ||
                        voiceName.toLowerCase().contains("masculine")) {
                        gender = "Male";
                        selectedVoice = voice; // Prefer explicitly male voices
                    } else if (voiceName.toLowerCase().contains("female") || 
                               voiceName.toLowerCase().contains("woman") ||
                               voiceName.toLowerCase().contains("feminine")) {
                        gender = "Female";
                    }
                    
                    Log.d(TAG, "  Voice: " + voiceName + " (" + gender + ")");
                }
                
                // If we found a male voice, use it
                if (selectedVoice != null) {
                    int result = textToSpeech.setVoice(selectedVoice);
                    Log.d(TAG, "Set male voice: " + selectedVoice.getName() + " (result: " + result + ")");
                } else {
                    Log.d(TAG, "No explicitly male voice found, using default");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error setting voice", e);
            }
        }
        
        // Configure voice parameters for powerful boxing coach sound
        updateVoiceParameters();
        
        Log.d(TAG, "Voice configured with user preferences");
    }
    
    private void updateVoiceParameters() {
        if (textToSpeech == null) return;
        
        float pitch = preferences.getFloat("voice_pitch", 0.8f);
        textToSpeech.setSpeechRate(0.85f);  // Slightly slower for authority
        textToSpeech.setPitch(pitch);       // User-configurable pitch
        
        Log.d(TAG, "Voice parameters updated: Rate=0.85, Pitch=" + pitch);
    }
    
    public void updateVoiceSettings() {
        Log.d(TAG, "Updating voice settings from preferences");
        updateVoiceParameters();
    }
    
    private void logAvailableLanguages() {
        if (textToSpeech != null) {
            Log.d(TAG, "Available TTS languages:");
            
            // Check common languages
            Locale[] commonLocales = {
                Locale.US, Locale.UK, Locale.CANADA,
                Locale.getDefault()
            };
            
            for (Locale locale : commonLocales) {
                int availability = textToSpeech.isLanguageAvailable(locale);
                String status = getLanguageStatusString(availability);
                Log.d(TAG, "  " + locale.toString() + ": " + status);
            }
        }
    }
    
    private String getLanguageStatusString(int status) {
        switch (status) {
            case TextToSpeech.LANG_AVAILABLE:
                return "AVAILABLE";
            case TextToSpeech.LANG_COUNTRY_AVAILABLE:
                return "COUNTRY_AVAILABLE";
            case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                return "COUNTRY_VAR_AVAILABLE";
            case TextToSpeech.LANG_MISSING_DATA:
                return "MISSING_DATA";
            case TextToSpeech.LANG_NOT_SUPPORTED:
                return "NOT_SUPPORTED";
            default:
                return "UNKNOWN(" + status + ")";
        }
    }
    
    // Production build - test methods removed
    
    private void loadCombosFromCSV() {
        try {
            InputStream inputStream = context.getAssets().open("boxing_combos_300.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                // Skip header row
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                String[] parts = line.split(",", 2); // Split into max 2 parts
                if (parts.length >= 2) {
                    String id = parts[0].trim();
                    String sequence = parts[1].trim();
                    combos.add(new BoxingCombo(id, sequence));
                }
            }
            
            reader.close();
            inputStream.close();
            
            Log.d(TAG, "Loaded " + combos.size() + " boxing combos from CSV");
            
        } catch (IOException e) {
            Log.e(TAG, "Error loading combos from CSV", e);
        }
    }
    
    public BoxingCombo getRandomCombo() {
        if (combos.isEmpty()) {
            return new BoxingCombo("Default", "Jab - Cross - Hook");
        }
        
        int randomIndex = random.nextInt(combos.size());
        return combos.get(randomIndex);
    }
    
    public void announceRandomCombo() {
        Log.d(TAG, "announceRandomCombo() called");
        
        if (!isAnnouncementEnabled()) {
            Log.d(TAG, "Combo announcements disabled in settings");
            return;
        }
        
        if (!ttsInitialized) {
            Log.w(TAG, "TTS not initialized, cannot announce combo");
            return;
        }
        
        BoxingCombo combo = getRandomCombo();
        announceCombo(combo);
    }
    
    public void announceCombo(BoxingCombo combo) {
        Log.d(TAG, "announceCombo() called for: " + combo.sequence);
        
        if (!ttsInitialized) {
            Log.w(TAG, "TTS not initialized");
            return;
        }
        
        if (!isAnnouncementEnabled()) {
            Log.d(TAG, "Announcements disabled");
            return;
        }
        
        // Format the announcement for better speech
        String announcement = formatComboForSpeech(combo.sequence);
        
        Log.d(TAG, "Announcing combo: " + announcement);
        
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "combo_" + combo.id);
        
        int result = textToSpeech.speak(announcement, TextToSpeech.QUEUE_FLUSH, params);
        Log.d(TAG, "TTS speak() result: " + result);
        
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "TTS speak() returned ERROR");
        }
    }
    
    private String formatComboForSpeech(String sequence) {
        // Replace dashes with pauses and make it more speech-friendly
        String formatted = sequence.replace(" - ", ", then ");
        
        // Add powerful boxing coach energy to the announcement
        String[] powerfulIntros = {
            "Execute combo: ",
            "Now throw: ",
            "Hit them with: ",
            "Fire combo: ",
            "Next sequence: "
        };
        
        String[] powerfulEndors = {
            " Make it count!",
            " Put power behind it!",
            " Stay sharp!",
            " Keep your guard up!",
            " Finish strong!"
        };
        
        String intro = powerfulIntros[random.nextInt(powerfulIntros.length)];
        String ending = powerfulEndors[random.nextInt(powerfulEndors.length)];
        
        return intro + formatted + "." + ending;
    }
    
    public boolean isAnnouncementEnabled() {
        boolean enabled = preferences.getBoolean("announce_combos_enabled", true); // Default to TRUE for better UX
        Log.d(TAG, "Announcements enabled: " + enabled);
        return enabled;
    }
    
    public int getComboFrequencySeconds() {
        return preferences.getInt("combo_frequency_seconds", 15);
    }
    
    public int getTotalCombosCount() {
        return combos.size();
    }
    
    public boolean isTTSReady() {
        return ttsInitialized;
    }
    
    public void retryTTSInitialization() {
        Log.d(TAG, "Manual TTS retry requested");
        initializationAttempts = 0;
        ttsInitialized = false;
        ttsInitializing = false;
        
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        
        initializeTTS();
    }
    
    public void release() {
        Log.d(TAG, "Releasing ComboManager resources");
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
            ttsInitialized = false;
        }
    }
    
    // Test method to announce a specific combo by ID
    public void announceComboById(String comboId) {
        for (BoxingCombo combo : combos) {
            if (combo.id.equals(comboId)) {
                announceCombo(combo);
                return;
            }
        }
        Log.w(TAG, "Combo not found: " + comboId);
    }
    
    // Get all combos (for testing or display purposes)
    public List<BoxingCombo> getAllCombos() {
        return new ArrayList<>(combos);
    }
} 