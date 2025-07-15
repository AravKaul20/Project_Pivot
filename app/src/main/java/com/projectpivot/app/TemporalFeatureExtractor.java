package com.projectpivot.app;

import android.graphics.PointF;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Utility class for extracting temporal features from pose sequences
 * Analyzes movement patterns, velocity, and acceleration for boxing analysis
 */
public class TemporalFeatureExtractor {
    private static final String TAG = "TemporalFeatureExtractor";
    private static final int DEFAULT_WINDOW_SIZE = 10;
    private static final int MAX_HISTORY_SIZE = 30;
    
    private Queue<List<PointF>> frameHistory;
    private Queue<Long> timestampHistory;
    private int windowSize;
    
    public TemporalFeatureExtractor() {
        this(DEFAULT_WINDOW_SIZE);
    }
    
    public TemporalFeatureExtractor(int windowSize) {
        this.windowSize = windowSize;
        this.frameHistory = new ArrayDeque<>(MAX_HISTORY_SIZE);
        this.timestampHistory = new ArrayDeque<>(MAX_HISTORY_SIZE);
    }
    
    /**
     * Add a new frame to the temporal analysis
     * @param keypoints Current frame keypoints
     * @param timestamp Current timestamp in milliseconds
     */
    public void addFrame(List<PointF> keypoints, long timestamp) {
        if (keypoints == null || keypoints.size() < 33) {
            return;
        }
        
        // Add to history
        frameHistory.offer(new ArrayList<>(keypoints));
        timestampHistory.offer(timestamp);
        
        // Maintain maximum history size
        while (frameHistory.size() > MAX_HISTORY_SIZE) {
            frameHistory.poll();
            timestampHistory.poll();
        }
    }
    
