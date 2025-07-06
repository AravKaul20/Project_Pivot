package com.projectpivot.app;

import java.util.ArrayList;
import java.util.List;

public class PredictionSmoother {
    private List<String> stanceHistory = new ArrayList<>();
    private List<String> executionHistory = new ArrayList<>();
    private final int HISTORY_SIZE = 5;
    private final double CONFIDENCE_THRESHOLD = 0.7;
    private String lastStableStance = "Unknown";
    private String lastStableExecution = "Unknown";
    private long lastUpdateTime = 0;
    private final long MIN_UPDATE_INTERVAL = 200; // 200ms
    
    public boolean shouldUpdate() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < MIN_UPDATE_INTERVAL) {
            return false;
        }
        lastUpdateTime = currentTime;
        return true;
    }
    
    public String getSmoothedStance(float[] logits) {
        return getSmoothed(logits, stanceHistory, true);
    }
    
    public String getSmoothedExecution(float[] logits) {
        return getSmoothed(logits, executionHistory, false);
    }
    
    private String getSmoothed(float[] logits, List<String> history, boolean isStance) {
        float confidence = Math.abs(logits[1] - logits[0]);
        String current = logits[1] > logits[0] ? 
            String.format("Correct (%.2f)", confidence) : 
            String.format("Incorrect (%.2f)", confidence);
        
        history.add(current);
        if (history.size() > HISTORY_SIZE) {
            history.remove(0);
        }
        
        if (confidence > CONFIDENCE_THRESHOLD && history.size() >= 3) {
            String majority = getMajority(history);
            if (majority != null && !majority.equals(isStance ? lastStableStance : lastStableExecution)) {
                if (isStance) {
                    lastStableStance = majority;
                } else {
                    lastStableExecution = majority;
                }
                return majority;
            }
        }
        
        return isStance ? lastStableStance : lastStableExecution;
    }
    
    private String getMajority(List<String> history) {
        if (history.size() < 3) return null;
        
        int correct = 0, incorrect = 0;
        for (String pred : history) {
            if (pred.startsWith("Correct")) correct++;
            else if (pred.startsWith("Incorrect")) incorrect++;
        }
        
        double consensus = Math.max(correct, incorrect) / (double) history.size();
        if (consensus >= 0.6) {
            return correct > incorrect ? history.get(history.size() - 1) : 
                   (incorrect > correct ? history.get(history.size() - 1) : null);
        }
        return null;
    }
} 