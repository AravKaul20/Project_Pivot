package com.projectpivot.app;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SessionTracker {
    
    private static final String TAG = "SessionTracker";
    private static SessionTracker instance;
    
    private Context context;
    private SharedPreferences preferences;
    private Gson gson;
    private FirestoreManager firestoreManager;
    
    // Current session
    private WorkoutSession currentSession;
    private AccuracyTracker accuracyTracker;
    private boolean sessionActive = false;
    
    public static class WorkoutSession {
        public String sessionId;
        public long startTime;
        public long endTime;
        public String mode; // "stance", "execution", "both"
        public float stanceAccuracy;
        public float executionAccuracy;
        public int totalPunches;
        public long workoutDuration; // milliseconds
        public int stanceMeasurements;
        public int executionMeasurements;
        public String date; // formatted date string
        
        public WorkoutSession() {
            this.sessionId = "session_" + System.currentTimeMillis();
            this.startTime = System.currentTimeMillis();
            this.mode = "both";
            this.stanceAccuracy = 0f;
            this.executionAccuracy = 0f;
            this.totalPunches = 0;
            this.workoutDuration = 0;
            this.stanceMeasurements = 0;
            this.executionMeasurements = 0;
            
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            this.date = sdf.format(new Date(this.startTime));
        }
        
        public String getFormattedDuration() {
            long seconds = workoutDuration / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            
            if (minutes > 0) {
                return String.format(Locale.getDefault(), "%dm %ds", minutes, seconds);
            } else {
                return String.format(Locale.getDefault(), "%ds", seconds);
            }
        }
        
        public String getFormattedAccuracy() {
            if (mode.equals("stance")) {
                return String.format(Locale.getDefault(), "%.1f%%", stanceAccuracy * 100);
            } else if (mode.equals("execution")) {
                return String.format(Locale.getDefault(), "%.1f%%", executionAccuracy * 100);
            } else {
                float avgAccuracy = (stanceAccuracy + executionAccuracy) / 2f;
                return String.format(Locale.getDefault(), "%.1f%%", avgAccuracy * 100);
            }
        }
    }
    
    private SessionTracker(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        this.accuracyTracker = new AccuracyTracker();
        this.firestoreManager = FirestoreManager.getInstance(context);
        updatePreferencesForCurrentUser();
    }
    
    private void updatePreferencesForCurrentUser() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = currentUser != null ? currentUser.getUid() : "local_user";
        String prefsName = "PROJECT_PIVOT_SESSIONS_" + userId;
        this.preferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
    }
    
    // Call this method when user signs in/out to update storage
    public void refreshUserSession() {
        updatePreferencesForCurrentUser();
        
        // Sync any existing local sessions to Firestore when user signs in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            firestoreManager.syncLocalSessionsToFirestore();
        }
    }
    
    public static synchronized SessionTracker getInstance(Context context) {
        if (instance == null) {
            instance = new SessionTracker(context);
        }
        return instance;
    }
    
    public void startSession(String mode) {
        if (sessionActive) {
            endSession();
        }
        
        currentSession = new WorkoutSession();
        currentSession.mode = mode;
        accuracyTracker.resetSession();
        sessionActive = true;
    }
    
    public void endSession() {
        if (!sessionActive || currentSession == null) {
            return;
        }
        
        // Finalize session data
        currentSession.endTime = System.currentTimeMillis();
        currentSession.workoutDuration = currentSession.endTime - currentSession.startTime;
        
        // Get accuracy data
        currentSession.stanceAccuracy = accuracyTracker.calculateSessionStanceAccuracy();
        currentSession.executionAccuracy = accuracyTracker.calculateSessionExecutionAccuracy();
        currentSession.stanceMeasurements = accuracyTracker.getStanceMeasurementCount();
        currentSession.executionMeasurements = accuracyTracker.getExecutionMeasurementCount();
        
        // Get punch data
        PunchAnalyzer punchAnalyzer = PunchAnalyzer.getInstance(context);
        PunchAnalyzer.PunchData punchData = punchAnalyzer.getCurrentData();
        currentSession.totalPunches = punchData.totalPunches;
        
        // Save session
        saveSession(currentSession);
        updateOverallStats(currentSession);
        
        sessionActive = false;
        currentSession = null;
    }
    
    public void recordStanceAccuracy(float[] logits, String prediction) {
        if (sessionActive && accuracyTracker != null) {
            accuracyTracker.recordStanceAccuracy(logits, prediction);
        }
    }
    
    public void recordExecutionAccuracy(float[] logits, String prediction) {
        if (sessionActive && accuracyTracker != null) {
            accuracyTracker.recordExecutionAccuracy(logits, prediction);
        }
    }
    
    private void saveSession(WorkoutSession session) {
        List<WorkoutSession> sessions = getAllSessions();
        sessions.add(0, session); // Add to beginning for recent-first order
        
        // Keep only last 50 sessions to manage storage
        if (sessions.size() > 50) {
            sessions = sessions.subList(0, 50);
        }
        
        String sessionsJson = gson.toJson(sessions);
        preferences.edit().putString("workout_sessions", sessionsJson).apply();
        
        // Also save to Firestore cloud storage
        firestoreManager.saveWorkoutSession(session);
    }
    
    private void updateOverallStats(WorkoutSession session) {
        SharedPreferences.Editor editor = preferences.edit();
        
        // Update total sessions
        int totalSessions = preferences.getInt("total_sessions", 0) + 1;
        editor.putInt("total_sessions", totalSessions);
        
        // Update total workout time
        long totalWorkoutTime = preferences.getLong("total_workout_time", 0) + session.workoutDuration;
        editor.putLong("total_workout_time", totalWorkoutTime);
        
        // Update best accuracies
        float bestStanceAccuracy = Math.max(preferences.getFloat("best_stance_accuracy", 0f), session.stanceAccuracy);
        float bestExecutionAccuracy = Math.max(preferences.getFloat("best_execution_accuracy", 0f), session.executionAccuracy);
        editor.putFloat("best_stance_accuracy", bestStanceAccuracy);
        editor.putFloat("best_execution_accuracy", bestExecutionAccuracy);
        
        // Update current streak
        updateWorkoutStreak(session);
        
        // Update last session timestamp
        editor.putLong("last_session_timestamp", session.endTime);
        
        editor.apply();
    }
    
    private void updateWorkoutStreak(WorkoutSession session) {
        // Calculate daily streak based on workout dates
        List<WorkoutSession> allSessions = getAllSessions();
        
        // Get unique workout dates (including today's session)
        Set<String> workoutDates = new HashSet<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        // Add all existing session dates
        for (WorkoutSession s : allSessions) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(s.endTime);
            workoutDates.add(dateFormat.format(cal.getTime()));
        }
        
        // Add current session date
        Calendar todayCal = Calendar.getInstance();
        todayCal.setTimeInMillis(session.endTime);
        workoutDates.add(dateFormat.format(todayCal.getTime()));
        
        // Calculate current daily streak
        int currentStreak = calculateDailyStreak(workoutDates, dateFormat.format(todayCal.getTime()));
        
        // Update best streak
        int bestStreak = Math.max(preferences.getInt("best_workout_streak", 0), currentStreak);
        
        preferences.edit()
            .putInt("workout_streak", currentStreak)
            .putInt("best_workout_streak", bestStreak)
            .apply();
    }
    
    private int calculateDailyStreak(Set<String> workoutDates, String todayDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        
        try {
            cal.setTime(dateFormat.parse(todayDate));
        } catch (ParseException e) {
            return 1;
        }
        
        int streak = 0;
        
        // Check backwards from today
        for (int i = 0; i < 365; i++) { // Max 365 days to prevent infinite loop
            String checkDate = dateFormat.format(cal.getTime());
            
            if (workoutDates.contains(checkDate)) {
                streak++;
                // Move to previous day
                cal.add(Calendar.DAY_OF_YEAR, -1);
            } else {
                // Streak broken
                break;
            }
        }
        
        return streak;
    }
    
    public List<WorkoutSession> getAllSessions() {
        String sessionsJson = preferences.getString("workout_sessions", "[]");
        Type listType = new TypeToken<List<WorkoutSession>>(){}.getType();
        
        try {
            return gson.fromJson(sessionsJson, listType);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    public List<WorkoutSession> getRecentSessions(int count) {
        List<WorkoutSession> allSessions = getAllSessions();
        return allSessions.subList(0, Math.min(count, allSessions.size()));
    }
    
    public WorkoutSession getCurrentSession() {
        return currentSession;
    }
    
    public boolean isSessionActive() {
        return sessionActive;
    }
    
    // Statistics methods
    public float getOverallStanceAccuracy() {
        List<WorkoutSession> sessions = getAllSessions();
        if (sessions.isEmpty()) return 0f;
        
        float totalAccuracy = 0f;
        int validSessions = 0;
        
        for (WorkoutSession session : sessions) {
            if (session.stanceMeasurements > 0) {
                totalAccuracy += session.stanceAccuracy;
                validSessions++;
            }
        }
        
        return validSessions > 0 ? totalAccuracy / validSessions : 0f;
    }
    
    public float getOverallExecutionAccuracy() {
        List<WorkoutSession> sessions = getAllSessions();
        if (sessions.isEmpty()) return 0f;
        
        float totalAccuracy = 0f;
        int validSessions = 0;
        
        for (WorkoutSession session : sessions) {
            if (session.executionMeasurements > 0) {
                totalAccuracy += session.executionAccuracy;
                validSessions++;
            }
        }
        
        return validSessions > 0 ? totalAccuracy / validSessions : 0f;
    }
    
    public int getTotalSessions() {
        return preferences.getInt("total_sessions", 0);
    }
    
    public int getCurrentStreak() {
        // Calculate current daily streak in real-time
        List<WorkoutSession> allSessions = getAllSessions();
        if (allSessions.isEmpty()) {
            return 0;
        }
        
        // Get unique workout dates
        Set<String> workoutDates = new HashSet<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        for (WorkoutSession session : allSessions) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(session.endTime);
            workoutDates.add(dateFormat.format(cal.getTime()));
        }
        
        // Calculate streak from today
        Calendar today = Calendar.getInstance();
        String todayDate = dateFormat.format(today.getTime());
        
        return calculateDailyStreak(workoutDates, todayDate);
    }
    
    public int getBestStreak() {
        return preferences.getInt("best_workout_streak", 0);
    }
    
    public long getTotalWorkoutTime() {
        return preferences.getLong("total_workout_time", 0);
    }
    
    public String getFormattedTotalWorkoutTime() {
        long totalMs = getTotalWorkoutTime();
        long totalMinutes = totalMs / (1000 * 60);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %dm", hours, minutes);
        } else {
            return String.format(Locale.getDefault(), "%dm", minutes);
        }
    }
    
    public int getWeeklySessions() {
        List<WorkoutSession> sessions = getAllSessions();
        long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
        
        int weeklyCount = 0;
        for (WorkoutSession session : sessions) {
            if (session.endTime >= oneWeekAgo) {
                weeklyCount++;
            }
        }
        
        return weeklyCount;
    }
} 