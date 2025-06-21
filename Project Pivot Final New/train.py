#!/usr/bin/env python3
"""
Training script for MediaPipe pose classification models.
Trains two separate MobileNetV2-based models for stance and execution classification.
"""

import json
import os
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
import torchvision.models as models
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, classification_report
import time
from tqdm import tqdm

# Check for GPU availability
DEVICE = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
print(f"Using device: {DEVICE}")

class PoseDataset(Dataset):
    """Custom dataset for pose keypoints classification."""
    
    def __init__(self, data_paths, labels, transform=None):
        self.data_paths = data_paths
        self.labels = labels
        self.transform = transform
        
    def __len__(self):
        return len(self.data_paths)
    
    def __getitem__(self, idx):
        # Load JSON file
        with open(self.data_paths[idx], 'r') as f:
            data = json.load(f)
        
        # Extract keypoints and normalize
        keypoints = np.array(data['keypoints'], dtype=np.float32)
        
        # Apply transform if provided
        if self.transform:
            keypoints = self.transform(keypoints)
        
        # Convert to tensor
        keypoints = torch.FloatTensor(keypoints)
        label = torch.LongTensor([self.labels[idx]])[0]
        
        return keypoints, label

class KeypointNormalizer:
    """Normalize keypoints to [-1, 1] range."""
    
    def __call__(self, keypoints):
        # Normalize keypoints to [-1, 1] range
        keypoints = (keypoints - keypoints.mean()) / (keypoints.std() + 1e-8)
        return keypoints

class PoseClassifier(nn.Module):
    """MobileNetV2-based classifier adapted for pose keypoints."""
    
    def __init__(self, input_size=28, num_classes=2):
        super(PoseClassifier, self).__init__()
        
        # Create a backbone similar to MobileNetV2 but for 1D data
        self.features = nn.Sequential(
            # First layer - expand input to larger feature space
            nn.Linear(input_size, 32),
            nn.BatchNorm1d(32),
            nn.ReLU6(inplace=True),
            
            # Depthwise separable convolution equivalent for 1D
            nn.Linear(32, 64),
            nn.BatchNorm1d(64),
            nn.ReLU6(inplace=True),
            nn.Dropout(0.2),
            
            nn.Linear(64, 128),
            nn.BatchNorm1d(128),
            nn.ReLU6(inplace=True),
            nn.Dropout(0.3),
            
            nn.Linear(128, 256),
            nn.BatchNorm1d(256),
            nn.ReLU6(inplace=True),
            nn.Dropout(0.3),
            
            nn.Linear(256, 128),
            nn.BatchNorm1d(128),
            nn.ReLU6(inplace=True),
            nn.Dropout(0.2),
            
            nn.Linear(128, 64),
            nn.BatchNorm1d(64),
            nn.ReLU6(inplace=True),
        )
        
        # Classifier
        self.classifier = nn.Sequential(
            nn.Dropout(0.2),
            nn.Linear(64, num_classes)
        )
        
    def forward(self, x):
        x = self.features(x)
        x = self.classifier(x)
        return x

def load_dataset(category):
    """Load dataset for a specific category (stance or execution)."""
    data_paths = []
    labels = []
    
    # Load correct samples
    correct_dir = f"MediaPipe_Dataset/{category}/correct {category}"
    if os.path.exists(correct_dir):
        for filename in os.listdir(correct_dir):
            if filename.endswith('.json'):
                data_paths.append(os.path.join(correct_dir, filename))
                labels.append(1)  # Correct = 1
    
    # Load incorrect samples  
    incorrect_dir = f"MediaPipe_Dataset/{category}/incorrect {category}"
    if os.path.exists(incorrect_dir):
        for filename in os.listdir(incorrect_dir):
            if filename.endswith('.json'):
                data_paths.append(os.path.join(incorrect_dir, filename))
                labels.append(0)  # Incorrect = 0
    
    return data_paths, labels

def train_model(model, train_loader, val_loader, num_epochs=10, learning_rate=1e-3):
    """Train the model."""
    criterion = nn.CrossEntropyLoss()
    optimizer = optim.Adam(model.parameters(), lr=learning_rate)
    scheduler = optim.lr_scheduler.StepLR(optimizer, step_size=5, gamma=0.1)
    
    best_val_acc = 0.0
    best_model_state = None
    train_losses = []
    train_accs = []
    val_accs = []
    
    model.to(DEVICE)
    
    for epoch in range(num_epochs):
        # Training phase
        model.train()
        running_loss = 0.0
        correct_train = 0
        total_train = 0
        
        pbar = tqdm(train_loader, desc=f'Epoch {epoch+1}/{num_epochs}')
        for inputs, labels in pbar:
            inputs, labels = inputs.to(DEVICE), labels.to(DEVICE)
            
            optimizer.zero_grad()
            outputs = model(inputs)
            loss = criterion(outputs, labels)
            loss.backward()
            optimizer.step()
            
            running_loss += loss.item()
            _, predicted = torch.max(outputs.data, 1)
            total_train += labels.size(0)
            correct_train += (predicted == labels).sum().item()
            
            pbar.set_postfix({
                'Loss': f'{loss.item():.4f}',
                'Acc': f'{100 * correct_train / total_train:.2f}%'
            })
        
        # Validation phase
        model.eval()
        correct_val = 0
        total_val = 0
        val_loss = 0.0
        
        with torch.no_grad():
            for inputs, labels in val_loader:
                inputs, labels = inputs.to(DEVICE), labels.to(DEVICE)
                outputs = model(inputs)
                loss = criterion(outputs, labels)
                val_loss += loss.item()
                
                _, predicted = torch.max(outputs, 1)
                total_val += labels.size(0)
                correct_val += (predicted == labels).sum().item()
        
        epoch_train_acc = 100 * correct_train / total_train
        epoch_val_acc = 100 * correct_val / total_val
        epoch_train_loss = running_loss / len(train_loader)
        
        train_losses.append(epoch_train_loss)
        train_accs.append(epoch_train_acc)
        val_accs.append(epoch_val_acc)
        
        print(f'Epoch [{epoch+1}/{num_epochs}]:')
        print(f'  Train Loss: {epoch_train_loss:.4f}, Train Acc: {epoch_train_acc:.2f}%')
        print(f'  Val Acc: {epoch_val_acc:.2f}%')
        print('-' * 50)
        
        # Save best model
        if epoch_val_acc > best_val_acc:
            best_val_acc = epoch_val_acc
            best_model_state = model.state_dict().copy()
        
        scheduler.step()
    
    # Load best model
    if best_model_state is not None:
        model.load_state_dict(best_model_state)
    
    return model, best_val_acc, train_losses, train_accs, val_accs

