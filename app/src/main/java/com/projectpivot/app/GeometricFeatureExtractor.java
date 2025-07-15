package com.projectpivot.app;

import android.graphics.PointF;
import android.util.Log;

import java.util.List;

/**
 * Utility class for extracting geometric features from pose landmarks
 * Includes distances, ratios, and orientations for enhanced boxing analysis
 */
public class GeometricFeatureExtractor {
    private static final String TAG = "GeometricFeatureExtractor";
    
    /**
     * Extract all geometric features for boxing analysis
     * @param keypoints List of pose keypoints
     * @return Array of geometric features
     */
    public static float[] extractGeometricFeatures(List<PointF> keypoints) {
        if (keypoints.size() < 33) {
            Log.w(TAG, "Not enough keypoints for geometric feature extraction");
            return new float[20]; // Return zeros
        }
        
        float[] features = new float[20];
        int index = 0;
        
        try {
            // Distance features (12 features)
            float[] distances = extractDistanceFeatures(keypoints);
            System.arraycopy(distances, 0, features, index, distances.length);
            index += distances.length;
            
            // Ratio features (8 features)
            float[] ratios = extractRatioFeatures(keypoints);
            System.arraycopy(ratios, 0, features, index, ratios.length);
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting geometric features", e);
        }
        
        return features;
    }
    
