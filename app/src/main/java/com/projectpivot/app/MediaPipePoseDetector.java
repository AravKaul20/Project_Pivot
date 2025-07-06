package com.projectpivot.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.ArrayList;
import java.util.List;

public class MediaPipePoseDetector {
    private static final String TAG = "MediaPipePoseDetector";
    
    // MediaPipe pose landmark indices (33 landmarks total)
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
    
    private PoseLandmarker poseLandmarker;
    private boolean isInitialized = false;
    
    public MediaPipePoseDetector(Context context) {
        initializePoseLandmarker(context);
    }
    
    private void initializePoseLandmarker(Context context) {
        try {
            // Configure MediaPipe PoseLandmarker
            BaseOptions baseOptions = BaseOptions.builder()
                .setModelAssetPath("pose_landmarker_lite.task")
                .build();
            
            PoseLandmarker.PoseLandmarkerOptions options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setNumPoses(1) // Detect only one person
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setRunningMode(RunningMode.IMAGE)
                .build();
            
            poseLandmarker = PoseLandmarker.createFromOptions(context, options);
            isInitialized = true;
            Log.d(TAG, "MediaPipe PoseLandmarker initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize MediaPipe PoseLandmarker", e);
            isInitialized = false;
        }
    }
    
    public List<PointF> detectPose(Bitmap bitmap) {
        List<PointF> keypoints = new ArrayList<>();
        
        if (!isInitialized || bitmap == null) {
            Log.w(TAG, "PoseLandmarker not initialized or bitmap is null");
            return keypoints;
        }
        
        try {
            // Convert Bitmap to MPImage
            MPImage mpImage = new BitmapImageBuilder(bitmap).build();
            
            // Run pose detection
            PoseLandmarkerResult result = poseLandmarker.detect(mpImage);
            
            if (result.landmarks().isEmpty()) {
                Log.d(TAG, "No pose detected in image");
                return keypoints;
            }
            
            // Extract landmarks from the first detected pose
            List<NormalizedLandmark> landmarks = result.landmarks().get(0);
            
            // Convert normalized landmarks to pixel coordinates
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            for (NormalizedLandmark landmark : landmarks) {
                float x = landmark.x() * width;
                float y = landmark.y() * height;
                
                // Only add landmarks with sufficient visibility
                if (landmark.visibility().orElse(0.0f) > 0.5f) {
                    keypoints.add(new PointF(x, y));
                } else {
                    keypoints.add(null); // Placeholder for invisible landmarks
                }
            }
            
            Log.d(TAG, "Detected " + keypoints.size() + " pose landmarks");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during pose detection", e);
        }
        
        return keypoints;
    }
    
    // Convert pose keypoints to the 28-feature format used by your models
    public float[] keypointsToFeatures(List<PointF> keypoints, int imageWidth, int imageHeight) {
        if (keypoints.size() < 33) {
            Log.w(TAG, "Not enough keypoints detected: " + keypoints.size());
            return new float[28]; // Return zeros if not enough keypoints
        }
        
        // Extract the 14 keypoints used in your training data
        // These MUST match exactly what was used during model training
        int[] selectedKeypoints = {
            LEFT_SHOULDER, RIGHT_SHOULDER,     // 0, 1 - Shoulders
            LEFT_ELBOW, RIGHT_ELBOW,           // 2, 3 - Elbows
            LEFT_WRIST, RIGHT_WRIST,           // 4, 5 - Wrists
            LEFT_HIP, RIGHT_HIP,               // 6, 7 - Hips
            LEFT_KNEE, RIGHT_KNEE,             // 8, 9 - Knees
            LEFT_ANKLE, RIGHT_ANKLE,           // 10, 11 - Ankles
            LEFT_HEEL, RIGHT_HEEL              // 12, 13 - Heels (CORRECTED from nose/mouth)
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
        // keypoints = (keypoints - keypoints.mean()) / (keypoints.std() + 1e-8)
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
        
        // Feature extraction completed - debug logging removed for production
        
        return features;
    }
    
    // Alternative method without statistical normalization - use this if the above doesn't work
    public float[] keypointsToFeaturesAlternative(List<PointF> keypoints, int imageWidth, int imageHeight) {
        if (keypoints.size() < 33) {
            Log.w(TAG, "Not enough keypoints detected: " + keypoints.size());
            return new float[28];
        }
        
        // Same keypoint selection as above
        int[] selectedKeypoints = {
            LEFT_SHOULDER, RIGHT_SHOULDER,     // 0, 1 - Shoulders
            LEFT_ELBOW, RIGHT_ELBOW,           // 2, 3 - Elbows
            LEFT_WRIST, RIGHT_WRIST,           // 4, 5 - Wrists
            LEFT_HIP, RIGHT_HIP,               // 6, 7 - Hips
            LEFT_KNEE, RIGHT_KNEE,             // 8, 9 - Knees
            LEFT_ANKLE, RIGHT_ANKLE,           // 10, 11 - Ankles
            LEFT_HEEL, RIGHT_HEEL              // 12, 13 - Heels
        };
        
        float[] features = new float[28];
        
        // Extract coordinates WITHOUT statistical normalization
        for (int i = 0; i < 14; i++) {
            if (selectedKeypoints[i] < keypoints.size()) {
                PointF point = keypoints.get(selectedKeypoints[i]);
                
                if (point != null) {
                    // Use raw normalized coordinates [0,1] range
                    features[i * 2] = point.x / imageWidth;
                    features[i * 2 + 1] = point.y / imageHeight;
                } else {
                    features[i * 2] = 0.0f;
                    features[i * 2 + 1] = 0.0f;
                }
            }
        }
        
        // Alternative features calculation - debug logging removed for production
        
        return features;
    }
    
    // Debug method removed for production release
    
    public void close() {
        if (poseLandmarker != null) {
            poseLandmarker.close();
            isInitialized = false;
        }
    }
    
    public boolean isReady() {
        return isInitialized;
    }
} 