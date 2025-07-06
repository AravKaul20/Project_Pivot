package com.projectpivot.app;

import android.content.Context;
import android.content.SharedPreferences;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;

public class PunchAnalyzer {
    
    private static final String TAG = "PunchAnalyzer";
    private static PunchAnalyzer instance;
    
    private Context context;
    private SharedPreferences preferences;
    
    // Improved punch detection parameters
    private static final float MIN_PUNCH_DISTANCE = 0.08f; // Minimum distance for punch (normalized coordinates)
    private static final float MAX_PUNCH_VELOCITY = 2.0f; // Maximum realistic velocity
    private static final int PUNCH_COOLDOWN_MS = 800; // Realistic minimum time between punches
    private static final int PUNCH_SEQUENCE_FRAMES = 8; // Frames to analyze for punch pattern
    private static final float SHOULDER_WIDTH_ESTIMATE = 0.4f; // Estimated shoulder width in normalized coords
    
    // Hand landmark indices (MediaPipe)
    private static final int LEFT_WRIST = 15;
    private static final int RIGHT_WRIST = 16;
    private static final int LEFT_SHOULDER = 11;
    private static final int RIGHT_SHOULDER = 12;
    private static final int NOSE = 0;
    
    // Tracking variables
    private List<WristPosition> leftWristHistory = new ArrayList<>();
    private List<WristPosition> rightWristHistory = new ArrayList<>();
    private int totalPunches = 0;
    private long lastLeftPunchTime = 0;
    private long lastRightPunchTime = 0;
    private long sessionStartTime = 0;
    private float estimatedPersonDistance = 1.5f; // meters from camera
    
    public static class WristPosition {
        public float x, y;
        public long timestamp;
        public float distanceFromBody; // Distance from center line
        
        public WristPosition(float x, float y, long timestamp, float distanceFromBody) {
            this.x = x;
            this.y = y;
            this.timestamp = timestamp;
            this.distanceFromBody = distanceFromBody;
        }
    }
    
    public static class PunchData {
        public int totalPunches;
        public float punchFrequency; // punches per minute
        public long sessionDuration; // milliseconds
        
        public PunchData(int totalPunches, float punchFrequency, long sessionDuration) {
            this.totalPunches = totalPunches;
            this.punchFrequency = punchFrequency;
            this.sessionDuration = sessionDuration;
        }
    }
    
    private PunchAnalyzer(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences("PROJECT_PIVOT_SETTINGS", Context.MODE_PRIVATE);
        resetSession();
    }
    
    public static synchronized PunchAnalyzer getInstance(Context context) {
        if (instance == null) {
            instance = new PunchAnalyzer(context);
        }
        return instance;
    }
    
    public void resetSession() {
        totalPunches = 0;
        sessionStartTime = System.currentTimeMillis();
        leftWristHistory.clear();
        rightWristHistory.clear();
        lastLeftPunchTime = 0;
        lastRightPunchTime = 0;
        estimatedPersonDistance = 1.5f;
    }
    
    public void analyzePose(List<PointF> landmarks) {
        if (!isPunchCounterEnabled() || landmarks == null || landmarks.size() < 21) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Get key landmarks
        PointF leftWrist = landmarks.get(LEFT_WRIST);
        PointF rightWrist = landmarks.get(RIGHT_WRIST);
        PointF leftShoulder = landmarks.get(LEFT_SHOULDER);
        PointF rightShoulder = landmarks.get(RIGHT_SHOULDER);
        PointF nose = landmarks.get(NOSE);
        
        if (leftWrist == null || rightWrist == null || leftShoulder == null || 
            rightShoulder == null || nose == null) {
            return;
        }
        
        // Estimate person's distance from camera based on shoulder width
        updateDistanceEstimate(leftShoulder, rightShoulder);
        
        // Calculate body center line
        float centerX = (leftShoulder.x + rightShoulder.x) / 2f;
        
        // Track wrist positions with distance from body center
        float leftDistanceFromBody = Math.abs(leftWrist.x - centerX);
        float rightDistanceFromBody = Math.abs(rightWrist.x - centerX);
        
        addWristPosition(leftWristHistory, leftWrist.x, leftWrist.y, currentTime, leftDistanceFromBody);
        addWristPosition(rightWristHistory, rightWrist.x, rightWrist.y, currentTime, rightDistanceFromBody);
        
        // Analyze for punches with improved detection
        analyzePunchPattern(leftWristHistory, true, currentTime, centerX);
        analyzePunchPattern(rightWristHistory, false, currentTime, centerX);
    }
    