    /**
     * Extract velocity features from recent frames
     * @return Array of velocity features
     */
    public float[] extractVelocityFeatures() {
        if (frameHistory.size() < 2) {
            return new float[10]; // Return zeros if not enough history
        }
        
        float[] velocities = new float[10];
        int index = 0;
        
        try {
            List<List<PointF>> recentFrames = new ArrayList<>(frameHistory);
            List<Long> recentTimestamps = new ArrayList<>(timestampHistory);
            
            int currentIndex = recentFrames.size() - 1;
            int previousIndex = currentIndex - 1;
            
            if (previousIndex >= 0) {
                List<PointF> currentFrame = recentFrames.get(currentIndex);
                List<PointF> previousFrame = recentFrames.get(previousIndex);
                long timeDiff = recentTimestamps.get(currentIndex) - recentTimestamps.get(previousIndex);
                
                if (timeDiff > 0) {
                    // Calculate velocities for key joints
                    velocities[index++] = calculatePointVelocity(
                        previousFrame.get(MediaPipePoseDetector.LEFT_WRIST),
                        currentFrame.get(MediaPipePoseDetector.LEFT_WRIST),
                        timeDiff
                    );
                    
                    velocities[index++] = calculatePointVelocity(
                        previousFrame.get(MediaPipePoseDetector.RIGHT_WRIST),
                        currentFrame.get(MediaPipePoseDetector.RIGHT_WRIST),
                        timeDiff
                    );
                    
                    velocities[index++] = calculatePointVelocity(
                        previousFrame.get(MediaPipePoseDetector.LEFT_ELBOW),
                        currentFrame.get(MediaPipePoseDetector.LEFT_ELBOW),
                        timeDiff
                    );
                    
                    velocities[index++] = calculatePointVelocity(
                        previousFrame.get(MediaPipePoseDetector.RIGHT_ELBOW),
                        currentFrame.get(MediaPipePoseDetector.RIGHT_ELBOW),
                        timeDiff
                    );
                    
                    velocities[index++] = calculatePointVelocity(
                        previousFrame.get(MediaPipePoseDetector.LEFT_SHOULDER),
                        currentFrame.get(MediaPipePoseDetector.LEFT_SHOULDER),
                        timeDiff
                    );
                    
                    velocities[index++] = calculatePointVelocity(
                        previousFrame.get(MediaPipePoseDetector.RIGHT_SHOULDER),
                        currentFrame.get(MediaPipePoseDetector.RIGHT_SHOULDER),
                        timeDiff
                    );
                    
                    // Hip movement (important for boxing power generation)
                    velocities[index++] = calculatePointVelocity(
                        previousFrame.get(MediaPipePoseDetector.LEFT_HIP),
                        currentFrame.get(MediaPipePoseDetector.LEFT_HIP),
                        timeDiff
                    );
                    
                    velocities[index++] = calculatePointVelocity(
                        previousFrame.get(MediaPipePoseDetector.RIGHT_HIP),
                        currentFrame.get(MediaPipePoseDetector.RIGHT_HIP),
                        timeDiff
                    );
                    
                    // Center of mass velocity
                    PointF prevCenterOfMass = GeometricFeatureExtractor.calculateCenterOfMass(previousFrame);
                    PointF currCenterOfMass = GeometricFeatureExtractor.calculateCenterOfMass(currentFrame);
                    velocities[index++] = calculatePointVelocity(prevCenterOfMass, currCenterOfMass, timeDiff);
                    
                    // Overall movement intensity
                    velocities[index++] = calculateOverallMovementIntensity(previousFrame, currentFrame, timeDiff);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting velocity features", e);
        }
        
        return velocities;
    }
    
    /**
     * Extract acceleration features
     * @return Array of acceleration features
     */
    public float[] extractAccelerationFeatures() {
        if (frameHistory.size() < 3) {
            return new float[5]; // Return zeros if not enough history
        }
        
        float[] accelerations = new float[5];
        int index = 0;
        
        try {
            List<List<PointF>> recentFrames = new ArrayList<>(frameHistory);
            List<Long> recentTimestamps = new ArrayList<>(timestampHistory);
            
            int currentIndex = recentFrames.size() - 1;
            int midIndex = currentIndex - 1;
            int previousIndex = currentIndex - 2;
            
            if (previousIndex >= 0) {
                List<PointF> currentFrame = recentFrames.get(currentIndex);
                List<PointF> midFrame = recentFrames.get(midIndex);
                List<PointF> previousFrame = recentFrames.get(previousIndex);
                
                long timeDiff1 = recentTimestamps.get(midIndex) - recentTimestamps.get(previousIndex);
                long timeDiff2 = recentTimestamps.get(currentIndex) - recentTimestamps.get(midIndex);
                
                if (timeDiff1 > 0 && timeDiff2 > 0) {
                    // Calculate accelerations for key points
                    accelerations[index++] = calculatePointAcceleration(
                        previousFrame.get(MediaPipePoseDetector.LEFT_WRIST),
                        midFrame.get(MediaPipePoseDetector.LEFT_WRIST),
                        currentFrame.get(MediaPipePoseDetector.LEFT_WRIST),
                        timeDiff1, timeDiff2
                    );
                    
                    accelerations[index++] = calculatePointAcceleration(
                        previousFrame.get(MediaPipePoseDetector.RIGHT_WRIST),
                        midFrame.get(MediaPipePoseDetector.RIGHT_WRIST),
                        currentFrame.get(MediaPipePoseDetector.RIGHT_WRIST),
                        timeDiff1, timeDiff2
                    );
                    
                    accelerations[index++] = calculatePointAcceleration(
                        previousFrame.get(MediaPipePoseDetector.LEFT_HIP),
                        midFrame.get(MediaPipePoseDetector.LEFT_HIP),
                        currentFrame.get(MediaPipePoseDetector.LEFT_HIP),
                        timeDiff1, timeDiff2
                    );
                    
                    accelerations[index++] = calculatePointAcceleration(
                        previousFrame.get(MediaPipePoseDetector.RIGHT_HIP),
                        midFrame.get(MediaPipePoseDetector.RIGHT_HIP),
                        currentFrame.get(MediaPipePoseDetector.RIGHT_HIP),
                        timeDiff1, timeDiff2
                    );
                    
                    // Overall acceleration intensity
                    accelerations[index++] = calculateOverallAccelerationIntensity(
                        previousFrame, midFrame, currentFrame, timeDiff1, timeDiff2
                    );
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting acceleration features", e);
        }
        
        return accelerations;
    }
    
    /**
     * Extract movement pattern features
     * @return Array of movement pattern features
     */
    public float[] extractMovementPatterns() {
        if (frameHistory.size() < windowSize) {
            return new float[8]; // Return zeros if not enough history
        }
        
        float[] patterns = new float[8];
        int index = 0;
        
        try {
            List<List<PointF>> recentFrames = new ArrayList<>(frameHistory);
            
            // Analyze punch-like movements
            patterns[index++] = detectPunchMovement(recentFrames, MediaPipePoseDetector.LEFT_WRIST);
            patterns[index++] = detectPunchMovement(recentFrames, MediaPipePoseDetector.RIGHT_WRIST);
            
            // Analyze stance stability
            patterns[index++] = calculateStanceStability(recentFrames);
            
            // Analyze hip rotation patterns
            patterns[index++] = calculateHipRotationPattern(recentFrames);
            
            // Analyze guard position consistency
            patterns[index++] = calculateGuardPositionConsistency(recentFrames);
            
            // Analyze movement smoothness
            patterns[index++] = calculateMovementSmoothness(recentFrames);
            
            // Analyze rhythm/timing
            patterns[index++] = calculateMovementRhythm(recentFrames);
            
            // Analyze balance/stability
            patterns[index++] = calculateBalanceStability(recentFrames);
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting movement patterns", e);
        }
        
        return patterns;
    }
    
    /**
     * Calculate velocity of a point between two frames
     */
    private float calculatePointVelocity(PointF prevPoint, PointF currPoint, long timeDiff) {
        if (prevPoint == null || currPoint == null || timeDiff <= 0) {
            return 0.0f;
        }
        
        float dx = currPoint.x - prevPoint.x;
        float dy = currPoint.y - prevPoint.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        return distance / (timeDiff / 1000.0f); // pixels per second
    }
    
    /**
     * Calculate acceleration of a point across three frames
     */
    private float calculatePointAcceleration(PointF prevPoint, PointF midPoint, PointF currPoint,
                                           long timeDiff1, long timeDiff2) {
        if (prevPoint == null || midPoint == null || currPoint == null || 
            timeDiff1 <= 0 || timeDiff2 <= 0) {
            return 0.0f;
        }
        
        float velocity1 = calculatePointVelocity(prevPoint, midPoint, timeDiff1);
        float velocity2 = calculatePointVelocity(midPoint, currPoint, timeDiff2);
        
        return (velocity2 - velocity1) / (timeDiff2 / 1000.0f); // pixels per second squared
    }
    
    /**
     * Calculate overall movement intensity across all keypoints
     */
    private float calculateOverallMovementIntensity(List<PointF> prevFrame, List<PointF> currFrame, long timeDiff) {
        float totalMovement = 0.0f;
        int validPoints = 0;
        
        for (int i = 0; i < Math.min(prevFrame.size(), currFrame.size()); i++) {
            PointF prev = prevFrame.get(i);
            PointF curr = currFrame.get(i);
            
            if (prev != null && curr != null) {
                float movement = calculatePointVelocity(prev, curr, timeDiff);
                totalMovement += movement;
                validPoints++;
            }
        }
        
        return validPoints > 0 ? totalMovement / validPoints : 0.0f;
    }
    
    /**
     * Calculate overall acceleration intensity
     */
    private float calculateOverallAccelerationIntensity(List<PointF> prevFrame, List<PointF> midFrame, 
                                                       List<PointF> currFrame, long timeDiff1, long timeDiff2) {
        float totalAcceleration = 0.0f;
        int validPoints = 0;
        
        for (int i = 0; i < Math.min(Math.min(prevFrame.size(), midFrame.size()), currFrame.size()); i++) {
            PointF prev = prevFrame.get(i);
            PointF mid = midFrame.get(i);
            PointF curr = currFrame.get(i);
            
            if (prev != null && mid != null && curr != null) {
                float acceleration = calculatePointAcceleration(prev, mid, curr, timeDiff1, timeDiff2);
                totalAcceleration += Math.abs(acceleration);
                validPoints++;
            }
        }
        
        return validPoints > 0 ? totalAcceleration / validPoints : 0.0f;
    }
    
    /**
     * Detect punch-like movement patterns
     */
    private float detectPunchMovement(List<List<PointF>> frames, int wristIndex) {
        if (frames.size() < 5) return 0.0f;
        
        float maxExtension = 0.0f;
        float minExtension = Float.MAX_VALUE;
        
        for (List<PointF> frame : frames) {
            if (frame.size() > wristIndex && frame.size() > MediaPipePoseDetector.LEFT_SHOULDER) {
                PointF wrist = frame.get(wristIndex);
                PointF shoulder = frame.get(wristIndex == MediaPipePoseDetector.LEFT_WRIST ? 
                    MediaPipePoseDetector.LEFT_SHOULDER : MediaPipePoseDetector.RIGHT_SHOULDER);
                
                if (wrist != null && shoulder != null) {
                    float extension = JointAngleCalculator.calculateDistance(shoulder, wrist);
                    maxExtension = Math.max(maxExtension, extension);
                    minExtension = Math.min(minExtension, extension);
                }
            }
        }
        
        return maxExtension - minExtension; // Range of motion
    }
    
    /**
     * Calculate stance stability over time
     */
    private float calculateStanceStability(List<List<PointF>> frames) {
        if (frames.size() < 3) return 0.0f;
        
        float totalVariation = 0.0f;
        int validMeasurements = 0;
        
        for (int i = 1; i < frames.size(); i++) {
            List<PointF> prevFrame = frames.get(i - 1);
            List<PointF> currFrame = frames.get(i);
            
            if (prevFrame.size() > MediaPipePoseDetector.RIGHT_ANKLE && 
                currFrame.size() > MediaPipePoseDetector.RIGHT_ANKLE) {
                
                PointF prevLeftAnkle = prevFrame.get(MediaPipePoseDetector.LEFT_ANKLE);
                PointF prevRightAnkle = prevFrame.get(MediaPipePoseDetector.RIGHT_ANKLE);
                PointF currLeftAnkle = currFrame.get(MediaPipePoseDetector.LEFT_ANKLE);
                PointF currRightAnkle = currFrame.get(MediaPipePoseDetector.RIGHT_ANKLE);
                
                if (prevLeftAnkle != null && prevRightAnkle != null && 
                    currLeftAnkle != null && currRightAnkle != null) {
                    
                    float prevStanceWidth = JointAngleCalculator.calculateDistance(prevLeftAnkle, prevRightAnkle);
                    float currStanceWidth = JointAngleCalculator.calculateDistance(currLeftAnkle, currRightAnkle);
                    
                    totalVariation += Math.abs(currStanceWidth - prevStanceWidth);
                    validMeasurements++;
                }
            }
        }
        
        return validMeasurements > 0 ? totalVariation / validMeasurements : 0.0f;
    }
    
    /**
     * Calculate hip rotation pattern
     */
    private float calculateHipRotationPattern(List<List<PointF>> frames) {
        // Implementation for hip rotation analysis
        return 0.0f; // Placeholder
    }
    
    /**
     * Calculate guard position consistency
     */
    private float calculateGuardPositionConsistency(List<List<PointF>> frames) {
        // Implementation for guard position analysis
        return 0.0f; // Placeholder
    }
    
    /**
     * Calculate movement smoothness
     */
    private float calculateMovementSmoothness(List<List<PointF>> frames) {
        // Implementation for smoothness analysis
        return 0.0f; // Placeholder
    }
    
    /**
     * Calculate movement rhythm
     */
    private float calculateMovementRhythm(List<List<PointF>> frames) {
        // Implementation for rhythm analysis
        return 0.0f; // Placeholder
    }
    
    /**
     * Calculate balance stability
     */
    private float calculateBalanceStability(List<List<PointF>> frames) {
        // Implementation for balance analysis
        return 0.0f; // Placeholder
    }
    
    /**
     * Reset the temporal analysis
     */
    public void reset() {
        frameHistory.clear();
        timestampHistory.clear();
    }
    
    /**
     * Get the current frame history size
     */
    public int getHistorySize() {
        return frameHistory.size();
    }
} 