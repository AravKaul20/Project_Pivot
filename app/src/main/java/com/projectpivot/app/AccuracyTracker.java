package com.projectpivot.app;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class AccuracyTracker {
    
    private static final String TAG = "AccuracyTracker";
    
    public static class AccuracyMeasurement {
        public long timestamp;
        public float accuracy; // 0.0 to 1.0
        public float confidence;
        public String prediction; // "Correct" or "Incorrect"
        public float[] rawLogits;
        
        public AccuracyMeasurement(long timestamp, float accuracy, float confidence, String prediction, float[] rawLogits) {
            this.timestamp = timestamp;
            this.accuracy = accuracy;
            this.confidence = confidence;
            this.prediction = prediction;
            this.rawLogits = rawLogits != null ? rawLogits.clone() : new float[0];
        }
    }
    
    private List<AccuracyMeasurement> stanceHistory = new ArrayList<>();
    private List<AccuracyMeasurement> executionHistory = new ArrayList<>();
    
    // Configuration
    private static final int MAX_HISTORY_SIZE = 1000; // Keep last 1000 measurements
    private static final long CONSISTENCY_WINDOW_MS = 5000; // 5 second window for consistency
    private static final float MIN_CONFIDENCE_THRESHOLD = 0.3f;
    
    public void recordStanceAccuracy(float[] logits, String prediction) {
        if (logits == null || logits.length < 2) return;
        
        long timestamp = System.currentTimeMillis();
        float confidence = calculateConfidence(logits);
        float accuracy = calculateInstantAccuracy(logits, prediction);
        
        AccuracyMeasurement measurement = new AccuracyMeasurement(
            timestamp, accuracy, confidence, prediction, logits
        );
        
        stanceHistory.add(measurement);
        
        // Keep history size manageable
        if (stanceHistory.size() > MAX_HISTORY_SIZE) {
            stanceHistory.remove(0);
        }
        
        Log.d(TAG, String.format("Stance accuracy recorded: %.2f (confidence: %.2f)", accuracy, confidence));
    }
    
    public void recordExecutionAccuracy(float[] logits, String prediction) {
        if (logits == null || logits.length < 2) return;
        
        long timestamp = System.currentTimeMillis();
        float confidence = calculateConfidence(logits);
        float accuracy = calculateInstantAccuracy(logits, prediction);
        
        AccuracyMeasurement measurement = new AccuracyMeasurement(
            timestamp, accuracy, confidence, prediction, logits
        );
        
        executionHistory.add(measurement);
        
        // Keep history size manageable
        if (executionHistory.size() > MAX_HISTORY_SIZE) {
            executionHistory.remove(0);
        }
        
        Log.d(TAG, String.format("Execution accuracy recorded: %.2f (confidence: %.2f)", accuracy, confidence));
    }
    
    private float calculateConfidence(float[] logits) {
        if (logits.length < 2) return 0f;
        
        // Confidence is the difference between the two predictions
        // Higher difference = more confident
        float confidence = Math.abs(logits[1] - logits[0]);
        
        // Normalize to 0-1 range (assuming logits are typically in -5 to +5 range)
        return Math.min(confidence / 10f, 1f);
    }
    
    private float calculateInstantAccuracy(float[] logits, String prediction) {
        if (logits.length < 2) return 0f;
        
        float confidence = calculateConfidence(logits);
        
        // If prediction is "Correct", accuracy is based on confidence
        // If prediction is "Incorrect", accuracy is lower
        if (prediction.toLowerCase().contains("correct")) {
            // Higher confidence in correct prediction = higher accuracy
            return Math.min(0.5f + (confidence * 0.5f), 1f);
        } else if (prediction.toLowerCase().contains("incorrect")) {
            // Confident incorrect prediction = very low accuracy
            return Math.max(0.5f - (confidence * 0.5f), 0f);
        } else {
            // Unknown/uncertain prediction
            return 0.3f;
        }
    }
    
    public float calculateSessionStanceAccuracy() {
        if (stanceHistory.isEmpty()) return 0f;
        
        float avgConfidence = calculateAverageConfidence(stanceHistory);
        float consistencyScore = calculateConsistencyScore(stanceHistory);
        float durationScore = calculateCorrectDurationRatio(stanceHistory);
        
        // Weighted combination
        float finalAccuracy = (avgConfidence * 0.4f) + (consistencyScore * 0.3f) + (durationScore * 0.3f);
        
        Log.d(TAG, String.format("Session stance accuracy: %.2f (conf: %.2f, cons: %.2f, dur: %.2f)", 
            finalAccuracy, avgConfidence, consistencyScore, durationScore));
        
        return Math.min(finalAccuracy, 1f);
    }
    
    public float calculateSessionExecutionAccuracy() {
        if (executionHistory.isEmpty()) return 0f;
        
        float avgConfidence = calculateAverageConfidence(executionHistory);
        float consistencyScore = calculateConsistencyScore(executionHistory);
        float durationScore = calculateCorrectDurationRatio(executionHistory);
        
        // Weighted combination
        float finalAccuracy = (avgConfidence * 0.4f) + (consistencyScore * 0.3f) + (durationScore * 0.3f);
        
        Log.d(TAG, String.format("Session execution accuracy: %.2f (conf: %.2f, cons: %.2f, dur: %.2f)", 
            finalAccuracy, avgConfidence, consistencyScore, durationScore));
        
        return Math.min(finalAccuracy, 1f);
    }
    
    private float calculateAverageConfidence(List<AccuracyMeasurement> history) {
        if (history.isEmpty()) return 0f;
        
        float totalConfidence = 0f;
        int validMeasurements = 0;
        
        for (AccuracyMeasurement measurement : history) {
            if (measurement.confidence >= MIN_CONFIDENCE_THRESHOLD) {
                totalConfidence += measurement.confidence;
                validMeasurements++;
            }
        }
        
        return validMeasurements > 0 ? totalConfidence / validMeasurements : 0f;
    }
    
    private float calculateConsistencyScore(List<AccuracyMeasurement> history) {
        if (history.size() < 3) return 0f;
        
        long currentTime = System.currentTimeMillis();
        int correctCount = 0;
        int totalCount = 0;
        
        // Look at recent measurements within consistency window
        for (AccuracyMeasurement measurement : history) {
            if (currentTime - measurement.timestamp <= CONSISTENCY_WINDOW_MS) {
                if (measurement.prediction.toLowerCase().contains("correct")) {
                    correctCount++;
                }
                totalCount++;
            }
        }
        
        return totalCount > 0 ? (float) correctCount / totalCount : 0f;
    }
    
    private float calculateCorrectDurationRatio(List<AccuracyMeasurement> history) {
        if (history.isEmpty()) return 0f;
        
        long totalDuration = 0;
        long correctDuration = 0;
        long lastTimestamp = 0;
        boolean wasCorrect = false;
        
        for (AccuracyMeasurement measurement : history) {
            if (lastTimestamp > 0) {
                long duration = measurement.timestamp - lastTimestamp;
                totalDuration += duration;
                
                if (wasCorrect) {
                    correctDuration += duration;
                }
            }
            
            wasCorrect = measurement.prediction.toLowerCase().contains("correct");
            lastTimestamp = measurement.timestamp;
        }
        
        return totalDuration > 0 ? (float) correctDuration / totalDuration : 0f;
    }
    
    public void resetSession() {
        stanceHistory.clear();
        executionHistory.clear();
        Log.d(TAG, "Accuracy tracking session reset");
    }
    
    public int getStanceMeasurementCount() {
        return stanceHistory.size();
    }
    
    public int getExecutionMeasurementCount() {
        return executionHistory.size();
    }
    
    public List<AccuracyMeasurement> getStanceHistory() {
        return new ArrayList<>(stanceHistory);
    }
    
    public List<AccuracyMeasurement> getExecutionHistory() {
        return new ArrayList<>(executionHistory);
    }
} 