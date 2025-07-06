package com.projectpivot.app;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ComboLibraryActivity extends AppCompatActivity {
    
    private static final String TAG = "ComboLibraryActivity";
    
    private ComboManager comboManager;
    private RecyclerView comboRecyclerView;
    private ComboAdapter comboAdapter;
    private EditText searchEditText;
    
    // Header views
    private TextView totalCombosText;
    private TextView ttsStatusText;
    
    // Filter buttons
    private CardView allFilterButton;
    private CardView jabFilterButton;
    private CardView hookFilterButton;
    private CardView uppercutFilterButton;
    private CardView advancedFilterButton;
    
    private String currentFilter = "all";
    private String currentSearchQuery = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_combo_library);
        
        comboManager = ComboManager.getInstance(this);
        
        initViews();
        setupRecyclerView();
        setupClickListeners();
        setupSearch();
        loadComboData();
        // Production build - entrance animations removed
    }
    
    private void initViews() {
        // Header views
        totalCombosText = findViewById(R.id.totalCombosText);
        ttsStatusText = findViewById(R.id.ttsStatusText);
        
        // Search
        searchEditText = findViewById(R.id.searchEditText);
        
        // Filter buttons
        allFilterButton = findViewById(R.id.allFilterButton);
        jabFilterButton = findViewById(R.id.jabFilterButton);
        hookFilterButton = findViewById(R.id.hookFilterButton);
        uppercutFilterButton = findViewById(R.id.uppercutFilterButton);
        advancedFilterButton = findViewById(R.id.advancedFilterButton);
        
        // RecyclerView
        comboRecyclerView = findViewById(R.id.comboRecyclerView);
    }
    
    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1);
        comboRecyclerView.setLayoutManager(layoutManager);
        
        // Ensure all combos are shown by default
        comboAdapter = new ComboAdapter(new ArrayList<>(), this::onComboSelected);
        comboRecyclerView.setAdapter(comboAdapter);
        
        // Fix for RecyclerView in ScrollView - ensure it shows all items
        comboRecyclerView.setHasFixedSize(false);
        comboRecyclerView.setNestedScrollingEnabled(false);
        

    }
    
    private void setupClickListeners() {
        // Back button
        findViewById(R.id.backButton).setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            finish();
            // Removed annoying transition animation
        });
        
        // TTS test button
        findViewById(R.id.ttsTestButton).setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            testTTS();
        });
        
        // Filter buttons
        allFilterButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            applyFilter("all");
        });
        jabFilterButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            applyFilter("jab");
        });
        hookFilterButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            applyFilter("hook");
        });
        uppercutFilterButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            applyFilter("uppercut");
        });
        advancedFilterButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            applyFilter("advanced");
        });
    }
    
    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                applyCurrentFilters();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void loadComboData() {
        // Load combo data
        List<ComboManager.BoxingCombo> allCombos = comboManager.getAllCombos();
        

        totalCombosText.setText(String.valueOf(allCombos.size()));
        
        // Update TTS status
        boolean ttsReady = comboManager.isTTSReady();
        boolean ttsEnabled = comboManager.isAnnouncementEnabled();
        
        String ttsStatus;
        if (ttsReady && ttsEnabled) {
            ttsStatus = "TTS Ready ✅";
        } else if (ttsReady && !ttsEnabled) {
            ttsStatus = "TTS Ready (Disabled) ⚠️";
        } else {
            ttsStatus = "TTS Not Ready ❌";
        }
        
        ttsStatusText.setText(ttsStatus);
        ttsStatusText.setTextColor(getResources().getColor(
            (ttsReady && ttsEnabled) ? R.color.accent_green : R.color.error_red));
        
        // Load combos with current filter
        applyFilter(currentFilter);
        

    }
    
    private void applyFilter(String filter) {
        currentFilter = filter;
        
        // Update filter button states
        resetFilterButtons();
        setActiveFilterButton(filter);
        
        // Apply current filters
        applyCurrentFilters();
    }
    
    private void applyCurrentFilters() {
        List<ComboManager.BoxingCombo> allCombos = comboManager.getAllCombos();
        List<ComboManager.BoxingCombo> filtered = new ArrayList<>();
        

        
        for (ComboManager.BoxingCombo combo : allCombos) {
            // Apply category filter
            boolean matchesFilter = false;
            switch (currentFilter) {
                case "all":
                    matchesFilter = true;
                    break;
                case "jab":
                    matchesFilter = combo.sequence.toLowerCase().contains("jab") || 
                                  combo.sequence.toLowerCase().contains("1");
                    break;
                case "hook":
                    matchesFilter = combo.sequence.toLowerCase().contains("hook") || 
                                  combo.sequence.toLowerCase().contains("3") ||
                                  combo.sequence.toLowerCase().contains("4");
                    break;
                case "uppercut":
                    matchesFilter = combo.sequence.toLowerCase().contains("uppercut") || 
                                  combo.sequence.toLowerCase().contains("5") ||
                                  combo.sequence.toLowerCase().contains("6");
                    break;
                case "advanced":
                    // Advanced combos have more than 4 punches or special moves
                    String[] parts = combo.sequence.split("[,-]");
                    matchesFilter = parts.length > 4 || 
                                  combo.sequence.toLowerCase().contains("slip") ||
                                  combo.sequence.toLowerCase().contains("duck") ||
                                  combo.sequence.toLowerCase().contains("pivot");
                    break;
            }
            
            // Apply search filter
            boolean matchesSearch = currentSearchQuery.isEmpty() || 
                                  combo.sequence.toLowerCase().contains(currentSearchQuery) ||
                                  combo.id.toLowerCase().contains(currentSearchQuery);
            
            if (matchesFilter && matchesSearch) {
                filtered.add(combo);
            }
        }
        

        
        // Update adapter
        comboAdapter.updateCombos(filtered);
        
        // Update results count
        findViewById(R.id.resultsCountText).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.resultsCountText)).setText(
            filtered.size() + " combos found");
    }
    
    private void resetFilterButtons() {
        setFilterButtonInactive(allFilterButton);
        setFilterButtonInactive(jabFilterButton);
        setFilterButtonInactive(hookFilterButton);
        setFilterButtonInactive(uppercutFilterButton);
        setFilterButtonInactive(advancedFilterButton);
    }
    
    private void setActiveFilterButton(String filter) {
        CardView activeButton = null;
        
        switch (filter) {
            case "all":
                activeButton = allFilterButton;
                break;
            case "jab":
                activeButton = jabFilterButton;
                break;
            case "hook":
                activeButton = hookFilterButton;
                break;
            case "uppercut":
                activeButton = uppercutFilterButton;
                break;
            case "advanced":
                activeButton = advancedFilterButton;
                break;
        }
        
        if (activeButton != null) {
            activeButton.setCardBackgroundColor(getResources().getColor(R.color.accent_green));
            TextView textView = (TextView) activeButton.getChildAt(0);
            textView.setTextColor(getResources().getColor(R.color.background_primary));
        }
    }
    
    private void setFilterButtonInactive(CardView button) {
        button.setCardBackgroundColor(getResources().getColor(R.color.background_tertiary));
        TextView textView = (TextView) button.getChildAt(0);
        textView.setTextColor(getResources().getColor(R.color.text_secondary));
    }
    
    private void onComboSelected(ComboManager.BoxingCombo combo) {
        
        // Play the combo announcement
        if (comboManager.isTTSReady()) {
            if (comboManager.isAnnouncementEnabled()) {
                comboManager.announceCombo(combo);
                Toast.makeText(this, "🔊 Playing: " + combo.sequence, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "🔊 TTS disabled. Enable in Settings.", Toast.LENGTH_SHORT).show();
                // Still announce it for testing
                comboManager.announceCombo(combo);
            }
        } else {
            Toast.makeText(this, "❌ TTS not ready. Retrying initialization...", Toast.LENGTH_SHORT).show();
            // Try to reinitialize TTS
            comboManager.retryTTSInitialization();
        }
    }
    
    // Production build - test methods removed
    
    // Production build - entrance animations removed
    
    private void testTTS() {
        
        if (comboManager.isTTSReady()) {
            // Test with a simple combo
            ComboManager.BoxingCombo testCombo = new ComboManager.BoxingCombo("TEST", "Jab - Cross - Hook");
            comboManager.announceCombo(testCombo);
            Toast.makeText(this, "🔊 Testing TTS: Jab - Cross - Hook", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "❌ TTS not ready. Retrying initialization...", Toast.LENGTH_LONG).show();
            comboManager.retryTTSInitialization();
            
            // Refresh status after a delay
            new android.os.Handler().postDelayed(() -> {
                loadComboData();
            }, 2000);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh TTS status when returning
        loadComboData();
    }
} 