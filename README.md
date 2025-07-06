# 🥊 PROJECT_PIVOT

<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="PROJECT_PIVOT Logo" width="120" height="120">
  
  **Real-time Boxing Training with AI-Powered Pose Analysis**
  
  [![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
  [![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
  [![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
  [![Build Status](https://img.shields.io/badge/Build-Passing-success.svg)](https://github.com/AravKaul20/Project_Pivot)
  
  *Transform your boxing training with real-time AI feedback*
</div>

---

## 📖 Overview

PROJECT_PIVOT is a cutting-edge Android application that revolutionizes boxing training through real-time pose analysis. Using advanced machine learning models and computer vision, it provides instant feedback on your boxing stance and execution, helping you perfect your technique like never before.

## 📋 Table of Contents

- [🚀 Features](#-features)
- [📱 Screenshots](#-screenshots)
- [🏗️ Architecture](#️-architecture)
- [🛠️ Setup & Build](#️-setup--build)
- [📋 Permissions](#-permissions)
- [🎯 Usage](#-usage)
- [🔧 Technical Details](#-technical-details)
- [🚧 Future Enhancements](#-future-enhancements)
- [🧪 Testing](#-testing)
- [🐛 Troubleshooting](#-troubleshooting)
- [📊 Model Details](#-model-details)
- [🤝 Contributing](#-contributing)
- [📄 License](#-license)

## 🚀 Features

- **Real-time Camera Feed**: Live camera preview using CameraX
- **Dual Model Inference**: Simultaneous stance and execution classification
- **Optimized Performance**: INT8-quantized ONNX models for fast inference
- **Low Latency**: ~1-2ms inference time per model
- **Background Processing**: Non-blocking inference on dedicated thread
- **Modern UI**: Clean, minimal interface with live prediction updates

## 📱 Screenshots

The app displays:
- Full-screen camera preview
- Real-time stance classification (Correct/Incorrect + confidence)
- Real-time execution classification (Correct/Incorrect + confidence)
- Inference latency metrics

## 🏗️ Architecture

### Models
- **Stance Model**: `stance_int8.onnx` (113KB, >95% accuracy)
- **Execution Model**: `execution_int8.onnx` (113KB, >95% accuracy)
- **Input**: 28 features (14 pose keypoints × 2 coordinates)
- **Output**: Binary classification (correct/incorrect) with confidence

### Components
- **MainActivity.java**: Camera integration, UI updates, lifecycle management
- **OnnxModel.java**: ONNX Runtime wrapper for model loading and inference
- **activity_main.xml**: UI layout with camera preview and prediction displays

### Dependencies
- ONNX Runtime Android: 1.16.0
- CameraX: 1.3.1 (Core, Camera2, Lifecycle, View)
- ConstraintLayout: 2.1.4
- AppCompat: 1.6.1

## 🛠️ Setup & Build

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK API 24+ (Android 7.0)
- Physical device or emulator with camera support

### Build Instructions

1. **Clone & Open Project**
   ```bash
   cd PROJECT_PIVOT
   # Open in Android Studio
   ```

2. **Sync Dependencies**
   - Open `app/build.gradle`
   - Click "Sync Now" when prompted
   - Ensure all dependencies download successfully

3. **Verify Assets**
   - Confirm `app/src/main/assets/` contains:
     - `stance_int8.onnx`
     - `execution_int8.onnx`

4. **Build APK**
   ```bash
   ./gradlew assembleDebug
   # Or use Android Studio: Build → Build Bundle(s)/APK(s) → Build APK(s)
   ```

5. **Install & Run**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   # Or use Android Studio: Run → Run 'app'
   ```

## 📋 Permissions

The app requires the following permissions:
- **Camera**: For live video feed and pose detection
- Auto-granted on first launch or manually in Settings

## 🎯 Usage

1. **Launch App**: Open PROJECT_PIVOT on your device
2. **Grant Permission**: Allow camera access when prompted
3. **View Predictions**: See real-time stance and execution classifications
4. **Monitor Performance**: Check inference latency in bottom panel

## 🔧 Technical Details

### Model Integration
- Models loaded asynchronously on app startup
- Inference runs on dedicated background thread
- UI updates dispatched to main thread
- Automatic resource cleanup on app exit

### Performance Metrics
- **Model Loading**: ~100-200ms (one-time)
- **Inference Latency**: ~1-2ms per model
- **Total Pipeline**: <5ms end-to-end
- **Memory Usage**: ~10MB (models + runtime)

### Current Implementation
The current version uses **dummy pose data** for demonstration. For production use, integrate MediaPipe or similar pose detection library to extract real keypoints from camera frames.

## 🚧 Future Enhancements

- [ ] **MediaPipe Integration**: Replace dummy data with real pose detection
- [ ] **Recording Mode**: Save predictions with timestamps
- [ ] **Statistics**: Track accuracy over time
- [ ] **Model Updates**: Hot-swap models without app restart
- [ ] **Multi-person**: Support multiple people in frame

## 🧪 Testing

### Smoke Test
1. Install APK on device
2. Launch app → Should show camera permission dialog
3. Grant permission → Should show live camera feed
4. Verify predictions updating continuously
5. Check latency metrics in bottom panel

### Performance Test
1. Monitor CPU usage during inference
2. Verify smooth 30fps camera preview
3. Confirm <5ms total latency
4. Test on different device specs

## 🐛 Troubleshooting

### Common Issues

**Camera not working:**
- Check permissions in device Settings
- Ensure camera is not used by another app
- Restart app or device

**Models not loading:**
- Verify ONNX files in assets folder
- Check Android Studio Build → Clean Project
- View logcat for detailed error messages

**Performance issues:**
- Close other camera apps
- Ensure sufficient device memory
- Consider using emulator with host GPU

### Debug Commands
```bash
# View logs
adb logcat | grep -E "(MainActivity|OnnxModel)"

# Check APK contents
unzip -l app-debug.apk | grep assets

# Monitor performance
adb shell top | grep project_pivot
```

## 📊 Model Details

### Training Dataset
- **Stance**: 355 samples (196 correct, 159 incorrect)
- **Execution**: 243 samples (118 correct, 125 incorrect)
- **Format**: MediaPipe pose keypoints (28 features)
- **Accuracy**: >95% validation accuracy

### Model Architecture
- MobileNetV2-inspired CNN adapted for 1D pose data
- 3 convolutional layers + 2 fully connected layers
- ReLU activation, dropout regularization
- Binary classification output

## 🤝 Contributing

We welcome contributions to PROJECT_PIVOT! Whether you're fixing bugs, adding features, or improving documentation, your help is appreciated.

### How to Contribute

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Commit your changes** (`git commit -m 'Add some amazing feature'`)
4. **Push to the branch** (`git push origin feature/amazing-feature`)
5. **Open a Pull Request**

Please read our [Contributing Guidelines](CONTRIBUTING.md) for detailed information about our development process, coding standards, and how to submit pull requests.

### Development Setup

```bash
# Clone the repository
git clone https://github.com/AravKaul20/Project_Pivot.git
cd Project_Pivot

# Open in Android Studio
# File → Open → Select the project directory
```

### Areas We Need Help With

- 🐛 **Bug fixes** - Help us identify and fix issues
- 🚀 **Performance optimizations** - Improve app speed and efficiency
- 🎨 **UI/UX improvements** - Make the app more beautiful and user-friendly
- 📱 **Device compatibility** - Test on different Android devices
- 🧪 **Testing** - Add unit tests and integration tests
- 📚 **Documentation** - Improve guides and API documentation

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2025 Arav Kaul - PROJECT_PIVOT

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<div align="center">
  
  **Built with ❤️ using Android Studio, ONNX Runtime, and CameraX**
  
  [⭐ Star this project](https://github.com/AravKaul20/Project_Pivot) • [🐛 Report Bug](https://github.com/AravKaul20/Project_Pivot/issues) • [✨ Request Feature](https://github.com/AravKaul20/Project_Pivot/issues)
  
  Made by [Arav Kaul](https://github.com/AravKaul20) with passion for boxing and AI 🥊🤖
  
</div> 