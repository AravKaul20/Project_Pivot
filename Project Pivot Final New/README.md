# MediaPipe Pose Classification Project

A machine learning pipeline for classifying pose correctness in stance and execution movements using MediaPipe pose estimation data.

## Project Overview

This project provides an end-to-end solution for training and deploying pose classification models that can distinguish between correct and incorrect poses in two categories:
- **Stance Classification**: Determines if a stance pose is correct or incorrect
- **Execution Classification**: Determines if an execution movement is correct or incorrect

## Dataset Structure

The project uses MediaPipe pose keypoint data organized as follows:

```
MediaPipe_Dataset/
├── stance/
│   ├── correct stance/     # 196 correct stance samples
│   └── incorrect stance/   # 159 incorrect stance samples
└── execution/
    ├── correct execution/  # 118 correct execution samples
    └── incorrect execution/ # 125 incorrect execution samples
```

**Dataset Statistics:**
- **Stance**: 355 total samples (196 correct, 159 incorrect)
- **Execution**: 243 total samples (118 correct, 125 incorrect)

Each sample is a JSON file containing:
- `keypoints`: Array of 28 normalized pose keypoint coordinates (14 points × 2 coordinates)
- `label`: 1 for correct, 0 for incorrect (inferred from folder structure)
- `category`: "stance" or "execution"
- `timestamp`: Sample timestamp

## Model Architecture

The models use a MobileNetV2-inspired architecture adapted for 1D pose keypoint data:
- **Input**: 28 pose keypoint coordinates (14 points × 2 coordinates)
- **Architecture**: Deep neural network with batch normalization and dropout
- **Output**: Binary classification (correct/incorrect)
- **Training**: 10 epochs, Adam optimizer, learning rate 1e-3, batch size 32

## Installation

### Prerequisites
- Python 3.7+
- PyTorch
- ONNX Runtime
- scikit-learn
- NumPy
- tqdm

### Install Dependencies
```bash
pip install torch torchvision torchaudio
pip install onnxruntime onnxruntime-tools
pip install scikit-learn numpy tqdm
```

## Usage

### 1. Training Models

Run the training script to train both stance and execution classification models:

```bash
python train.py
```

**What it does:**
- Loads and preprocesses the MediaPipe pose dataset
- Splits data into training (80%) and validation (20%) sets
- Trains two separate models for stance and execution classification
- Saves PyTorch models (`models/stance.pt`, `models/execution.pt`)
- Exports ONNX models (`models/stance.onnx`, `models/execution.onnx`)
- Reports training progress, validation accuracy, and classification metrics

**Expected Output:**
```
=== MediaPipe Pose Classification Training ===

Dataset Statistics:
Stance:
  - Correct samples: 196
  - Incorrect samples: 159
  - Total samples: 355

Execution:
  - Correct samples: 118
  - Incorrect samples: 125
  - Total samples: 243

Training STANCE Classification Model
============================================================
Epoch [1/10]:
  Train Loss: 0.6234, Train Acc: 68.45%
  Val Acc: 72.11%
...
```

### 2. Quantizing Models

After training, quantize the ONNX models for faster inference:

```bash
python quantize.py
```

**What it does:**
- Converts ONNX models to INT8 quantized versions
- Compares file sizes and performance metrics
- Generates quantized models (`models/stance_int8.onnx`, `models/execution_int8.onnx`)
- Reports accuracy changes and speed improvements

**Expected Output:**
```
=== MediaPipe Pose Model Quantization ===

✓ Found models/stance.onnx
✓ Found models/execution.onnx

Quantizing models/stance.onnx...
Quantized model saved: models/stance_int8.onnx

COMPARISON REPORT: STANCE MODELS
============================================================
File Size Comparison:
  Original: 0.45 MB
  Quantized: 0.12 MB
  Size Reduction: 73.3%
...
```

## Generated Files

After running both scripts, you'll have:

```
models/
├── stance.pt           # PyTorch stance model
├── execution.pt        # PyTorch execution model
├── stance.onnx         # ONNX stance model
├── execution.onnx      # ONNX execution model
├── stance_int8.onnx    # Quantized stance model
└── execution_int8.onnx # Quantized execution model
```

## Model Performance

The models typically achieve:
- **Training Accuracy**: 85-95%
- **Validation Accuracy**: 70-85%
- **Inference Time**: 1-5ms per sample (quantized models)
- **Model Size**: 0.1-0.5 MB (quantized models are ~75% smaller)

## Using the Models

### PyTorch Models
```python
import torch
from train import PoseClassifier, KeypointNormalizer

# Load model
model = PoseClassifier(input_size=28, num_classes=2)
model.load_state_dict(torch.load('models/stance.pt'))
model.eval()

# Prepare input
keypoints = [...]  # Your 28 keypoint coordinates (14 points × 2 coords)
normalizer = KeypointNormalizer()
input_tensor = torch.FloatTensor(normalizer(keypoints)).unsqueeze(0)

# Make prediction
with torch.no_grad():
    output = model(input_tensor)
    prediction = torch.argmax(output, dim=1).item()
    confidence = torch.softmax(output, dim=1).max().item()

print(f"Prediction: {'Correct' if prediction == 1 else 'Incorrect'}")
print(f"Confidence: {confidence:.3f}")
```

### ONNX Models
```python
import onnxruntime as ort
import numpy as np

# Load ONNX model
session = ort.InferenceSession('models/stance_int8.onnx')
input_name = session.get_inputs()[0].name

# Prepare input
keypoints = np.array([...], dtype=np.float32)  # Your 28 keypoint coordinates
keypoints = (keypoints - keypoints.mean()) / (keypoints.std() + 1e-8)
input_data = keypoints.reshape(1, -1)

# Make prediction
outputs = session.run(None, {input_name: input_data})
prediction = np.argmax(outputs[0], axis=1)[0]
confidence = np.max(outputs[0])

print(f"Prediction: {'Correct' if prediction == 1 else 'Incorrect'}")
print(f"Confidence: {confidence:.3f}")
```

## Model Deployment

The quantized ONNX models are optimized for deployment:
- **Small file size**: Typically 0.1-0.2 MB
- **Fast inference**: Optimized for CPU execution
- **Cross-platform**: Can run on various platforms with ONNX Runtime
- **No PyTorch dependency**: Only requires ONNX Runtime for inference

## Troubleshooting

### Common Issues

1. **ModuleNotFoundError**: Install missing dependencies
   ```bash
   pip install -r requirements.txt
   ```

2. **CUDA out of memory**: The script automatically uses CPU if GPU is unavailable

3. **File not found errors**: Ensure the MediaPipe_Dataset folder structure is correct

4. **Low accuracy**: Check data quality and consider:
   - Increasing training epochs
   - Adjusting learning rate
   - Adding data augmentation

### Performance Optimization

- **For faster training**: Use GPU if available
- **For faster inference**: Use quantized models
- **For better accuracy**: Increase training epochs or collect more data
- **For smaller models**: Use quantization or model pruning

## Technical Details

### Data Preprocessing
- Keypoints are normalized using mean and standard deviation
- Data is split 80/20 for training/validation
- Stratified sampling ensures balanced class distribution

### Model Architecture
- Based on MobileNetV2 design principles
- Adapted for 1D pose keypoint data
- Uses batch normalization and dropout for regularization
- ReLU6 activation functions for mobile deployment

### Quantization
- Dynamic quantization using ONNX Runtime
- Converts FP32 weights to INT8
- Maintains activation precision while reducing model size
- Optimized for CPU inference

## Contributing

1. Ensure your pose data follows the JSON format
2. Add new categories by creating appropriate folder structures
3. Modify the model architecture in `train.py` for different use cases
4. Update the README when adding new features

## License

This project is provided as-is for educational and research purposes. 