    private void updateDistanceEstimate(PointF leftShoulder, PointF rightShoulder) {
        // Calculate shoulder width in normalized coordinates
        float observedShoulderWidth = Math.abs(rightShoulder.x - leftShoulder.x);
        
        // Estimate distance based on shoulder width (inverse relationship)
        // Closer person = wider shoulders in screen, farther = narrower
        if (observedShoulderWidth > 0.1f) { // Avoid division by very small numbers
            estimatedPersonDistance = SHOULDER_WIDTH_ESTIMATE / observedShoulderWidth;
            // Clamp to reasonable range (0.5m to 3m)
            estimatedPersonDistance = Math.max(0.5f, Math.min(3.0f, estimatedPersonDistance));
        }
    }
    
    private void addWristPosition(List<WristPosition> history, float x, float y, long timestamp, float distanceFromBody) {
        history.add(new WristPosition(x, y, timestamp, distanceFromBody));
        
        // Keep only recent positions for analysis (1.5 seconds)
        while (!history.isEmpty() && timestamp - history.get(0).timestamp > 1500) {
            history.remove(0);
        }
    }
    
    private void analyzePunchPattern(List<WristPosition> history, boolean isLeftHand, long currentTime, float centerX) {
        if (history.size() < PUNCH_SEQUENCE_FRAMES) return;
        
        // Check cooldown
        long lastPunchTime = isLeftHand ? lastLeftPunchTime : lastRightPunchTime;
        if (currentTime - lastPunchTime < PUNCH_COOLDOWN_MS) {
            return;
        }
        
        // Analyze recent movement pattern
        List<WristPosition> recentFrames = history.subList(
            Math.max(0, history.size() - PUNCH_SEQUENCE_FRAMES), 
            history.size()
        );
        
        if (isPunchPattern(recentFrames, isLeftHand, centerX)) {
            registerPunch(isLeftHand, currentTime);
        }
    }
    
    private boolean isPunchPattern(List<WristPosition> frames, boolean isLeftHand, float centerX) {
        if (frames.size() < 4) return false;
        
        WristPosition start = frames.get(0);
        WristPosition end = frames.get(frames.size() - 1);
        
        // Calculate movement metrics
        float totalDistance = calculateDistance(start, end);
        float timeSpan = (end.timestamp - start.timestamp) / 1000f; // seconds
        
        if (timeSpan <= 0) return false;
        
        // Scale distance by estimated person distance for realistic measurements
        float realWorldDistance = totalDistance * estimatedPersonDistance;
        float velocity = realWorldDistance / timeSpan;
        
        // Check for punch characteristics:
        // 1. Sufficient forward movement (away from body center)
        boolean movingForward = isLeftHand ? 
            (end.x < start.x) : // Left hand moves left (away from center)
            (end.x > start.x);   // Right hand moves right (away from center)
        
        // 2. Reasonable distance (15cm to 60cm in real world)
        boolean sufficientDistance = realWorldDistance >= 0.15f && realWorldDistance <= 0.6f;
        
        // 3. Realistic velocity (0.5 to 4 m/s for punches)
        boolean realisticVelocity = velocity >= 0.5f && velocity <= 4.0f;
        
        // 4. Extension pattern (wrist moves away from body center)
        boolean extensionPattern = end.distanceFromBody > start.distanceFromBody;
        
        // 5. Not just random jittery movement
        boolean smoothMovement = isMovementSmooth(frames);
        
        boolean isPunch = movingForward && sufficientDistance && realisticVelocity && 
                         extensionPattern && smoothMovement;
        
        return isPunch;
    }
    
