# PROJECT_PIVOT API Documentation

This document provides detailed information about the PROJECT_PIVOT Android application's architecture, APIs, and integration points.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Core Components](#core-components)
- [ML Model Integration](#ml-model-integration)
- [Firebase Integration](#firebase-integration)
- [Camera Integration](#camera-integration)
- [Data Models](#data-models)
- [Utilities](#utilities)

## Architecture Overview

PROJECT_PIVOT follows a modular architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                             │
├─────────────────────────────────────────────────────────────┤
│                    Business Logic                           │
├─────────────────────────────────────────────────────────────┤
│                    Data Layer                               │
├─────────────────────────────────────────────────────────────┤
│                External Services                            │
└─────────────────────────────────────────────────────────────┘
```

### Key Design Principles

- **Single Responsibility**: Each class has a single, well-defined purpose
- **Dependency Injection**: Loose coupling between components
- **Background Processing**: ML inference runs on dedicated threads
- **Lifecycle Awareness**: Proper Android lifecycle management
- **Error Handling**: Comprehensive error handling and recovery

## Core Components

### MainActivity

The main entry point for the boxing training experience.

```java
public class MainActivity extends AppCompatActivity {
    // Camera and ML integration
    private CameraX cameraX;
    private MediaPipePoseDetector poseDetector;
    private OnnxModel stanceModel;
    private OnnxModel executionModel;
    
    // Core methods
    public void startCamera();
    public void processFrame(ImageProxy image);
    public void updatePredictions(float stanceScore, float executionScore);
}
```

**Key Features:**
- Real-time camera preview
- Pose detection integration
- ML model inference
- UI updates and feedback

### ShadowboxingActivity

Advanced training mode with real-time feedback.

```java
public class ShadowboxingActivity extends AppCompatActivity {
    private SessionTracker sessionTracker;
    private AccuracyTracker accuracyTracker;
    private SoundManager soundManager;
    
    public void startWorkout();
    public void pauseWorkout();
    public void endWorkout();
    public void provideFeedback(PoseAnalysis analysis);
}
```

### DashboardActivity

User's training dashboard and progress overview.

```java
public class DashboardActivity extends AppCompatActivity {
    private FirestoreManager firestoreManager;
    private AchievementManager achievementManager;
    
    public void loadUserStats();
    public void displayRecentSessions();
    public void updateAchievements();
}
```

## ML Model Integration

### OnnxModel

Wrapper for ONNX Runtime integration.

```java
public class OnnxModel {
    private OrtSession session;
    private OrtEnvironment environment;
    
    // Core methods
    public boolean loadModel(String modelPath);
    public float[] runInference(float[] inputData);
    public void cleanup();
    
    // Performance monitoring
    public long getLastInferenceTime();
    public boolean isModelLoaded();
}
```

**Usage Example:**
```java
OnnxModel stanceModel = new OnnxModel();
stanceModel.loadModel("stance_int8.onnx");

float[] poseData = extractPoseFeatures(landmarks);
float[] results = stanceModel.runInference(poseData);
float confidence = results[0];
```

### MediaPipePoseDetector

Real-time pose detection using MediaPipe.

```java
public class MediaPipePoseDetector {
    private PoseLandmarker poseLandmarker;
    
    public interface PoseDetectionCallback {
        void onPoseDetected(List<NormalizedLandmark> landmarks);
        void onDetectionError(String error);
    }
    
    public void detectPose(ImageProxy image, PoseDetectionCallback callback);
    public float[] extractFeatures(List<NormalizedLandmark> landmarks);
}
```

**Key Features:**
- Real-time pose landmark detection
- 33 pose landmarks with 3D coordinates
- Optimized for mobile performance
- Error handling and fallback mechanisms

### PredictionSmoother

Stabilizes ML predictions for smooth user experience.

```java
public class PredictionSmoother {
    private Queue<Float> recentPredictions;
    private float smoothingFactor;
    
    public float smoothPrediction(float newPrediction);
    public void reset();
    public void setSmoothing(float factor);
}
```

## Firebase Integration

### FirestoreManager

Handles all Firestore database operations.

```java
public class FirestoreManager {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    
    // User management
    public void createUser(User user, OnCompleteListener<Void> listener);
    public void updateUser(User user, OnCompleteListener<Void> listener);
    public void getUser(String userId, OnCompleteListener<DocumentSnapshot> listener);
    
    // Session management
    public void saveSession(TrainingSession session, OnCompleteListener<DocumentReference> listener);
    public void getUserSessions(String userId, OnCompleteListener<QuerySnapshot> listener);
    
    // Achievements
    public void updateAchievements(String userId, List<Achievement> achievements);
    public void getAchievements(String userId, OnCompleteListener<QuerySnapshot> listener);
}
```

### Data Models

#### User
```java
public class User {
    private String id;
    private String name;
    private String email;
    private String profileImageUrl;
    private Date createdAt;
    private UserStats stats;
    
    // Getters and setters
}
```

#### TrainingSession
```java
public class TrainingSession {
    private String id;
    private String userId;
    private Date startTime;
    private Date endTime;
    private int duration; // in seconds
    private float averageStanceAccuracy;
    private float averageExecutionAccuracy;
    private int totalPunches;
    private List<PunchData> punches;
    
    // Getters and setters
}
```

#### Achievement
```java
public class Achievement {
    private String id;
    private String title;
    private String description;
    private String iconUrl;
    private boolean unlocked;
    private Date unlockedAt;
    private int progress;
    private int target;
    
    // Getters and setters
}
```

## Camera Integration

### CameraX Setup

```java
public class CameraManager {
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;
    private Preview preview;
    
    public void startCamera(Context context, PreviewView previewView, 
                           ImageAnalysis.Analyzer analyzer);
    public void stopCamera();
    public void switchCamera();
}
```

### Image Processing Pipeline

```java
public class ImageProcessor implements ImageAnalysis.Analyzer {
    private MediaPipePoseDetector poseDetector;
    private OnnxModel stanceModel;
    private OnnxModel executionModel;
    
    @Override
    public void analyze(@NonNull ImageProxy image) {
        // Convert image to format suitable for MediaPipe
        Bitmap bitmap = imageProxyToBitmap(image);
        
        // Detect pose landmarks
        poseDetector.detectPose(image, new PoseDetectionCallback() {
            @Override
            public void onPoseDetected(List<NormalizedLandmark> landmarks) {
                // Extract features for ML models
                float[] features = extractFeatures(landmarks);
                
                // Run inference
                float[] stanceResults = stanceModel.runInference(features);
                float[] executionResults = executionModel.runInference(features);
                
                // Update UI on main thread
                runOnUiThread(() -> updatePredictions(stanceResults, executionResults));
            }
        });
        
        image.close();
    }
}
```

## Data Models

### PoseData
```java
public class PoseData {
    private List<NormalizedLandmark> landmarks;
    private float[] features;
    private long timestamp;
    
    public float[] extractFeatures();
    public boolean isValidPose();
}
```

### PunchData
```java
public class PunchData {
    private long timestamp;
    private float stanceScore;
    private float executionScore;
    private String punchType;
    private boolean isCorrect;
    
    // Getters and setters
}
```

## Utilities

### AccuracyTracker
```java
public class AccuracyTracker {
    private List<Float> stanceScores;
    private List<Float> executionScores;
    
    public void addScore(float stance, float execution);
    public float getAverageStanceAccuracy();
    public float getAverageExecutionAccuracy();
    public void reset();
}
```

### SessionTracker
```java
public class SessionTracker {
    private long startTime;
    private long endTime;
    private List<PunchData> punches;
    private AccuracyTracker accuracyTracker;
    
    public void startSession();
    public void endSession();
    public TrainingSession generateSession();
}
```

### SoundManager
```java
public class SoundManager {
    private SoundPool soundPool;
    private Map<String, Integer> soundMap;
    
    public void loadSounds(Context context);
    public void playSound(String soundName);
    public void setVolume(float volume);
    public void cleanup();
}
```

## Performance Considerations

### Threading Model
- **Main Thread**: UI updates and user interactions
- **Background Thread**: ML inference and heavy computations
- **Camera Thread**: Image processing and analysis

### Memory Management
- Proper cleanup of ML models and resources
- Image recycling and memory optimization
- Efficient data structures for real-time processing

### Battery Optimization
- Efficient camera usage
- Optimized ML inference
- Background processing limitations

## Error Handling

### Common Error Scenarios
1. **Camera Permission Denied**: Graceful fallback and user guidance
2. **ML Model Loading Failed**: Error reporting and recovery
3. **Network Connectivity Issues**: Offline mode and data caching
4. **Device Compatibility**: Feature detection and fallbacks

### Error Recovery Strategies
```java
public class ErrorHandler {
    public static void handleCameraError(Exception e, Context context);
    public static void handleMLError(Exception e, Context context);
    public static void handleNetworkError(Exception e, Context context);
}
```

## Testing

### Unit Tests
- ML model inference accuracy
- Data processing utilities
- Business logic components

### Integration Tests
- Camera and ML pipeline
- Firebase integration
- UI interactions

### Performance Tests
- ML inference latency
- Memory usage monitoring
- Battery consumption analysis

## Security

### Data Protection
- Local processing of sensitive data
- Secure Firebase rules
- No persistent storage of camera data

### Authentication
- Google Sign-In integration
- Secure token management
- User session handling

---

For more detailed implementation examples, see the source code in the respective Java files. 