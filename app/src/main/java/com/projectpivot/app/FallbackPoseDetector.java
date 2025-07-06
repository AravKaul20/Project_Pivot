package com.projectpivot.app;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class FallbackPoseDetector {
    private static final String TAG = "FallbackPoseDetector";
    
    // MediaPipe pose landmark indices
    public static final int NOSE = 0;
    public static final int LEFT_EYE_INNER = 1;
    public static final int LEFT_EYE = 2;
    public static final int LEFT_EYE_OUTER = 3;
    public static final int RIGHT_EYE_INNER = 4;
    public static final int RIGHT_EYE = 5;
    public static final int RIGHT_EYE_OUTER = 6;
    public static final int LEFT_EAR = 7;
    public static final int RIGHT_EAR = 8;
    public static final int MOUTH_LEFT = 9;
    public static final int MOUTH_RIGHT = 10;
    public static final int LEFT_SHOULDER = 11;
    public static final int RIGHT_SHOULDER = 12;
    public static final int LEFT_ELBOW = 13;
    public static final int RIGHT_ELBOW = 14;
    public static final int LEFT_WRIST = 15;
    public static final int RIGHT_WRIST = 16;
    public static final int LEFT_PINKY = 17;
    public static final int RIGHT_PINKY = 18;
    public static final int LEFT_INDEX = 19;
    public static final int RIGHT_INDEX = 20;
    public static final int LEFT_THUMB = 21;
    public static final int RIGHT_THUMB = 22;
    public static final int LEFT_HIP = 23;
    public static final int RIGHT_HIP = 24;
    public static final int LEFT_KNEE = 25;
    public static final int RIGHT_KNEE = 26;
    public static final int LEFT_ANKLE = 27;
    public static final int RIGHT_ANKLE = 28;
    public static final int LEFT_HEEL = 29;
    public static final int RIGHT_HEEL = 30;
    public static final int LEFT_FOOT_INDEX = 31;
    public static final int RIGHT_FOOT_INDEX = 32;
    
    private static boolean warningLogged = false;
    
    public List<PointF> detectPose(Bitmap bitmap) {
        if (!warningLogged) {
            Log.w(TAG, "Using fallback pose detector - MediaPipe not available");
            warningLogged = true;
        }
        
        List<PointF> keypoints = new ArrayList<>();
        
        if (bitmap == null) {
            return keypoints;
        }
        
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // Generate stable, centered pose keypoints
        float centerX = width * 0.5f;
        float centerY = height * 0.5f;
        
        // Face keypoints
        keypoints.add(new PointF(centerX, centerY - 200));           // NOSE
        keypoints.add(new PointF(centerX - 10, centerY - 210));      // LEFT_EYE_INNER
        keypoints.add(new PointF(centerX - 20, centerY - 210));      // LEFT_EYE
        keypoints.add(new PointF(centerX - 30, centerY - 210));      // LEFT_EYE_OUTER
        keypoints.add(new PointF(centerX + 10, centerY - 210));      // RIGHT_EYE_INNER
        keypoints.add(new PointF(centerX + 20, centerY - 210));      // RIGHT_EYE
        keypoints.add(new PointF(centerX + 30, centerY - 210));      // RIGHT_EYE_OUTER
        keypoints.add(new PointF(centerX - 40, centerY - 200));      // LEFT_EAR
        keypoints.add(new PointF(centerX + 40, centerY - 200));      // RIGHT_EAR
        keypoints.add(new PointF(centerX - 15, centerY - 180));      // MOUTH_LEFT
        keypoints.add(new PointF(centerX + 15, centerY - 180));      // MOUTH_RIGHT
        
        // Torso keypoints
        keypoints.add(new PointF(centerX - 80, centerY - 100));      // LEFT_SHOULDER
        keypoints.add(new PointF(centerX + 80, centerY - 100));      // RIGHT_SHOULDER
        
        // Arm keypoints
        keypoints.add(new PointF(centerX - 120, centerY - 20));      // LEFT_ELBOW
        keypoints.add(new PointF(centerX + 120, centerY - 20));      // RIGHT_ELBOW
        keypoints.add(new PointF(centerX - 140, centerY + 40));      // LEFT_WRIST
        keypoints.add(new PointF(centerX + 140, centerY + 40));      // RIGHT_WRIST
        
        // Hand keypoints
        keypoints.add(new PointF(centerX - 145, centerY + 50));      // LEFT_PINKY
        keypoints.add(new PointF(centerX + 145, centerY + 50));      // RIGHT_PINKY
        keypoints.add(new PointF(centerX - 150, centerY + 45));      // LEFT_INDEX
        keypoints.add(new PointF(centerX + 150, centerY + 45));      // RIGHT_INDEX
        keypoints.add(new PointF(centerX - 135, centerY + 35));      // LEFT_THUMB
        keypoints.add(new PointF(centerX + 135, centerY + 35));      // RIGHT_THUMB
        
        // Hip keypoints
        keypoints.add(new PointF(centerX - 60, centerY + 80));       // LEFT_HIP
        keypoints.add(new PointF(centerX + 60, centerY + 80));       // RIGHT_HIP
        
        // Leg keypoints
        keypoints.add(new PointF(centerX - 70, centerY + 200));      // LEFT_KNEE
        keypoints.add(new PointF(centerX + 70, centerY + 200));      // RIGHT_KNEE
        keypoints.add(new PointF(centerX - 75, centerY + 320));      // LEFT_ANKLE
        keypoints.add(new PointF(centerX + 75, centerY + 320));      // RIGHT_ANKLE
        
        // Foot keypoints
        keypoints.add(new PointF(centerX - 90, centerY + 340));      // LEFT_HEEL
        keypoints.add(new PointF(centerX + 90, centerY + 340));      // RIGHT_HEEL
        keypoints.add(new PointF(centerX - 60, centerY + 340));      // LEFT_FOOT_INDEX
        keypoints.add(new PointF(centerX + 60, centerY + 340));      // RIGHT_FOOT_INDEX
        
        return keypoints;
    }
    
    // Convert pose keypoints to the 28-feature format used by your models
    public float[] keypointsToFeatures(List<PointF> keypoints, int imageWidth, int imageHeight) {
        if (keypoints.size() < 33) {
            Log.w(TAG, "Not enough keypoints detected: " + keypoints.size());
            return new float[28]; // Return zeros if not enough keypoints
        }
        
        // Extract the 14 keypoints used in your training data
        int[] selectedKeypoints = {
            LEFT_SHOULDER, RIGHT_SHOULDER,     // 0, 1
            LEFT_ELBOW, RIGHT_ELBOW,           // 2, 3
            LEFT_WRIST, RIGHT_WRIST,           // 4, 5
            LEFT_HIP, RIGHT_HIP,               // 6, 7
            LEFT_KNEE, RIGHT_KNEE,             // 8, 9
            LEFT_ANKLE, RIGHT_ANKLE,           // 10, 11
            NOSE, MOUTH_LEFT                   // 12, 13 (for balance/head position)
        };
        
        float[] features = new float[28]; // 14 keypoints * 2 coordinates
        
        // First, extract raw coordinates
        for (int i = 0; i < 14; i++) {
            if (selectedKeypoints[i] < keypoints.size()) {
                PointF point = keypoints.get(selectedKeypoints[i]);
                
                if (point != null) {
                    // Convert to normalized coordinates [0, 1]
                    features[i * 2] = point.x / imageWidth;        // x coordinate
                    features[i * 2 + 1] = point.y / imageHeight;   // y coordinate
                } else {
                    // Use zeros for invisible/missing keypoints
                    features[i * 2] = 0.0f;
                    features[i * 2 + 1] = 0.0f;
                }
            }
        }
        
        // Apply statistical normalization EXACTLY like training data
        float mean = 0.0f;
        for (float value : features) {
            mean += value;
        }
        mean /= features.length;
        
        float variance = 0.0f;
        for (float value : features) {
            variance += (value - mean) * (value - mean);
        }
        variance /= features.length;
        float std = (float) Math.sqrt(variance) + 1e-8f;
        
        // Apply normalization
        for (int i = 0; i < features.length; i++) {
            features[i] = (features[i] - mean) / std;
        }
        
        return features;
    }
} 