#!/usr/bin/env python3
"""
Quantization script for MediaPipe pose classification models.
Converts ONNX models to quantized INT8 versions using ONNX Runtime.
"""

import os
import json
import numpy as np
import onnxruntime as ort
from onnxruntime.quantization import quantize_dynamic, QuantType
import torch
from sklearn.metrics import accuracy_score, classification_report
from train import PoseDataset, KeypointNormalizer, load_dataset
from torch.utils.data import DataLoader
from sklearn.model_selection import train_test_split
import time

def get_file_size_mb(filepath):
    """Get file size in megabytes."""
    if os.path.exists(filepath):
        size_bytes = os.path.getsize(filepath)
        size_mb = size_bytes / (1024 * 1024)
        return size_mb
    return 0

def quantize_model(model_path, quantized_path):
    """Quantize ONNX model using dynamic quantization."""
    if not os.path.exists(model_path):
        print(f"Model not found: {model_path}")
        return False
    
    try:
        print(f"Quantizing {model_path}...")
        
        # Perform dynamic quantization
        quantize_dynamic(
            model_input=model_path,
            model_output=quantized_path,
            weight_type=QuantType.QInt8
        )
        
        print(f"Quantized model saved: {quantized_path}")
        return True
        
    except Exception as e:
        print(f"Error quantizing {model_path}: {str(e)}")
        return False

def evaluate_onnx_model(model_path, category, num_samples=100):
    """Evaluate ONNX model on validation data."""
    print(f"Evaluating {model_path} on {category} data...")
    
    # Load dataset
    data_paths, labels = load_dataset(category)
    if len(data_paths) == 0:
        print(f"No data found for {category}")
        return None, None, None
    
    # Split dataset and take a subset for evaluation
    _, val_paths, _, val_labels = train_test_split(
        data_paths, labels, test_size=0.2, random_state=42, stratify=labels
    )
    
    # Limit evaluation samples for speed
    if len(val_paths) > num_samples:
        val_paths = val_paths[:num_samples]
        val_labels = val_labels[:num_samples]
    
    # Load ONNX model
    try:
        session = ort.InferenceSession(model_path, providers=['CPUExecutionProvider'])
        input_name = session.get_inputs()[0].name
    except Exception as e:
        print(f"Error loading ONNX model {model_path}: {str(e)}")
        return None, None, None
    
    # Prepare data
    normalizer = KeypointNormalizer()
    predictions = []
    true_labels = []
    inference_times = []
    
    for i, (path, label) in enumerate(zip(val_paths, val_labels)):
        try:
            # Load and preprocess data
            with open(path, 'r') as f:
                data = json.load(f)
            
            keypoints = np.array(data['keypoints'], dtype=np.float32)
            keypoints = normalizer(keypoints)
            keypoints = keypoints.reshape(1, -1)  # Add batch dimension
            
            # Run inference
            start_time = time.time()
            outputs = session.run(None, {input_name: keypoints})
            inference_time = time.time() - start_time
            inference_times.append(inference_time)
            
            # Get prediction
            predicted_class = np.argmax(outputs[0], axis=1)[0]
            predictions.append(predicted_class)
            true_labels.append(label)
            
        except Exception as e:
            print(f"Error processing sample {i}: {str(e)}")
            continue
    
    if len(predictions) == 0:
        print("No successful predictions made")
        return None, None, None
    
    # Calculate metrics
    accuracy = accuracy_score(true_labels, predictions)
    avg_inference_time = np.mean(inference_times) * 1000  # Convert to milliseconds
    
    return accuracy, avg_inference_time, len(predictions)

def compare_models(category):
    """Compare original and quantized models."""
    original_path = f"models/{category}.onnx"
    quantized_path = f"models/{category}_int8.onnx"
    
    print(f"\n{'='*60}")
    print(f"COMPARISON REPORT: {category.upper()} MODELS")
    print('='*60)
    
    # File sizes
    original_size = get_file_size_mb(original_path)
    quantized_size = get_file_size_mb(quantized_path)
    
    print("File Size Comparison:")
    print(f"  Original ({original_path}): {original_size:.2f} MB")
    print(f"  Quantized ({quantized_path}): {quantized_size:.2f} MB")
    
    if original_size > 0:
        size_reduction = ((original_size - quantized_size) / original_size) * 100
        print(f"  Size Reduction: {size_reduction:.1f}%")
    print()
    
    # Performance comparison
    print("Performance Comparison:")
    
    # Evaluate original model
    original_acc, original_time, original_samples = evaluate_onnx_model(original_path, category)
    if original_acc is not None:
        print(f"  Original Model:")
        print(f"    Accuracy: {original_acc:.4f} ({original_acc*100:.2f}%)")
        print(f"    Avg Inference Time: {original_time:.2f} ms")
        print(f"    Samples Evaluated: {original_samples}")
    
    # Evaluate quantized model
    quantized_acc, quantized_time, quantized_samples = evaluate_onnx_model(quantized_path, category)
    if quantized_acc is not None:
        print(f"  Quantized Model:")
        print(f"    Accuracy: {quantized_acc:.4f} ({quantized_acc*100:.2f}%)")
        print(f"    Avg Inference Time: {quantized_time:.2f} ms")
        print(f"    Samples Evaluated: {quantized_samples}")
    
    # Calculate differences
    if original_acc is not None and quantized_acc is not None:
        acc_diff = (quantized_acc - original_acc) * 100
        time_speedup = ((original_time - quantized_time) / original_time) * 100
        
        print(f"  Performance Changes:")
        print(f"    Accuracy Difference: {acc_diff:+.2f} percentage points")
        print(f"    Speed Improvement: {time_speedup:+.1f}%")
    
    print()

def main():
    print("=== MediaPipe Pose Model Quantization ===")
    print()
    
    # Check if ONNX models exist
    categories = ['stance', 'execution']
    models_to_quantize = []
    
    for category in categories:
        model_path = f"models/{category}.onnx"
        if os.path.exists(model_path):
            models_to_quantize.append(category)
            print(f"✓ Found {model_path}")
        else:
            print(f"✗ Missing {model_path}")
    
    if not models_to_quantize:
        print("\nNo ONNX models found. Please run train.py first.")
        return
    
    print(f"\nQuantizing {len(models_to_quantize)} model(s)...")
    print()
    
    # Quantize each model
    successful_quantizations = []
    
    for category in models_to_quantize:
        model_path = f"models/{category}.onnx"
        quantized_path = f"models/{category}_int8.onnx"
        
        success = quantize_model(model_path, quantized_path)
        if success:
            successful_quantizations.append(category)
        print()
    
    # Compare models
    print("Generating comparison reports...")
    print()
    
    for category in successful_quantizations:
        compare_models(category)
    
    # Summary
    print("="*60)
    print("QUANTIZATION SUMMARY")
    print("="*60)
    
    print("Generated Files:")
    for category in successful_quantizations:
        quantized_path = f"models/{category}_int8.onnx"
        size = get_file_size_mb(quantized_path)
        print(f"  - {quantized_path} ({size:.2f} MB)")
    
    print(f"\nSuccessfully quantized {len(successful_quantizations)} out of {len(models_to_quantize)} models.")
    
    if len(successful_quantizations) > 0:
        print("\nQuantization Benefits:")
        print("  - Reduced model size (typically 50-75% smaller)")
        print("  - Faster inference (INT8 operations)")
        print("  - Lower memory usage")
        print("  - Better deployment performance on CPU")
        print("\nNote: Quantized models may have slight accuracy reduction")
        print("but offer significant performance improvements for deployment.")

if __name__ == "__main__":
    main() 