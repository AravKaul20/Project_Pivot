package com.projectpivot.app;

import android.graphics.PointF;
import android.util.Log;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.List;

/**
 * Utility class for calculating joint angles from MediaPipe pose landmarks
 * Used for enhanced boxing form analysis
 */
public class JointAngleCalculator {
    private static final String TAG = "JointAngleCalculator";
    
    /**
     * Calculate angle between three points (joint angle)
     * @param point1 First point (e.g., shoulder)
     * @param joint Joint point (e.g., elbow)
     * @param point2 Second point (e.g., wrist)
     * @return Angle in radians
     */
    public static float calculateJointAngle(PointF point1, PointF joint, PointF point2) {
        if (point1 == null || joint == null || point2 == null) {
            return 0.0f;
        }
        
        // Vector from joint to point1
        float vec1X = point1.x - joint.x;
        float vec1Y = point1.y - joint.y;
        
        // Vector from joint to point2
        float vec2X = point2.x - joint.x;
        float vec2Y = point2.y - joint.y;
        
        // Calculate angle between vectors
        return calculateAngleBetweenVectors(vec1X, vec1Y, vec2X, vec2Y);
    }
    
    /**
     * Calculate angle between three 3D points using NormalizedLandmark
     */
    public static float calculateJointAngle3D(NormalizedLandmark point1, NormalizedLandmark joint, NormalizedLandmark point2) {
        if (point1 == null || joint == null || point2 == null) {
            return 0.0f;
        }
        
        // Vector from joint to point1
        float vec1X = point1.x() - joint.x();
        float vec1Y = point1.y() - joint.y();
        float vec1Z = point1.z() - joint.z();
        
        // Vector from joint to point2
        float vec2X = point2.x() - joint.x();
        float vec2Y = point2.y() - joint.y();
        float vec2Z = point2.z() - joint.z();
        
        // Calculate angle between 3D vectors
        return calculateAngleBetweenVectors3D(vec1X, vec1Y, vec1Z, vec2X, vec2Y, vec2Z);
    }
    
    /**
     * Calculate angle between two 2D vectors
     */
    private static float calculateAngleBetweenVectors(float vec1X, float vec1Y, float vec2X, float vec2Y) {
        // Calculate dot product
        float dotProduct = vec1X * vec2X + vec1Y * vec2Y;
        
        // Calculate magnitudes
        float mag1 = (float) Math.sqrt(vec1X * vec1X + vec1Y * vec1Y);
        float mag2 = (float) Math.sqrt(vec2X * vec2X + vec2Y * vec2Y);
        
        // Avoid division by zero
        if (mag1 == 0 || mag2 == 0) {
            return 0.0f;
        }
        
        // Calculate angle
        float cosAngle = dotProduct / (mag1 * mag2);
        
        // Clamp to valid range for acos
        cosAngle = Math.max(-1.0f, Math.min(1.0f, cosAngle));
        
        return (float) Math.acos(cosAngle);
    }
    
    /**
     * Calculate angle between two 3D vectors
     */
    private static float calculateAngleBetweenVectors3D(float vec1X, float vec1Y, float vec1Z, 
                                                       float vec2X, float vec2Y, float vec2Z) {
        // Calculate dot product
        float dotProduct = vec1X * vec2X + vec1Y * vec2Y + vec1Z * vec2Z;
        
        // Calculate magnitudes
        float mag1 = (float) Math.sqrt(vec1X * vec1X + vec1Y * vec1Y + vec1Z * vec1Z);
        float mag2 = (float) Math.sqrt(vec2X * vec2X + vec2Y * vec2Y + vec2Z * vec2Z);
        
        // Avoid division by zero
        if (mag1 == 0 || mag2 == 0) {
            return 0.0f;
        }
        
        // Calculate angle
        float cosAngle = dotProduct / (mag1 * mag2);
        
        // Clamp to valid range for acos
        cosAngle = Math.max(-1.0f, Math.min(1.0f, cosAngle));
        
        return (float) Math.acos(cosAngle);
    }
    
