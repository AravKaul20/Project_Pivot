package com.projectpivot.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class PoseOverlayView extends View {
    private static final String TAG = "PoseOverlayView";
    
    private Paint keypointPaint;
    private Paint connectionPaint;
    private Paint textPaint;
    
    private List<PointF> poseKeypoints;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    
    // MediaPipe pose connections (pairs of keypoint indices)
    private static final int[][] POSE_CONNECTIONS = {
        // Face
        {0, 1}, {1, 2}, {2, 3}, {3, 7},
        {0, 4}, {4, 5}, {5, 6}, {6, 8},
        // Torso
        {9, 10},
        {11, 12}, {11, 13}, {13, 15}, {15, 17}, {15, 19}, {15, 21}, {17, 19},
        {12, 14}, {14, 16}, {16, 18}, {16, 20}, {16, 22}, {18, 20},
        {11, 23}, {12, 24}, {23, 24},
        // Left leg
        {23, 25}, {25, 27}, {27, 29}, {29, 31}, {27, 31},
        // Right leg
        {24, 26}, {26, 28}, {28, 30}, {30, 32}, {28, 32}
    };
    
    public PoseOverlayView(Context context) {
        super(context);
        init();
    }
    
    public PoseOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public PoseOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // Paint for keypoints
        keypointPaint = new Paint();
        keypointPaint.setColor(Color.RED);
        keypointPaint.setStyle(Paint.Style.FILL);
        keypointPaint.setStrokeWidth(8);
        keypointPaint.setAntiAlias(true);
        
        // Paint for connections
        connectionPaint = new Paint();
        connectionPaint.setColor(Color.GREEN);
        connectionPaint.setStyle(Paint.Style.STROKE);
        connectionPaint.setStrokeWidth(4);
        connectionPaint.setAntiAlias(true);
        
        // Paint for text
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30);
        textPaint.setAntiAlias(true);
    }
    
    public void updatePose(List<PointF> keypoints, int imageWidth, int imageHeight) {
        this.poseKeypoints = keypoints;
        
        // Calculate scale factors to map from image coordinates to view coordinates
        if (getWidth() > 0 && getHeight() > 0) {
            scaleX = (float) getWidth() / imageWidth;
            scaleY = (float) getHeight() / imageHeight;
        }
        
        invalidate(); // Trigger redraw
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (poseKeypoints == null || poseKeypoints.isEmpty()) {
            return;
        }
        
        // Draw connections first (so they appear behind keypoints)
        drawConnections(canvas);
        
        // Draw keypoints
        drawKeypoints(canvas);
        
        // Draw keypoint count
        canvas.drawText("Keypoints: " + poseKeypoints.size(), 20, 50, textPaint);
    }
    
    private void drawConnections(Canvas canvas) {
        for (int[] connection : POSE_CONNECTIONS) {
            int startIdx = connection[0];
            int endIdx = connection[1];
            
            if (startIdx < poseKeypoints.size() && endIdx < poseKeypoints.size()) {
                PointF start = poseKeypoints.get(startIdx);
                PointF end = poseKeypoints.get(endIdx);
                
                if (start != null && end != null) {
                    float startX = start.x * scaleX;
                    float startY = start.y * scaleY;
                    float endX = end.x * scaleX;
                    float endY = end.y * scaleY;
                    
                    canvas.drawLine(startX, startY, endX, endY, connectionPaint);
                }
            }
        }
    }
    
    private void drawKeypoints(Canvas canvas) {
        for (int i = 0; i < poseKeypoints.size(); i++) {
            PointF keypoint = poseKeypoints.get(i);
            if (keypoint != null) {
                float x = keypoint.x * scaleX;
                float y = keypoint.y * scaleY;
                
                // Draw keypoint circle
                canvas.drawCircle(x, y, 8, keypointPaint);
                
                // Draw keypoint index (for debugging)
                canvas.drawText(String.valueOf(i), x + 10, y - 10, textPaint);
            }
        }
    }
    
    public void clearPose() {
        poseKeypoints = null;
        invalidate();
    }
} 