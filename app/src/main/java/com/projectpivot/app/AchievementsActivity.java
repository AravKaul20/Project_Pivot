package com.projectpivot.app;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AchievementsActivity extends AppCompatActivity {
    
    private static final String TAG = "AchievementsActivity";
    
    private AchievementManager achievementManager;
    private RecyclerView achievementsRecyclerView;
    private AchievementAdapter achievementAdapter;
    
    // Header views
    private TextView levelText;
    private TextView xpText;
    private ProgressBar levelProgressBar;
    private TextView progressText;
    
    // Stats views
    private TextView totalAchievementsText;
    private TextView unlockedAchievementsText;
    private TextView completionPercentageText;
    
    // Filter buttons
    private CardView allFilterButton;
    private CardView unlockedFilterButton;
    private CardView consistencyFilterButton;
    private CardView volumeFilterButton;
    private CardView accuracyFilterButton;
    private CardView milestoneFilterButton;
    private CardView specialFilterButton;
    
    private String currentFilter = "all";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);
        
        achievementManager = AchievementManager.getInstance(this);
        
        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadAchievementData();
        animateProgressBars();
    }
    
    private void initViews() {
        // Header views
        levelText = findViewById(R.id.levelText);
        xpText = findViewById(R.id.xpText);
        levelProgressBar = findViewById(R.id.levelProgressBar);
        progressText = findViewById(R.id.progressText);
        
        // Stats views
        totalAchievementsText = findViewById(R.id.totalAchievementsText);
        unlockedAchievementsText = findViewById(R.id.unlockedAchievementsText);
        completionPercentageText = findViewById(R.id.completionPercentageText);
        
        // Filter buttons
        allFilterButton = findViewById(R.id.allFilterButton);
        unlockedFilterButton = findViewById(R.id.unlockedFilterButton);
        consistencyFilterButton = findViewById(R.id.consistencyFilterButton);
        volumeFilterButton = findViewById(R.id.volumeFilterButton);
        accuracyFilterButton = findViewById(R.id.accuracyFilterButton);
        milestoneFilterButton = findViewById(R.id.milestoneFilterButton);
        specialFilterButton = findViewById(R.id.specialFilterButton);
        
        // RecyclerView
        achievementsRecyclerView = findViewById(R.id.achievementsRecyclerView);
    }
    
    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1);
        achievementsRecyclerView.setLayoutManager(layoutManager);
        
        achievementAdapter = new AchievementAdapter(new ArrayList<>());
        achievementsRecyclerView.setAdapter(achievementAdapter);
    }
    
    private void setupClickListeners() {
        // Back button
        findViewById(R.id.backButton).setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            finish();
            // Removed annoying transition animation
        });
        
        // Filter buttons
        allFilterButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            applyFilter("all");
        });
        unlockedFilterButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            applyFilter("unlocked");
        });
        consistencyFilterButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            applyFilter("consistency");
        });
        volumeFilterButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            applyFilter("volume");
        });
        accuracyFilterButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            applyFilter("accuracy");
        });
        milestoneFilterButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            applyFilter("milestone");
        });
        specialFilterButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            applyFilter("special");
        });
    }
    
    private void loadAchievementData() {
        // Check for new achievements first
        achievementManager.checkForNewAchievements();
        
        // Load level and XP data
        int currentLevel = achievementManager.getLevel();
        int totalXP = achievementManager.getTotalXP();
        float levelProgress = achievementManager.getLevelProgress();
        int xpForNext = achievementManager.getXPForNextLevel();
        
        levelText.setText("LEVEL " + currentLevel);
        xpText.setText(totalXP + " XP");
        progressText.setText(String.format("%d / %d XP", totalXP % 1000, xpForNext));
        
        // Set progress bar (animate later)
        levelProgressBar.setProgress((int)(levelProgress * 100));
        
        // Load achievement stats
        List<AchievementManager.Achievement> allAchievements = achievementManager.getAllAchievements();
        List<AchievementManager.Achievement> unlockedAchievements = achievementManager.getUnlockedAchievements();
        
        // Production build - debug logging removed
        
        totalAchievementsText.setText(String.valueOf(allAchievements.size()));
        unlockedAchievementsText.setText(String.valueOf(unlockedAchievements.size()));
        
        int completionPercentage = allAchievements.size() > 0 ? 
            (unlockedAchievements.size() * 100) / allAchievements.size() : 0;
        completionPercentageText.setText(completionPercentage + "%");
        
        // Load achievements with current filter
        applyFilter(currentFilter);
    }
    
    private void applyFilter(String filter) {
        currentFilter = filter;
        
        // Update filter button states
        resetFilterButtons();
        setActiveFilterButton(filter);
        
        // Get filtered achievements
        List<AchievementManager.Achievement> filteredAchievements = getFilteredAchievements(filter);
        
        // Update adapter
        // Production build - debug logging removed
        achievementAdapter.updateAchievements(filteredAchievements);
    }
    
    private List<AchievementManager.Achievement> getFilteredAchievements(String filter) {
        List<AchievementManager.Achievement> allAchievements = achievementManager.getAllAchievements();
        List<AchievementManager.Achievement> filtered = new ArrayList<>();
        
        for (AchievementManager.Achievement achievement : allAchievements) {
            switch (filter) {
                case "all":
                    filtered.add(achievement);
                    break;
                case "unlocked":
                    if (achievement.isUnlocked) {
                        filtered.add(achievement);
                    }
                    break;
                case "consistency":
                    if ("consistency".equals(achievement.category)) {
                        filtered.add(achievement);
                    }
                    break;
                case "volume":
                    if ("volume".equals(achievement.category)) {
                        filtered.add(achievement);
                    }
                    break;
                case "accuracy":
                    if ("accuracy".equals(achievement.category)) {
                        filtered.add(achievement);
                    }
                    break;
                case "milestone":
                    if ("milestone".equals(achievement.category)) {
                        filtered.add(achievement);
                    }
                    break;
                case "special":
                    if ("special".equals(achievement.category)) {
                        filtered.add(achievement);
                    }
                    break;
            }
        }
        
        return filtered;
    }
    
    private void resetFilterButtons() {
        setFilterButtonInactive(allFilterButton);
        setFilterButtonInactive(unlockedFilterButton);
        setFilterButtonInactive(consistencyFilterButton);
        setFilterButtonInactive(volumeFilterButton);
        setFilterButtonInactive(accuracyFilterButton);
        setFilterButtonInactive(milestoneFilterButton);
        setFilterButtonInactive(specialFilterButton);
    }
    
    private void setActiveFilterButton(String filter) {
        CardView activeButton = null;
        
        switch (filter) {
            case "all":
                activeButton = allFilterButton;
                break;
            case "unlocked":
                activeButton = unlockedFilterButton;
                break;
            case "consistency":
                activeButton = consistencyFilterButton;
                break;
            case "volume":
                activeButton = volumeFilterButton;
                break;
            case "accuracy":
                activeButton = accuracyFilterButton;
                break;
            case "milestone":
                activeButton = milestoneFilterButton;
                break;
            case "special":
                activeButton = specialFilterButton;
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
    
    private void animateProgressBars() {
        // Animate level progress bar
        ValueAnimator progressAnimator = ValueAnimator.ofInt(0, levelProgressBar.getProgress());
        progressAnimator.setDuration(1000);
        progressAnimator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            levelProgressBar.setProgress(animatedValue);
        });
        progressAnimator.start();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to achievements
        loadAchievementData();
    }
} 