    /**
     * Calculate all joint angles for boxing analysis
     * @param keypoints List of pose keypoints
     * @return Array of joint angles
     */
    public static float[] calculateAllJointAngles(List<PointF> keypoints) {
        if (keypoints.size() < 33) {
            Log.w(TAG, "Not enough keypoints for joint angle calculation");
            return new float[14]; // Return zeros
        }
        
        float[] angles = new float[14];
        int index = 0;
        
        try {
            // Elbow angles (most important for boxing)
            angles[index++] = calculateJointAngle(
                keypoints.get(MediaPipePoseDetector.LEFT_SHOULDER),
                keypoints.get(MediaPipePoseDetector.LEFT_ELBOW),
                keypoints.get(MediaPipePoseDetector.LEFT_WRIST)
            );
            
            angles[index++] = calculateJointAngle(
                keypoints.get(MediaPipePoseDetector.RIGHT_SHOULDER),
                keypoints.get(MediaPipePoseDetector.RIGHT_ELBOW),
                keypoints.get(MediaPipePoseDetector.RIGHT_WRIST)
            );
            
            // Shoulder angles
            angles[index++] = calculateJointAngle(
                keypoints.get(MediaPipePoseDetector.LEFT_EAR),
                keypoints.get(MediaPipePoseDetector.LEFT_SHOULDER),
                keypoints.get(MediaPipePoseDetector.LEFT_ELBOW)
            );
            
            angles[index++] = calculateJointAngle(
                keypoints.get(MediaPipePoseDetector.RIGHT_EAR),
                keypoints.get(MediaPipePoseDetector.RIGHT_SHOULDER),
                keypoints.get(MediaPipePoseDetector.RIGHT_ELBOW)
            );
            
            // Hip angles
            angles[index++] = calculateJointAngle(
                keypoints.get(MediaPipePoseDetector.LEFT_SHOULDER),
                keypoints.get(MediaPipePoseDetector.LEFT_HIP),
                keypoints.get(MediaPipePoseDetector.LEFT_KNEE)
            );
            
            angles[index++] = calculateJointAngle(
                keypoints.get(MediaPipePoseDetector.RIGHT_SHOULDER),
                keypoints.get(MediaPipePoseDetector.RIGHT_HIP),
                keypoints.get(MediaPipePoseDetector.RIGHT_KNEE)
            );
            
            // Knee angles
            angles[index++] = calculateJointAngle(
                keypoints.get(MediaPipePoseDetector.LEFT_HIP),
                keypoints.get(MediaPipePoseDetector.LEFT_KNEE),
                keypoints.get(MediaPipePoseDetector.LEFT_ANKLE)
            );
            
            angles[index++] = calculateJointAngle(
                keypoints.get(MediaPipePoseDetector.RIGHT_HIP),
                keypoints.get(MediaPipePoseDetector.RIGHT_KNEE),
                keypoints.get(MediaPipePoseDetector.RIGHT_ANKLE)
            );
            
            // Ankle angles
            angles[index++] = calculateJointAngle(
                keypoints.get(MediaPipePoseDetector.LEFT_KNEE),
                keypoints.get(MediaPipePoseDetector.LEFT_ANKLE),
                keypoints.get(MediaPipePoseDetector.LEFT_HEEL)
            );
            
            angles[index++] = calculateJointAngle(
                keypoints.get(MediaPipePoseDetector.RIGHT_KNEE),
                keypoints.get(MediaPipePoseDetector.RIGHT_ANKLE),
                keypoints.get(MediaPipePoseDetector.RIGHT_HEEL)
            );
            
            // Wrist angles (approximate)
            angles[index++] = calculateJointAngle(
                keypoints.get(MediaPipePoseDetector.LEFT_ELBOW),
                keypoints.get(MediaPipePoseDetector.LEFT_WRIST),
                keypoints.get(MediaPipePoseDetector.LEFT_INDEX)
            );
            
            angles[index++] = calculateJointAngle(
                keypoints.get(MediaPipePoseDetector.RIGHT_ELBOW),
                keypoints.get(MediaPipePoseDetector.RIGHT_WRIST),
                keypoints.get(MediaPipePoseDetector.RIGHT_INDEX)
            );
            
            // Spine angles (torso alignment)
            angles[index++] = calculateJointAngle(
                keypoints.get(MediaPipePoseDetector.NOSE),
                keypoints.get(MediaPipePoseDetector.LEFT_SHOULDER),
                keypoints.get(MediaPipePoseDetector.LEFT_HIP)
            );
            
            angles[index++] = calculateJointAngle(
                keypoints.get(MediaPipePoseDetector.NOSE),
                keypoints.get(MediaPipePoseDetector.RIGHT_SHOULDER),
                keypoints.get(MediaPipePoseDetector.RIGHT_HIP)
            );
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating joint angles", e);
        }
        
        return angles;
    }
    
    /**
     * Calculate distance between two points
     */
    public static float calculateDistance(PointF point1, PointF point2) {
        if (point1 == null || point2 == null) {
            return 0.0f;
        }
        
        float dx = point2.x - point1.x;
        float dy = point2.y - point1.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Calculate 3D distance between two normalized landmarks
     */
    public static float calculateDistance3D(NormalizedLandmark point1, NormalizedLandmark point2) {
        if (point1 == null || point2 == null) {
            return 0.0f;
        }
        
        float dx = point2.x() - point1.x();
        float dy = point2.y() - point1.y();
        float dz = point2.z() - point1.z();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
} 