def export_to_onnx(model, model_path, onnx_path, input_size=28):
    """Export PyTorch model to ONNX format."""
    # Set model to evaluation mode
    model.eval()
    
    # Create dummy input
    dummy_input = torch.randn(1, input_size).to(DEVICE)
    
    # Export to ONNX
    torch.onnx.export(
        model,                      # model being run
        dummy_input,                # model input (or a tuple for multiple inputs)
        onnx_path,                  # where to save the model
        export_params=True,         # store the trained parameter weights inside the model file
        opset_version=13,           # the ONNX version to export the model to
        do_constant_folding=True,   # whether to execute constant folding for optimization
        input_names=['input'],      # the model's input names
        output_names=['output'],    # the model's output names
        dynamic_axes={
            'input': {0: 'batch_size'},      # variable length axes
            'output': {0: 'batch_size'}
        }
    )
    print(f"Model exported to ONNX: {onnx_path}")

def main():
    print("=== MediaPipe Pose Classification Training ===")
    print()
    
    # Dataset statistics
    print("Dataset Statistics:")
    for category in ['stance', 'execution']:
        data_paths, labels = load_dataset(category)
        correct_count = sum(labels)
        incorrect_count = len(labels) - correct_count
        print(f"{category.capitalize()}:")
        print(f"  - Correct samples: {correct_count}")
        print(f"  - Incorrect samples: {incorrect_count}")
        print(f"  - Total samples: {len(labels)}")
        print()
    
    # Prepare transforms
    transform = KeypointNormalizer()
    
    # Train models for both categories
    for category in ['stance', 'execution']:
        print(f"\n{'='*60}")
        print(f"Training {category.upper()} Classification Model")
        print('='*60)
        
        # Load dataset
        data_paths, labels = load_dataset(category)
        
        if len(data_paths) == 0:
            print(f"No data found for {category}. Skipping...")
            continue
        
        # Split dataset
        train_paths, val_paths, train_labels, val_labels = train_test_split(
            data_paths, labels, test_size=0.2, random_state=42, stratify=labels
        )
        
        # Create datasets
        train_dataset = PoseDataset(train_paths, train_labels, transform=transform)
        val_dataset = PoseDataset(val_paths, val_labels, transform=transform)
        
        # Create data loaders
        train_loader = DataLoader(train_dataset, batch_size=32, shuffle=True)
        val_loader = DataLoader(val_dataset, batch_size=32, shuffle=False)
        
        # Create model
        model = PoseClassifier(input_size=28, num_classes=2)
        
        # Train model
        model, best_acc, train_losses, train_accs, val_accs = train_model(
            model, train_loader, val_loader, num_epochs=10, learning_rate=1e-3
        )
        
        print(f"\nBest validation accuracy for {category}: {best_acc:.2f}%")
        
        # Save PyTorch model
        torch_path = f"models/{category}.pt"
        torch.save(model.state_dict(), torch_path)
        print(f"PyTorch model saved: {torch_path}")
        
        # Export to ONNX
        onnx_path = f"models/{category}.onnx"
        export_to_onnx(model, torch_path, onnx_path, input_size=28)
        
        # Generate classification report
        print(f"\nEvaluating {category} model on validation set...")
        model.eval()
        val_predictions = []
        val_true_labels = []
        
        with torch.no_grad():
            for inputs, labels in val_loader:
                inputs, labels = inputs.to(DEVICE), labels.to(DEVICE)
                outputs = model(inputs)
                _, predicted = torch.max(outputs, 1)
                val_predictions.extend(predicted.cpu().numpy())
                val_true_labels.extend(labels.cpu().numpy())
        
        print(f"Classification Report for {category}:")
        print(classification_report(val_true_labels, val_predictions, 
                                  target_names=['Incorrect', 'Correct']))
        print()
    
    print("Training completed successfully!")
    print("\nGenerated files:")
    print("- models/stance.pt")
    print("- models/execution.pt") 
    print("- models/stance.onnx")
    print("- models/execution.onnx")

if __name__ == "__main__":
    main() 