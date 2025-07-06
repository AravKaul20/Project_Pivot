package com.projectpivot.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.*;

public class AchievementManager {
    
    private static final String TAG = "AchievementManager";
    private static final String PREFS_NAME = "achievements";
    private static AchievementManager instance;
    
    private Context context;
    private SharedPreferences preferences;
    private SessionTracker sessionTracker;
    private List<Achievement> allAchievements;
    
    public static class Achievement {
        public String id;
        public String title;
        public String description;
        public String icon;
        public String category; // "consistency", "accuracy", "volume", "milestone"
        public int targetValue;
        public String condition; // "sessions", "streak", "accuracy", "punches", "time"
        public boolean isUnlocked;
        public long unlockedDate;
        public int progress;
        public String reward; // XP, badge, etc.
        
        public Achievement(String id, String title, String description, String icon, 
                         String category, int targetValue, String condition, String reward) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.icon = icon;
            this.category = category;
            this.targetValue = targetValue;
            this.condition = condition;
            this.reward = reward;
            this.isUnlocked = false;
            this.unlockedDate = 0;
            this.progress = 0;
        }
        
        public int getProgressPercentage() {
            return Math.min(100, (progress * 100) / targetValue);
        }
    }
    
    private AchievementManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.sessionTracker = SessionTracker.getInstance(context);
        initializeAchievements();
        loadAchievementProgress();
    }
    
    public static synchronized AchievementManager getInstance(Context context) {
        if (instance == null) {
            instance = new AchievementManager(context);
        }
        return instance;
    }
    
    private void initializeAchievements() {
        allAchievements = new ArrayList<>();
        
        // Consistency Achievements
        allAchievements.add(new Achievement(
            "first_workout", "First Steps", "Complete your first workout", "🥊",
            "milestone", 1, "sessions", "50 XP"
        ));
        
        allAchievements.add(new Achievement(
            "streak_3", "Getting Started", "Maintain a 3-day workout streak", "🔥",
            "consistency", 3, "streak", "100 XP"
        ));
        
        allAchievements.add(new Achievement(
            "streak_7", "Week Warrior", "Maintain a 7-day workout streak", "⚡",
            "consistency", 7, "streak", "200 XP"
        ));
        
        allAchievements.add(new Achievement(
            "streak_30", "Monthly Master", "Maintain a 30-day workout streak", "🏆",
            "consistency", 30, "streak", "500 XP"
        ));
        
        allAchievements.add(new Achievement(
            "streak_100", "Century Champion", "Maintain a 100-day workout streak", "👑",
            "consistency", 100, "streak", "1000 XP"
        ));
        
        // Volume Achievements
        allAchievements.add(new Achievement(
            "sessions_10", "Dedicated Boxer", "Complete 10 workout sessions", "🥇",
            "volume", 10, "sessions", "150 XP"
        ));
        
        allAchievements.add(new Achievement(
            "sessions_50", "Training Veteran", "Complete 50 workout sessions", "🎖️",
            "volume", 50, "sessions", "300 XP"
        ));
        
        allAchievements.add(new Achievement(
            "sessions_100", "Boxing Pro", "Complete 100 workout sessions", "🏅",
            "volume", 100, "sessions", "600 XP"
        ));
        
        allAchievements.add(new Achievement(
            "sessions_250", "Elite Athlete", "Complete 250 workout sessions", "💎",
            "volume", 250, "sessions", "1000 XP"
        ));
        
        // Punch Volume Achievements
        allAchievements.add(new Achievement(
            "punches_100", "Hundred Puncher", "Throw 100 total punches", "👊",
            "volume", 100, "punches", "100 XP"
        ));
        
        allAchievements.add(new Achievement(
            "punches_1000", "Thousand Striker", "Throw 1,000 total punches", "💥",
            "volume", 1000, "punches", "250 XP"
        ));
        
        allAchievements.add(new Achievement(
            "punches_5000", "Heavy Hitter", "Throw 5,000 total punches", "⚡",
            "volume", 5000, "punches", "500 XP"
        ));
        
        allAchievements.add(new Achievement(
            "punches_10000", "Punch Master", "Throw 10,000 total punches", "🔥",
            "volume", 10000, "punches", "1000 XP"
        ));
        
        // Accuracy Achievements
        allAchievements.add(new Achievement(
            "accuracy_80", "Precise Striker", "Achieve 80% average accuracy", "🎯",
            "accuracy", 80, "accuracy", "200 XP"
        ));
        
        allAchievements.add(new Achievement(
            "accuracy_90", "Sharp Shooter", "Achieve 90% average accuracy", "🏹",
            "accuracy", 90, "accuracy", "400 XP"
        ));
        
        allAchievements.add(new Achievement(
            "accuracy_95", "Perfect Form", "Achieve 95% average accuracy", "✨",
            "accuracy", 95, "accuracy", "800 XP"
        ));
        
        // Time-based Achievements
        allAchievements.add(new Achievement(
            "time_1hour", "Hour Hero", "Complete 1 hour of total training", "⏰",
            "volume", 60, "time", "150 XP"
        ));
        
        allAchievements.add(new Achievement(
            "time_10hours", "Time Warrior", "Complete 10 hours of total training", "⌚",
            "volume", 600, "time", "300 XP"
        ));
        
        allAchievements.add(new Achievement(
            "time_50hours", "Training Machine", "Complete 50 hours of total training", "🤖",
            "volume", 3000, "time", "750 XP"
        ));
        
        // Special Achievements
        allAchievements.add(new Achievement(
            "perfect_session", "Flawless Victory", "Complete a session with 100% accuracy", "⭐",
            "accuracy", 100, "perfect", "300 XP"
        ));
        
        allAchievements.add(new Achievement(
            "marathon_session", "Marathon Boxer", "Complete a 30-minute session", "🏃",
            "volume", 30, "session_duration", "250 XP"
        ));
        
        allAchievements.add(new Achievement(
            "early_bird", "Early Bird", "Complete a workout before 7 AM", "🌅",
            "special", 1, "early_workout", "200 XP"
        ));
        
        allAchievements.add(new Achievement(
            "night_owl", "Night Owl", "Complete a workout after 10 PM", "🌙",
            "special", 1, "late_workout", "200 XP"
        ));
    }
    
    private void loadAchievementProgress() {
        for (Achievement achievement : allAchievements) {
            achievement.isUnlocked = preferences.getBoolean(achievement.id + "_unlocked", false);
            achievement.unlockedDate = preferences.getLong(achievement.id + "_date", 0);
            achievement.progress = preferences.getInt(achievement.id + "_progress", 0);
        }
        
        // Production ready - no auto-unlock testing code
    }
    
    private void saveAchievement(Achievement achievement) {
        preferences.edit()
            .putBoolean(achievement.id + "_unlocked", achievement.isUnlocked)
            .putLong(achievement.id + "_date", achievement.unlockedDate)
            .putInt(achievement.id + "_progress", achievement.progress)
            .apply();
    }
    
    public List<Achievement> checkForNewAchievements() {
        List<Achievement> newlyUnlocked = new ArrayList<>();
        updateAllProgress();
        
        for (Achievement achievement : allAchievements) {
            if (!achievement.isUnlocked && achievement.progress >= achievement.targetValue) {
                achievement.isUnlocked = true;
                achievement.unlockedDate = System.currentTimeMillis();
                saveAchievement(achievement);
                newlyUnlocked.add(achievement);
                
                Log.d(TAG, "Achievement unlocked: " + achievement.title);
                
                // Award XP
                awardXP(achievement);
                
                // Show notification
                showAchievementNotification(achievement);
            }
        }
        
        return newlyUnlocked;
    }
    
    private void updateAllProgress() {
        List<SessionTracker.WorkoutSession> sessions = sessionTracker.getAllSessions();
        
        for (Achievement achievement : allAchievements) {
            if (achievement.isUnlocked) continue;
            
            int newProgress = calculateProgress(achievement, sessions);
            if (newProgress != achievement.progress) {
                achievement.progress = newProgress;
                saveAchievement(achievement);
            }
        }
    }
    
    private int calculateProgress(Achievement achievement, List<SessionTracker.WorkoutSession> sessions) {
        switch (achievement.condition) {
            case "sessions":
                return sessions.size();
                
            case "streak":
                return sessionTracker.getCurrentStreak();
                
            case "punches":
                return sessions.stream().mapToInt(s -> s.totalPunches).sum();
                
            case "time":
                return (int) (sessionTracker.getTotalWorkoutTime() / (1000 * 60)); // minutes
                
            case "accuracy":
                float avgAccuracy = (sessionTracker.getOverallStanceAccuracy() + 
                                   sessionTracker.getOverallExecutionAccuracy()) / 2 * 100;
                return (int) avgAccuracy;
                
            case "perfect":
                return (int) sessions.stream()
                    .filter(s -> (s.stanceAccuracy + s.executionAccuracy) / 2 >= 1.0f)
                    .count();
                    
            case "session_duration":
                return (int) sessions.stream()
                    .mapToLong(s -> s.workoutDuration / (1000 * 60))
                    .max().orElse(0);
                    
            case "early_workout":
                Calendar calEarly = Calendar.getInstance();
                return (int) sessions.stream()
                    .filter(s -> {
                        calEarly.setTimeInMillis(s.startTime);
                        return calEarly.get(Calendar.HOUR_OF_DAY) < 7;
                    })
                    .count();
                    
            case "late_workout":
                Calendar calLate = Calendar.getInstance();
                return (int) sessions.stream()
                    .filter(s -> {
                        calLate.setTimeInMillis(s.startTime);
                        return calLate.get(Calendar.HOUR_OF_DAY) >= 22;
                    })
                    .count();
                    
            default:
                return 0;
        }
    }
    
    private void awardXP(Achievement achievement) {
        int currentXP = preferences.getInt("total_xp", 0);
        int xpReward = Integer.parseInt(achievement.reward.replaceAll("[^0-9]", ""));
        int newXP = currentXP + xpReward;
        
        preferences.edit().putInt("total_xp", newXP).apply();
        
        Log.d(TAG, "Awarded " + xpReward + " XP for " + achievement.title);
    }
    
    private void showAchievementNotification(Achievement achievement) {
        // Use existing notification system with enhanced haptic feedback
        SoundManager soundManager = SoundManager.getInstance(context);
        soundManager.playMotivationalSound();
        soundManager.hapticSuccess(); // Enhanced success haptic pattern
        
        // Achievement notification system
    }
    
    public List<Achievement> getAllAchievements() {
        return new ArrayList<>(allAchievements);
    }
    
    public List<Achievement> getUnlockedAchievements() {
        return allAchievements.stream()
            .filter(a -> a.isUnlocked)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public List<Achievement> getAchievementsByCategory(String category) {
        return allAchievements.stream()
            .filter(a -> a.category.equals(category))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public int getTotalXP() {
        return preferences.getInt("total_xp", 0);
    }
    
    public int getLevel() {
        int xp = getTotalXP();
        // Simple level calculation: Level = sqrt(XP / 100)
        return (int) Math.sqrt(xp / 100.0) + 1;
    }
    
    public int getXPForNextLevel() {
        int currentLevel = getLevel();
        int xpForNextLevel = (currentLevel * currentLevel) * 100;
        return xpForNextLevel - getTotalXP();
    }
    
    public float getLevelProgress() {
        int currentLevel = getLevel();
        int xpForCurrentLevel = ((currentLevel - 1) * (currentLevel - 1)) * 100;
        int xpForNextLevel = (currentLevel * currentLevel) * 100;
        int currentXP = getTotalXP();
        
        if (xpForNextLevel == xpForCurrentLevel) return 1.0f;
        
        return (float) (currentXP - xpForCurrentLevel) / (xpForNextLevel - xpForCurrentLevel);
    }
    
    public Map<String, Integer> getAchievementStats() {
        Map<String, Integer> stats = new HashMap<>();
        
        int total = allAchievements.size();
        int unlocked = (int) allAchievements.stream().filter(a -> a.isUnlocked).count();
        
        stats.put("total", total);
        stats.put("unlocked", unlocked);
        stats.put("locked", total - unlocked);
        stats.put("completion_percentage", total > 0 ? (unlocked * 100) / total : 0);
        
        // Category breakdown
        for (String category : Arrays.asList("consistency", "accuracy", "volume", "milestone", "special")) {
            int categoryTotal = (int) allAchievements.stream().filter(a -> a.category.equals(category)).count();
            int categoryUnlocked = (int) allAchievements.stream()
                .filter(a -> a.category.equals(category) && a.isUnlocked).count();
            
            stats.put(category + "_total", categoryTotal);
            stats.put(category + "_unlocked", categoryUnlocked);
        }
        
        return stats;
    }
} 