    /**
     * Extract distance-based features
     */
    private static float[] extractDistanceFeatures(List<PointF> keypoints) {
        float[] distances = new float[12];
        int index = 0;
        
        // Stance width (distance between feet)
        distances[index++] = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.LEFT_ANKLE),
            keypoints.get(MediaPipePoseDetector.RIGHT_ANKLE)
        );
        
        // Shoulder width
        distances[index++] = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.LEFT_SHOULDER),
            keypoints.get(MediaPipePoseDetector.RIGHT_SHOULDER)
        );
        
        // Hip width
        distances[index++] = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.LEFT_HIP),
            keypoints.get(MediaPipePoseDetector.RIGHT_HIP)
        );
        
        // Left arm extension (shoulder to wrist)
        distances[index++] = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.LEFT_SHOULDER),
            keypoints.get(MediaPipePoseDetector.LEFT_WRIST)
        );
        
        // Right arm extension (shoulder to wrist)
        distances[index++] = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.RIGHT_SHOULDER),
            keypoints.get(MediaPipePoseDetector.RIGHT_WRIST)
        );
        
        // Left leg length (hip to ankle)
        distances[index++] = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.LEFT_HIP),
            keypoints.get(MediaPipePoseDetector.LEFT_ANKLE)
        );
        
        // Right leg length (hip to ankle)
        distances[index++] = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.RIGHT_HIP),
            keypoints.get(MediaPipePoseDetector.RIGHT_ANKLE)
        );
        
        // Torso height (average shoulder to average hip)
        PointF avgShoulder = calculateMidpoint(
            keypoints.get(MediaPipePoseDetector.LEFT_SHOULDER),
            keypoints.get(MediaPipePoseDetector.RIGHT_SHOULDER)
        );
        PointF avgHip = calculateMidpoint(
            keypoints.get(MediaPipePoseDetector.LEFT_HIP),
            keypoints.get(MediaPipePoseDetector.RIGHT_HIP)
        );
        distances[index++] = JointAngleCalculator.calculateDistance(avgShoulder, avgHip);
        
        // Head to shoulder distance (neck length)
        distances[index++] = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.NOSE),
            avgShoulder
        );
        
        // Guard position distances (hands to face)
        distances[index++] = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.LEFT_WRIST),
            keypoints.get(MediaPipePoseDetector.NOSE)
        );
        
        distances[index++] = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.RIGHT_WRIST),
            keypoints.get(MediaPipePoseDetector.NOSE)
        );
        
        // Center of gravity estimation (hip center to ankle center)
        PointF avgAnkle = calculateMidpoint(
            keypoints.get(MediaPipePoseDetector.LEFT_ANKLE),
            keypoints.get(MediaPipePoseDetector.RIGHT_ANKLE)
        );
        distances[index++] = JointAngleCalculator.calculateDistance(avgHip, avgAnkle);
        
        return distances;
    }
    
    /**
     * Extract ratio-based features
     */
    private static float[] extractRatioFeatures(List<PointF> keypoints) {
        float[] ratios = new float[8];
        int index = 0;
        
        // Stance width to height ratio
        float stanceWidth = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.LEFT_ANKLE),
            keypoints.get(MediaPipePoseDetector.RIGHT_ANKLE)
        );
        float bodyHeight = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.NOSE),
            calculateMidpoint(
                keypoints.get(MediaPipePoseDetector.LEFT_ANKLE),
                keypoints.get(MediaPipePoseDetector.RIGHT_ANKLE)
            )
        );
        ratios[index++] = bodyHeight > 0 ? stanceWidth / bodyHeight : 0;
        
        // Arm span to height ratio
        float armSpan = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.LEFT_WRIST),
            keypoints.get(MediaPipePoseDetector.RIGHT_WRIST)
        );
        ratios[index++] = bodyHeight > 0 ? armSpan / bodyHeight : 0;
        
        // Left arm extension ratio (elbow to wrist / shoulder to elbow)
        float leftForearm = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.LEFT_ELBOW),
            keypoints.get(MediaPipePoseDetector.LEFT_WRIST)
        );
        float leftUpperArm = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.LEFT_SHOULDER),
            keypoints.get(MediaPipePoseDetector.LEFT_ELBOW)
        );
        ratios[index++] = leftUpperArm > 0 ? leftForearm / leftUpperArm : 0;
        
        // Right arm extension ratio
        float rightForearm = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.RIGHT_ELBOW),
            keypoints.get(MediaPipePoseDetector.RIGHT_WRIST)
        );
        float rightUpperArm = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.RIGHT_SHOULDER),
            keypoints.get(MediaPipePoseDetector.RIGHT_ELBOW)
        );
        ratios[index++] = rightUpperArm > 0 ? rightForearm / rightUpperArm : 0;
        
        // Leg flexion ratios (thigh to calf)
        float leftThigh = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.LEFT_HIP),
            keypoints.get(MediaPipePoseDetector.LEFT_KNEE)
        );
        float leftCalf = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.LEFT_KNEE),
            keypoints.get(MediaPipePoseDetector.LEFT_ANKLE)
        );
        ratios[index++] = leftThigh > 0 ? leftCalf / leftThigh : 0;
        
        float rightThigh = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.RIGHT_HIP),
            keypoints.get(MediaPipePoseDetector.RIGHT_KNEE)
        );
        float rightCalf = JointAngleCalculator.calculateDistance(
            keypoints.get(MediaPipePoseDetector.RIGHT_KNEE),
            keypoints.get(MediaPipePoseDetector.RIGHT_ANKLE)
        );
        ratios[index++] = rightThigh > 0 ? rightCalf / rightThigh : 0;
        
        // Symmetry ratios (left vs right)
        ratios[index++] = rightUpperArm > 0 ? leftUpperArm / rightUpperArm : 0;
        ratios[index++] = rightThigh > 0 ? leftThigh / rightThigh : 0;
        
        return ratios;
    }
    
    /**
     * Extract orientation features (directional vectors)
     */
    public static float[] extractOrientationFeatures(List<PointF> keypoints) {
        float[] orientations = new float[6];
        int index = 0;
        
        try {
            // Torso orientation (shoulder line angle)
            PointF leftShoulder = keypoints.get(MediaPipePoseDetector.LEFT_SHOULDER);
            PointF rightShoulder = keypoints.get(MediaPipePoseDetector.RIGHT_SHOULDER);
            orientations[index++] = calculateAngleFromHorizontal(leftShoulder, rightShoulder);
            
            // Hip orientation
            PointF leftHip = keypoints.get(MediaPipePoseDetector.LEFT_HIP);
            PointF rightHip = keypoints.get(MediaPipePoseDetector.RIGHT_HIP);
            orientations[index++] = calculateAngleFromHorizontal(leftHip, rightHip);
            
            // Left arm orientation (shoulder to wrist)
            PointF leftWrist = keypoints.get(MediaPipePoseDetector.LEFT_WRIST);
            orientations[index++] = calculateAngleFromHorizontal(leftShoulder, leftWrist);
            
            // Right arm orientation
            PointF rightWrist = keypoints.get(MediaPipePoseDetector.RIGHT_WRIST);
            orientations[index++] = calculateAngleFromHorizontal(rightShoulder, rightWrist);
            
            // Stance orientation (foot line angle)
            PointF leftAnkle = keypoints.get(MediaPipePoseDetector.LEFT_ANKLE);
            PointF rightAnkle = keypoints.get(MediaPipePoseDetector.RIGHT_ANKLE);
            orientations[index++] = calculateAngleFromHorizontal(leftAnkle, rightAnkle);
            
            // Body facing direction (shoulder to hip center)
            PointF shoulderCenter = calculateMidpoint(leftShoulder, rightShoulder);
            PointF hipCenter = calculateMidpoint(leftHip, rightHip);
            orientations[index++] = calculateAngleFromVertical(shoulderCenter, hipCenter);
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting orientation features", e);
        }
        
        return orientations;
    }
    
    /**
     * Calculate midpoint between two points
     */
    private static PointF calculateMidpoint(PointF point1, PointF point2) {
        if (point1 == null || point2 == null) {
            return new PointF(0, 0);
        }
        return new PointF((point1.x + point2.x) / 2, (point1.y + point2.y) / 2);
    }
    
    /**
     * Calculate angle from horizontal (0 = horizontal, π/2 = vertical)
     */
    private static float calculateAngleFromHorizontal(PointF point1, PointF point2) {
        if (point1 == null || point2 == null) {
            return 0.0f;
        }
        
        float dx = point2.x - point1.x;
        float dy = point2.y - point1.y;
        
        return (float) Math.atan2(dy, dx);
    }
    
    /**
     * Calculate angle from vertical (0 = vertical, π/2 = horizontal)
     */
    private static float calculateAngleFromVertical(PointF point1, PointF point2) {
        if (point1 == null || point2 == null) {
            return 0.0f;
        }
        
        float dx = point2.x - point1.x;
        float dy = point2.y - point1.y;
        
        return (float) Math.atan2(dx, dy);
    }
    
    /**
     * Calculate center of mass approximation
     */
    public static PointF calculateCenterOfMass(List<PointF> keypoints) {
        if (keypoints.size() < 33) {
            return new PointF(0, 0);
        }
        
        // Use key body points for center of mass estimation
        PointF shoulderCenter = calculateMidpoint(
            keypoints.get(MediaPipePoseDetector.LEFT_SHOULDER),
            keypoints.get(MediaPipePoseDetector.RIGHT_SHOULDER)
        );
        
        PointF hipCenter = calculateMidpoint(
            keypoints.get(MediaPipePoseDetector.LEFT_HIP),
            keypoints.get(MediaPipePoseDetector.RIGHT_HIP)
        );
        
        // Weighted average (torso has more mass)
        float torsoWeight = 0.6f;
        float hipWeight = 0.4f;
        
        return new PointF(
            shoulderCenter.x * torsoWeight + hipCenter.x * hipWeight,
            shoulderCenter.y * torsoWeight + hipCenter.y * hipWeight
        );
    }
} 