package com.projectpivot.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
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
import java.util.List;
import java.util.concurrent.ExecutionException;

import ai.onnxruntime.OrtException;

import android.animation.ObjectAnimator;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private PreviewView previewView;
    private PoseOverlayView poseOverlayView;
    private TextView stanceTextView;
    private TextView executionTextView;
    private TextView latencyTextView;
    private TextView punchStatsTextView;
    private TextView modeIndicator;
    private TextView backButton;
    private TextView settingsButton;
    private TextView workoutTimer;
    private TextView startStopButton;
    private androidx.cardview.widget.CardView liveTipsToggle;
    private TextView liveTipsStatus;
    private androidx.cardview.widget.CardView liveTipCard;
    private TextView liveTipText;
    private TextView closeTipButton;
    private View stanceIndicator;
    private View executionIndicator;
    private TextView shadowboxingButton;
    private TextView tipsButton;
    
    private OnnxModel stanceModel;
    private OnnxModel executionModel;
    private MediaPipePoseDetector poseDetector;
    private FallbackPoseDetector fallbackPoseDetector;
    private PunchAnalyzer punchAnalyzer;
    private PredictionSmoother predictionSmoother;
    private SessionTracker sessionTracker;
    
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private Handler uiHandler = new Handler(Looper.getMainLooper());
    
    private boolean isProcessing = false;
    private String analysisMode = "both"; // "stance", "execution", or "both"
    
    // Workout functionality
    private boolean isWorkoutActive = false;
    private long workoutStartTime = 0;
    private boolean liveTipsEnabled = true;
    private String[] motivationalTips = {
        "Keep your guard up! Protect that chin!",
        "Stay light on your feet - dance around!",
        "Breathe with your punches - exhale on impact!",
        "Nice form! Keep those elbows tucked!",
        "Rotate those hips for more power!",
        "Good stance! Stay balanced and ready!",
        "Focus on your footwork - stay mobile!",
        "Great technique! Keep it up!",
        "Remember to snap your punches back!",
        "Excellent defense positioning!"
    };
    private int currentTipIndex = 0;

    @Override
    @ExperimentalGetImage
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        previewView = findViewById(R.id.previewView);
        poseOverlayView = findViewById(R.id.poseOverlayView);
        stanceTextView = findViewById(R.id.stanceTextView);
        executionTextView = findViewById(R.id.executionTextView);
        latencyTextView = findViewById(R.id.latencyTextView);
        punchStatsTextView = findViewById(R.id.punchStatsTextView);
        modeIndicator = findViewById(R.id.modeIndicator);
        backButton = findViewById(R.id.backButton);
        settingsButton = findViewById(R.id.settingsButton);
        workoutTimer = findViewById(R.id.workoutTimer);
        startStopButton = findViewById(R.id.startStopButton);
        liveTipsToggle = findViewById(R.id.liveTipsToggle);
        liveTipsStatus = findViewById(R.id.liveTipsStatus);
        liveTipCard = findViewById(R.id.liveTipCard);
        liveTipText = findViewById(R.id.liveTipText);
        closeTipButton = findViewById(R.id.closeTipButton);
        stanceIndicator = findViewById(R.id.stanceIndicator);
        executionIndicator = findViewById(R.id.executionIndicator);
        shadowboxingButton = findViewById(R.id.shadowboxingButton);
        tipsButton = findViewById(R.id.tipsButton);
        
        // Get analysis mode from intent
        String intentMode = getIntent().getStringExtra("analysis_mode");
        if (intentMode != null) {
            analysisMode = intentMode;
            updateModeUI();
        }
        
        // Setup all click listeners
        setupClickListeners();
        
        // Initialize pose detector, prediction smoother, punch analyzer, and session tracker
        poseDetector = new MediaPipePoseDetector(this);
        fallbackPoseDetector = new FallbackPoseDetector();
        predictionSmoother = new PredictionSmoother();
        punchAnalyzer = PunchAnalyzer.getInstance(this);
        sessionTracker = SessionTracker.getInstance(this);
        
        // Start background thread
        backgroundThread = new HandlerThread("InferenceThread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        
        // Load ONNX models
        loadModels();
        
        // Start live tips if enabled
        if (liveTipsEnabled) {
            startLiveTips();
        }
        
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }
    
    private void setupClickListeners() {
        // Setup back button
        backButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            finish();
            // Removed annoying transition animation
        });
        
        // Setup settings button
        settingsButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            android.content.Intent intent = new android.content.Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            // Removed annoying transition animation
        });
        
        // Setup workout start/stop button
        startStopButton.setOnClickListener(v -> {
            SoundManager soundManager = SoundManager.getInstance(this);
            if (isWorkoutActive) {
                soundManager.hapticSelect();
                stopWorkout();
            } else {
                soundManager.hapticSelect();
                startWorkout();
            }
        });
        
        // Setup live tips toggle
        liveTipsToggle.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            liveTipsEnabled = !liveTipsEnabled;
            updateLiveTipsUI();
            if (liveTipsEnabled && isWorkoutActive) {
                startLiveTips();
            } else {
                liveTipCard.setVisibility(View.GONE);
            }
        });
        
        // Setup close tip button
        closeTipButton.setOnClickListener(v -> {
            liveTipCard.setVisibility(View.GONE);
        });
        
        // Setup shadowboxing button
        shadowboxingButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            android.content.Intent intent = new android.content.Intent(MainActivity.this, ShadowboxingActivity.class);
            startActivity(intent);
            // Removed annoying transition animation
        });
        
        // Setup tips button
        tipsButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            android.content.Intent intent = new android.content.Intent(MainActivity.this, BoxingTipsActivity.class);
            startActivity(intent);
            // Removed annoying transition animation
        });
    }
    
    private void startWorkout() {
        isWorkoutActive = true;
        workoutStartTime = System.currentTimeMillis();
        startStopButton.setText("STOP_WORKOUT");
        startStopButton.setTextColor(getColor(R.color.error_red));
        
        // Reset punch analyzer for new session
        punchAnalyzer.resetSession();
        
        // Start session tracking
        sessionTracker.startSession(analysisMode);
        
        // Start workout timer
        updateWorkoutTimer();
        
        // Start live tips if enabled
        if (liveTipsEnabled) {
            startLiveTips();
        }
        
        android.widget.Toast.makeText(this, "Workout started! Stay focused!", android.widget.Toast.LENGTH_SHORT).show();
    }
    
    private void stopWorkout() {
        isWorkoutActive = false;
        startStopButton.setText("START_WORKOUT");
        startStopButton.setTextColor(getColor(R.color.accent_green));
        
        // Hide live tips and punch stats
        liveTipCard.setVisibility(View.GONE);
        punchStatsTextView.setVisibility(View.GONE);
        
        // End session tracking (this saves all data)
        endSession();
        
        // Save punch session data
        punchAnalyzer.saveSessionData();
        
        long workoutDuration = (System.currentTimeMillis() - workoutStartTime) / 1000;
        String punchSummary = punchAnalyzer.getTotalPunches() > 0 ? 
            " | " + punchAnalyzer.getTotalPunches() + " punches" : "";
        
        android.widget.Toast.makeText(this, "Workout completed! Duration: " + formatTime(workoutDuration) + punchSummary, android.widget.Toast.LENGTH_LONG).show();
    }
    
    private void updateWorkoutTimer() {
        if (isWorkoutActive) {
            long elapsed = (System.currentTimeMillis() - workoutStartTime) / 1000;
            workoutTimer.setText(formatTime(elapsed));
            
            // Update every second
            uiHandler.postDelayed(this::updateWorkoutTimer, 1000);
        }
    }
    
    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
    
    private void startLiveTips() {
        if (!liveTipsEnabled || !isWorkoutActive) return;
        
        // Show random tip every 15-30 seconds
        int delay = 15000 + (int) (Math.random() * 15000); // 15-30 seconds
        
        uiHandler.postDelayed(() -> {
            if (isWorkoutActive && liveTipsEnabled) {
                showRandomTip();
                startLiveTips(); // Schedule next tip
            }
        }, delay);
    }
    
    private void showRandomTip() {
        if (liveTipCard.getVisibility() == View.VISIBLE) return; // Don't show if already visible
        
        currentTipIndex = (int) (Math.random() * motivationalTips.length);
        liveTipText.setText(motivationalTips[currentTipIndex]);
        
        // Animate tip appearance
        liveTipCard.setAlpha(0f);
        liveTipCard.setVisibility(View.VISIBLE);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(liveTipCard, "alpha", 0f, 1f);
        fadeIn.setDuration(500);
        fadeIn.start();
        
        // Auto-hide after 5 seconds
        uiHandler.postDelayed(() -> {
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(liveTipCard, "alpha", 1f, 0f);
            fadeOut.setDuration(500);
            fadeOut.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    liveTipCard.setVisibility(View.GONE);
                }
            });
            fadeOut.start();
        }, 5000);
    }
    
    private void updateLiveTipsUI() {
        liveTipsStatus.setText(liveTipsEnabled ? "TIPS_ON" : "TIPS_OFF");
        liveTipsStatus.setTextColor(liveTipsEnabled ? getColor(R.color.accent_green) : getColor(R.color.text_secondary));
    }
    
    private void updateModeUI() {
        if (modeIndicator != null) {
            switch (analysisMode) {
                case "stance":
                    modeIndicator.setText("● STANCE_ANALYSIS_MODE");
                    modeIndicator.setTextColor(getColor(R.color.accent_green));
                    // Hide execution text view
                    if (executionTextView != null) {
                        findViewById(R.id.executionCard).setVisibility(android.view.View.GONE);
                    }
                    break;
                case "execution":
                    modeIndicator.setText("● EXECUTION_ANALYSIS_MODE");
                    modeIndicator.setTextColor(getColor(R.color.accent_green));
                    // Hide stance text view
                    if (stanceTextView != null) {
                        findViewById(R.id.stanceCard).setVisibility(android.view.View.GONE);
                    }
                    break;
                default:
                    modeIndicator.setText("● FULL_ANALYSIS_MODE");
                    modeIndicator.setTextColor(getColor(R.color.accent_green));
                    break;
            }
        }
    }
    
    private void updateAnalysisIndicators(String stancePrediction, String executionPrediction) {
        // Update stance indicator
        if (stanceIndicator != null) {
            if (stancePrediction.toLowerCase().contains("correct")) {
                stanceIndicator.setBackgroundColor(getColor(R.color.accent_green));
            } else if (stancePrediction.toLowerCase().contains("incorrect")) {
                stanceIndicator.setBackgroundColor(getColor(R.color.error_red));
            } else {
                stanceIndicator.setBackgroundColor(getColor(R.color.text_secondary));
            }
        }
        
        // Update execution indicator
        if (executionIndicator != null) {
            if (executionPrediction.toLowerCase().contains("correct")) {
                executionIndicator.setBackgroundColor(getColor(R.color.accent_green));
            } else if (executionPrediction.toLowerCase().contains("incorrect")) {
                executionIndicator.setBackgroundColor(getColor(R.color.error_red));
            } else {
                executionIndicator.setBackgroundColor(getColor(R.color.text_secondary));
            }
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
                
                // Models loaded successfully
                
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
    
    @ExperimentalGetImage
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
    
    @ExperimentalGetImage
    private void analyzeImage(ImageProxy imageProxy) {
        if (isProcessing || stanceModel == null || executionModel == null) {
            imageProxy.close();
            return;
        }
        
        // Skip processing if smoother says we should wait
        if (!predictionSmoother.shouldUpdate()) {
            imageProxy.close();
            return;
        }
        
        isProcessing = true;
        
        try {
            // Convert ImageProxy to Bitmap
            Bitmap bitmap = imageProxyToBitmap(imageProxy);
            if (bitmap == null) {
                return;
            }
            
            // Detect pose keypoints with fallback
            List<PointF> keypoints;
            float[] poseData;
            
            if (poseDetector.isReady()) {
                keypoints = poseDetector.detectPose(bitmap);
                poseData = poseDetector.keypointsToFeatures(keypoints, bitmap.getWidth(), bitmap.getHeight());
            } else {
                // Use fallback detector if MediaPipe is not ready
                keypoints = fallbackPoseDetector.detectPose(bitmap);
                poseData = fallbackPoseDetector.keypointsToFeatures(keypoints, bitmap.getWidth(), bitmap.getHeight());
            }
            
            // Update pose overlay on UI thread
            runOnUiThread(() -> {
                poseOverlayView.updatePose(keypoints, bitmap.getWidth(), bitmap.getHeight());
            });
            
            // Analyze punches if enabled
            if (punchAnalyzer.isPunchCounterEnabled()) {
                punchAnalyzer.analyzePose(keypoints);
            }
            long[] inputShape = {1, 28};
            
            long startTime = System.nanoTime();
            
            // Run inference based on analysis mode
            float[] stanceLogits = null;
            float[] executionLogits = null;
            String stancePrediction = "–";
            String executionPrediction = "–";
            
            if (analysisMode.equals("stance") || analysisMode.equals("both")) {
                stanceLogits = stanceModel.run(poseData, inputShape);
                stancePrediction = predictionSmoother.getSmoothedStance(stanceLogits);
                
                // Record stance accuracy for session tracking
                if (sessionTracker.isSessionActive()) {
                    sessionTracker.recordStanceAccuracy(stanceLogits, stancePrediction);
                }
            }
            
            if (analysisMode.equals("execution") || analysisMode.equals("both")) {
                executionLogits = executionModel.run(poseData, inputShape);
                executionPrediction = predictionSmoother.getSmoothedExecution(executionLogits);
                
                // Record execution accuracy for session tracking
                if (sessionTracker.isSessionActive()) {
                    sessionTracker.recordExecutionAccuracy(executionLogits, executionPrediction);
                }
            }
            
            long totalLatency = (System.nanoTime() - startTime) / 1_000_000;
            
            // Update UI
            final String finalStancePrediction = stancePrediction;
            final String finalExecutionPrediction = executionPrediction;
            final String punchStats = punchAnalyzer.getFormattedStats();
            runOnUiThread(() -> {
                stanceTextView.setText("Stance: " + finalStancePrediction);
                executionTextView.setText("Execution: " + finalExecutionPrediction);
                latencyTextView.setText("Inference: " + totalLatency + "ms | Keypoints: " + keypoints.size());
                
                // Show punch stats if punch counter is enabled and workout is active
                if (punchAnalyzer.isPunchCounterEnabled() && isWorkoutActive) {
                    punchStatsTextView.setText(punchStats);
                    punchStatsTextView.setVisibility(View.VISIBLE);
                } else {
                    punchStatsTextView.setVisibility(View.GONE);
                }
            });
            
            // Update analysis indicators
            updateAnalysisIndicators(finalStancePrediction, finalExecutionPrediction);
            
        } catch (Exception e) {
            Log.e(TAG, "Error during inference", e);
        } finally {
            isProcessing = false;
            imageProxy.close();
        }
    }
    
    @ExperimentalGetImage
    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        try {
            Image.Plane[] planes = imageProxy.getImage().getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, 
                imageProxy.getWidth(), imageProxy.getHeight(), null);
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, 
                imageProxy.getWidth(), imageProxy.getHeight()), 100, out);
            
            byte[] imageBytes = out.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            
            // Rotate bitmap if needed (front camera is usually mirrored)
            Matrix matrix = new Matrix();
            matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());
            matrix.postScale(-1, 1); // Mirror for front camera
            
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e);
            return null;
        }
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
    @ExperimentalGetImage
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
        if (poseDetector != null) {
            poseDetector.close();
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void endSession() {
        if (sessionTracker.isSessionActive()) {
            sessionTracker.endSession();
            Log.d(TAG, "Workout session ended and saved to local storage and Firebase cloud");
            showToast("Workout completed! Data saved to cloud ☁️");
            
            // Check for new achievements
            AchievementManager achievementManager = AchievementManager.getInstance(this);
            List<AchievementManager.Achievement> newAchievements = achievementManager.checkForNewAchievements();
            
            if (!newAchievements.isEmpty()) {
                // Show achievement notification
                for (AchievementManager.Achievement achievement : newAchievements) {
                    showToast("🏆 Achievement Unlocked: " + achievement.title + "!");
                    Log.d(TAG, "New achievement unlocked: " + achievement.title);
                }
            }
        }
    }
} 