# Contributing to PROJECT_PIVOT

Thank you for your interest in contributing to PROJECT_PIVOT! This document provides guidelines for contributing to this real-time pose classification Android application.

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK API 24+ (Android 7.0)
- Git
- Basic knowledge of Android development, Java, and machine learning concepts

### Setting Up the Development Environment

1. **Fork the repository**
   ```bash
   git clone https://github.com/AravKaul20/Project_Pivot.git
   cd Project_Pivot
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the cloned directory

3. **Sync dependencies**
   - Wait for Gradle sync to complete
   - Ensure all dependencies are downloaded

## 🛠️ Development Guidelines

### Code Style
- Follow [Android coding standards](https://source.android.com/setup/contribute/code-style)
- Use meaningful variable and method names
- Add comments for complex logic, especially in ML-related code
- Keep methods focused and concise

### Architecture
- **MainActivity.java**: Camera integration and UI management
- **OnnxModel.java**: Machine learning model wrapper
- **MediaPipePoseDetector.java**: Pose detection logic
- **PredictionSmoother.java**: Prediction stabilization

### Testing
- Test on physical devices when possible (camera functionality)
- Verify performance on different Android versions
- Test edge cases (poor lighting, multiple people, etc.)

## 📝 How to Contribute

### Reporting Issues
1. Check existing issues first
2. Use the issue template
3. Include:
   - Android version and device model
   - Steps to reproduce
   - Expected vs actual behavior
   - Logs/screenshots if applicable

### Submitting Changes

1. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes**
   - Follow the coding guidelines
   - Add/update tests if necessary
   - Update documentation

3. **Commit your changes**
   ```bash
   git add .
   git commit -m "feat: add your feature description"
   ```

4. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```

5. **Create a Pull Request**
   - Use the PR template
   - Describe your changes clearly
   - Reference related issues

### Commit Message Format
```
type(scope): description

[optional body]

[optional footer]
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes
- `refactor`: Code refactoring
- `test`: Adding tests
- `chore`: Maintenance tasks

## 🎯 Areas for Contribution

### High Priority
- [ ] Performance optimizations
- [ ] UI/UX improvements
- [ ] Additional pose detection models
- [ ] Recording and playback features
- [ ] Statistics and analytics

### Medium Priority
- [ ] Multi-person pose detection
- [ ] Custom workout routines
- [ ] Social features
- [ ] Accessibility improvements

### Low Priority
- [ ] Internationalization
- [ ] Advanced settings
- [ ] Export functionality

## 🧪 Testing Guidelines

### Manual Testing
1. **Basic functionality**
   - App launches successfully
   - Camera permission granted
   - Live camera feed displays
   - Pose detection works

2. **Performance testing**
   - Monitor CPU/memory usage
   - Verify smooth frame rates
   - Test on different devices

3. **Edge cases**
   - Test in various lighting conditions
   - Multiple people in frame
   - Rapid movements
   - App backgrounding/foregrounding

### Automated Testing
- Unit tests for utility functions
- Integration tests for ML models
- UI tests for critical user flows

## 📚 Resources

### Documentation
- [Android Developer Guide](https://developer.android.com/guide)
- [ONNX Runtime Android](https://onnxruntime.ai/docs/get-started/with-android.html)
- [MediaPipe](https://developers.google.com/mediapipe)
- [CameraX](https://developer.android.com/training/camerax)

### Learning Resources
- [Android ML Kit](https://developers.google.com/ml-kit)
- [TensorFlow Lite](https://www.tensorflow.org/lite)
- [Computer Vision Fundamentals](https://opencv.org/)

## 🤝 Code of Conduct

### Our Standards
- Be respectful and inclusive
- Focus on constructive feedback
- Help others learn and grow
- Maintain professional communication

### Unacceptable Behavior
- Harassment or discrimination
- Spam or off-topic discussions
- Sharing sensitive information
- Disruptive behavior

## 📞 Getting Help

### Questions?
- Check the [README](README.md) first
- Search existing issues
- Create a new issue with the "question" label
- Contact the maintainers

### Development Support
- Join discussions in issues
- Share your progress
- Ask for code reviews
- Collaborate on features

## 🎉 Recognition

Contributors will be recognized in:
- README.md contributors section
- Release notes
- Project documentation

Thank you for contributing to PROJECT_PIVOT! 🥊💪 