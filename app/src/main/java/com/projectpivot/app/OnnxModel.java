package com.projectpivot.app;

import android.content.Context;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Collections;

public class OnnxModel {
    private static final String TAG = "OnnxModel";
    
    private OrtEnvironment ortEnvironment;
    private OrtSession ortSession;
    private String modelName;
    
    public OnnxModel(Context context, String modelFileName) throws OrtException, IOException {
        this.modelName = modelFileName;
        ortEnvironment = OrtEnvironment.getEnvironment();
        
        // Load model from assets
        try (InputStream inputStream = context.getAssets().open(modelFileName)) {
            byte[] modelBytes = new byte[inputStream.available()];
            inputStream.read(modelBytes);
            
            ortSession = ortEnvironment.createSession(modelBytes);
            
        } catch (IOException e) {
            throw e;
        }
    }
    
    public float[] run(float[] input, long[] shape) throws OrtException {
        if (ortSession == null) {
            throw new IllegalStateException("Model not loaded");
        }
        
        // Create input tensor
        OnnxTensor inputTensor = OnnxTensor.createTensor(ortEnvironment, 
            FloatBuffer.wrap(input), shape);
        
        try {
            // Run inference
            OrtSession.Result result = ortSession.run(
                Collections.singletonMap("input", inputTensor));
            
            // Extract output
            OnnxTensor outputTensor = (OnnxTensor) result.get(0);
            float[][] outputArray = (float[][]) outputTensor.getValue();
            
            return outputArray[0]; // Return logits for first (and only) batch item
            
        } finally {
            inputTensor.close();
        }
    }
    
    public String predict(float[] input, long[] shape) throws OrtException {
        float[] logits = run(input, shape);
        
        // Convert logits to prediction (softmax not needed for comparison)
        boolean isCorrect = logits[1] > logits[0]; // Assuming class 1 = correct, class 0 = incorrect
        
        // Calculate confidence
        float confidence = Math.abs(logits[1] - logits[0]);
        
        return isCorrect ? 
            String.format("Correct (%.2f)", confidence) : 
            String.format("Incorrect (%.2f)", confidence);
    }
    
    public void close() {
        if (ortSession != null) {
            try {
                ortSession.close();
            } catch (OrtException e) {
                // Silently ignore close errors
            }
        }
    }
} 