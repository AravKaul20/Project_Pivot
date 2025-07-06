package com.projectpivot.app;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreManager {
    
    private static final String TAG = "FirestoreManager";
    private static FirestoreManager instance;
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Context context;
    
    private FirestoreManager(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }
    
    public static synchronized FirestoreManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirestoreManager(context);
        }
        return instance;
    }
    
    // Save a workout session to Firestore
    public void saveWorkoutSession(SessionTracker.WorkoutSession session) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No user signed in, cannot save to Firestore");
            return;
        }
        
        String userId = currentUser.getUid();
        
        // Convert session to Map for Firestore
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("sessionId", session.sessionId);
        sessionData.put("startTime", session.startTime);
        sessionData.put("endTime", session.endTime);
        sessionData.put("mode", session.mode);
        sessionData.put("stanceAccuracy", session.stanceAccuracy);
        sessionData.put("executionAccuracy", session.executionAccuracy);
        sessionData.put("totalPunches", session.totalPunches);
        sessionData.put("workoutDuration", session.workoutDuration);
        sessionData.put("stanceMeasurements", session.stanceMeasurements);
        sessionData.put("executionMeasurements", session.executionMeasurements);
        sessionData.put("date", session.date);
        sessionData.put("timestamp", System.currentTimeMillis());
        
        // Save to Firestore
        db.collection("users")
            .document(userId)
            .collection("workoutSessions")
            .document(session.sessionId)
            .set(sessionData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Workout session saved to Firestore: " + session.sessionId);
                updateUserStats(userId);
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error saving workout session to Firestore", e);
            });
    }
    
    // Update user statistics in Firestore
    private void updateUserStats(String userId) {
        db.collection("users")
            .document(userId)
            .collection("workoutSessions")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int totalSessions = queryDocumentSnapshots.size();
                long totalWorkoutTime = 0;
                float totalStanceAccuracy = 0;
                float totalExecutionAccuracy = 0;
                int totalPunches = 0;
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Long duration = document.getLong("workoutDuration");
                    Double stanceAcc = document.getDouble("stanceAccuracy");
                    Double execAcc = document.getDouble("executionAccuracy");
                    Long punches = document.getLong("totalPunches");
                    
                    if (duration != null) totalWorkoutTime += duration;
                    if (stanceAcc != null) totalStanceAccuracy += stanceAcc.floatValue();
                    if (execAcc != null) totalExecutionAccuracy += execAcc.floatValue();
                    if (punches != null) totalPunches += punches.intValue();
                }
                
                // Calculate averages
                float avgStanceAccuracy = totalSessions > 0 ? totalStanceAccuracy / totalSessions : 0;
                float avgExecutionAccuracy = totalSessions > 0 ? totalExecutionAccuracy / totalSessions : 0;
                
                // Update user document with stats
                Map<String, Object> userStats = new HashMap<>();
                userStats.put("totalSessions", totalSessions);
                userStats.put("totalWorkoutTime", totalWorkoutTime);
                userStats.put("averageStanceAccuracy", avgStanceAccuracy);
                userStats.put("averageExecutionAccuracy", avgExecutionAccuracy);
                userStats.put("totalPunches", totalPunches);
                userStats.put("lastUpdated", System.currentTimeMillis());
                
                db.collection("users")
                    .document(userId)
                    .update(userStats)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User stats updated in Firestore");
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error updating user stats", e);
                    });
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error calculating user stats", e);
            });
    }
    
    // Sync local sessions to Firestore
    public void syncLocalSessionsToFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No user signed in, cannot sync to Firestore");
            return;
        }
        
        SessionTracker sessionTracker = SessionTracker.getInstance(context);
        List<SessionTracker.WorkoutSession> localSessions = sessionTracker.getAllSessions();
        
        Log.d(TAG, "Syncing " + localSessions.size() + " local sessions to Firestore");
        
        for (SessionTracker.WorkoutSession session : localSessions) {
            saveWorkoutSession(session);
        }
    }
    
    // Load user sessions from Firestore (for backup/restore)
    public void loadUserSessionsFromFirestore(OnSessionsLoadedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No user signed in, cannot load from Firestore");
            if (listener != null) listener.onSessionsLoaded(null);
            return;
        }
        
        String userId = currentUser.getUid();
        
        db.collection("users")
            .document(userId)
            .collection("workoutSessions")
            .orderBy("startTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                java.util.List<SessionTracker.WorkoutSession> sessions = new java.util.ArrayList<>();
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    SessionTracker.WorkoutSession session = new SessionTracker.WorkoutSession();
                    
                    session.sessionId = document.getString("sessionId");
                    session.startTime = document.getLong("startTime");
                    session.endTime = document.getLong("endTime");
                    session.mode = document.getString("mode");
                    session.stanceAccuracy = document.getDouble("stanceAccuracy").floatValue();
                    session.executionAccuracy = document.getDouble("executionAccuracy").floatValue();
                    session.totalPunches = document.getLong("totalPunches").intValue();
                    session.workoutDuration = document.getLong("workoutDuration");
                    session.stanceMeasurements = document.getLong("stanceMeasurements").intValue();
                    session.executionMeasurements = document.getLong("executionMeasurements").intValue();
                    session.date = document.getString("date");
                    
                    sessions.add(session);
                }
                
                Log.d(TAG, "Loaded " + sessions.size() + " sessions from Firestore");
                if (listener != null) listener.onSessionsLoaded(sessions);
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error loading sessions from Firestore", e);
                if (listener != null) listener.onSessionsLoaded(null);
            });
    }
    
    // Get user statistics from Firestore
    public void getUserStats(OnUserStatsLoadedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No user signed in, cannot get stats from Firestore");
            if (listener != null) listener.onStatsLoaded(null);
            return;
        }
        
        String userId = currentUser.getUid();
        
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("totalSessions", documentSnapshot.getLong("totalSessions"));
                    stats.put("totalWorkoutTime", documentSnapshot.getLong("totalWorkoutTime"));
                    stats.put("averageStanceAccuracy", documentSnapshot.getDouble("averageStanceAccuracy"));
                    stats.put("averageExecutionAccuracy", documentSnapshot.getDouble("averageExecutionAccuracy"));
                    stats.put("totalPunches", documentSnapshot.getLong("totalPunches"));
                    
                    Log.d(TAG, "User stats loaded from Firestore");
                    if (listener != null) listener.onStatsLoaded(stats);
                } else {
                    Log.d(TAG, "No user stats found in Firestore");
                    if (listener != null) listener.onStatsLoaded(null);
                }
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error loading user stats from Firestore", e);
                if (listener != null) listener.onStatsLoaded(null);
            });
    }
    
    // Interfaces for callbacks
    public interface OnSessionsLoadedListener {
        void onSessionsLoaded(List<SessionTracker.WorkoutSession> sessions);
    }
    
    public interface OnUserStatsLoadedListener {
        void onStatsLoaded(Map<String, Object> stats);
    }
} 