    private boolean isMovementSmooth(List<WristPosition> frames) {
        if (frames.size() < 3) return false;
        
        float totalJitter = 0f;
        for (int i = 1; i < frames.size() - 1; i++) {
            WristPosition prev = frames.get(i - 1);
            WristPosition curr = frames.get(i);
            WristPosition next = frames.get(i + 1);
            
            // Calculate direction changes (jitter)
            float dir1X = curr.x - prev.x;
            float dir1Y = curr.y - prev.y;
            float dir2X = next.x - curr.x;
            float dir2Y = next.y - curr.y;
            
            // Dot product to measure direction consistency
            float dotProduct = dir1X * dir2X + dir1Y * dir2Y;
            float mag1 = (float) Math.sqrt(dir1X * dir1X + dir1Y * dir1Y);
            float mag2 = (float) Math.sqrt(dir2X * dir2X + dir2Y * dir2Y);
            
            if (mag1 > 0 && mag2 > 0) {
                float cosAngle = dotProduct / (mag1 * mag2);
                totalJitter += Math.abs(1.0f - cosAngle); // Higher = more direction change
            }
        }
        
        float avgJitter = totalJitter / (frames.size() - 2);
        return avgJitter < 0.5f; // Threshold for smooth movement
    }
    
    private float calculateDistance(WristPosition start, WristPosition end) {
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    private void registerPunch(boolean isLeftHand, long timestamp) {
        totalPunches++;
        
        // Update last punch time
        if (isLeftHand) {
            lastLeftPunchTime = timestamp;
        } else {
            lastRightPunchTime = timestamp;
        }
        
        // Add haptic feedback for punch detection
        SoundManager.getInstance(context).hapticClick();
    }
    
    public PunchData getCurrentData() {
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        float sessionMinutes = sessionDuration / 60000f; // Convert to minutes
        
        float punchFrequency = sessionMinutes > 0 ? totalPunches / sessionMinutes : 0f;
        
        return new PunchData(totalPunches, punchFrequency, sessionDuration);
    }
    
    public boolean isPunchCounterEnabled() {
        return preferences.getBoolean("punch_counter_enabled", true);
    }
    
    public int getTotalPunches() {
        return totalPunches;
    }
    
    public float getPunchFrequency() {
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        float sessionMinutes = sessionDuration / 60000f;
        return sessionMinutes > 0 ? totalPunches / sessionMinutes : 0f;
    }
    
    public float getEstimatedDistance() {
        return estimatedPersonDistance;
    }
    
    public String getFormattedStats() {
        PunchData data = getCurrentData();
        return String.format("Punches: %d | Freq: %.1f/min | Distance: %.1fm", 
                data.totalPunches, data.punchFrequency, estimatedPersonDistance);
    }
    
    // Method to save session data for progress tracking
    public void saveSessionData() {
        PunchData data = getCurrentData();
        
        // Save to shared preferences for history
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("last_session_punches", data.totalPunches);
        editor.putFloat("last_session_frequency", data.punchFrequency);
        editor.putLong("last_session_duration", data.sessionDuration);
        editor.putLong("last_session_timestamp", System.currentTimeMillis());
        
        // Update all-time stats
        int totalSessionPunches = preferences.getInt("total_all_time_punches", 0) + data.totalPunches;
        float bestFrequency = Math.max(preferences.getFloat("best_punch_frequency", 0f), data.punchFrequency);
        
        editor.putInt("total_all_time_punches", totalSessionPunches);
        editor.putFloat("best_punch_frequency", bestFrequency);
        
        editor.apply();
    }
} 