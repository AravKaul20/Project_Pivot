package com.example.project_pivot;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

import ai.onnxruntime.OrtException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private PreviewView previewView;
    private TextView stanceTextView;
    private TextView executionTextView;
    private TextView latencyTextView;
    
    private OnnxModel stanceModel;
    private OnnxModel executionModel;
    
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        previewView = findViewById(R.id.previewView);
        stanceTextView = findViewById(R.id.stanceTextView);
        executionTextView = findViewById(R.id.executionTextView);
        latencyTextView = findViewById(R.id.latencyTextView);
        
        // Start background thread
        backgroundThread = new HandlerThread("InferenceThread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        
        // Load ONNX models
        loadModels();
        
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }
    
    private void loadModels() {
        backgroundHandler.post(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                stanceModel = new OnnxModel(this, "stance_int8.onnx");
                executionModel = new OnnxModel(this, "execution_int8.onnx");
                
                long loadTime = System.currentTimeMillis() - startTime;
                Log.d(TAG, "Models loaded in " + loadTime + "ms");
                
                // Test inference latency
                testInferenceLatency();
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "Models loaded successfully!", Toast.LENGTH_SHORT).show();
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to load models", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to load models: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void testInferenceLatency() {
        try {
            // Create dummy input for testing (28 pose keypoints)
            float[] dummyInput = new float[28];
            for (int i = 0; i < 28; i++) {
                dummyInput[i] = (float) Math.random();
            }
            long[] inputShape = {1, 28};
            
            // Test stance model
            long startTime = System.nanoTime();
            stanceModel.run(dummyInput, inputShape);
            long stanceLatency = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms
            
            // Test execution model
            startTime = System.nanoTime();
            executionModel.run(dummyInput, inputShape);
            long executionLatency = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms
            
            Log.d(TAG, "Stance model latency: " + stanceLatency + "ms");
            Log.d(TAG, "Execution model latency: " + executionLatency + "ms");
            
            runOnUiThread(() -> {
                latencyTextView.setText(String.format("Latency: %dms | %dms", 
                    stanceLatency, executionLatency));
            });
            
        } catch (OrtException e) {
            Log.e(TAG, "Error testing inference latency", e);
        }
    }
    
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(this);
        
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                
                // Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                
                // Image analysis
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setTargetResolution(new android.util.Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();
                
                imageAnalysis.setAnalyzer(backgroundHandler::post, this::analyzeImage);
                
                // Camera selector - Use front camera for pose classification
                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                
                // Unbind use cases before rebinding
                cameraProvider.unbindAll();
                
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
                
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }
    
    private void analyzeImage(ImageProxy imageProxy) {
        if (isProcessing || stanceModel == null || executionModel == null) {
            imageProxy.close();
            return;
        }
        
        isProcessing = true;
        
        try {
            // For now, we'll generate dummy pose data since we don't have MediaPipe integration
            // In a real implementation, you would extract pose keypoints here
            float[] poseData = generateDummyPoseData();
            long[] inputShape = {1, 28};
            
            long startTime = System.nanoTime();
            
            // Run inference on both models
            String stancePrediction = stanceModel.predict(poseData, inputShape);
            String executionPrediction = executionModel.predict(poseData, inputShape);
            
            long totalLatency = (System.nanoTime() - startTime) / 1_000_000;
            
            // Update UI
            runOnUiThread(() -> {
                stanceTextView.setText("Stance: " + stancePrediction);
                executionTextView.setText("Execution: " + executionPrediction);
                latencyTextView.setText("Inference: " + totalLatency + "ms");
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error during inference", e);
        } finally {
            isProcessing = false;
            imageProxy.close();
        }
    }
    
    private float[] generateDummyPoseData() {
        // Generate dummy pose keypoints for testing
        // In a real implementation, this would come from MediaPipe pose detection
        float[] poseData = new float[28]; // 14 keypoints * 2 coordinates
        for (int i = 0; i < 28; i++) {
            poseData[i] = (float) (Math.random() * 2 - 1); // Random values between -1 and 1
        }
        return poseData;
    }
    
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up models
        if (stanceModel != null) {
            stanceModel.close();
        }
        if (executionModel != null) {
            executionModel.close();
        }
        
        // Clean up background thread
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping background thread", e);
            }
        }
    }
} 