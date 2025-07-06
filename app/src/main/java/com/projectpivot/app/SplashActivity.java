package com.projectpivot.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private ImageView logoImage;
    private TextView appTitle;
    private TextView appSubtitle;
    private TextView loadingText;
    private TextView statusText;
    private TextView progressText;
    private View progressBar;
    private View loadingContainer;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private int typingIndex = 0;
    private String titleText = "PROJECT_PIVOT";
    private String subtitleText = "ai_boxing_coach";
    private String loadingTextStr = "initializing_ai_models...";
    private String[] statusMessages = {
        "loading_mediapipe_models...",
        "calibrating_pose_detection...",
        "preparing_neural_networks...",
        "optimizing_performance...",
        "ready_to_analyze_form"
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        initViews();
        startAnimationSequence();
    }
    
    private void initViews() {
        logoImage = findViewById(R.id.logoImage);
        appTitle = findViewById(R.id.appTitle);
        appSubtitle = findViewById(R.id.appSubtitle);
        loadingText = findViewById(R.id.loadingText);
        statusText = findViewById(R.id.statusText);
        progressText = findViewById(R.id.progressText);
        progressBar = findViewById(R.id.progressBar);
        loadingContainer = findViewById(R.id.loadingContainer);
    }
    
    private void startAnimationSequence() {
        // Step 1: Fade in logo
        fadeInLogo();
        
        // Step 2: Type title (after 800ms)
        handler.postDelayed(() -> typeText(appTitle, titleText, () -> {
            // Step 3: Type subtitle (after title completes)
            handler.postDelayed(() -> typeText(appSubtitle, subtitleText, () -> {
                // Step 4: Show loading section (after subtitle completes)
                handler.postDelayed(this::showLoadingSection, 500);
            }), 300);
        }), 800);
    }
    
    private void fadeInLogo() {
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(logoImage, "alpha", 0f, 1f);
        fadeIn.setDuration(1000);
        fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeIn.start();
        
        // Add subtle scale animation
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logoImage, "scaleX", 0.8f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logoImage, "scaleY", 0.8f, 1f);
        scaleX.setDuration(1000);
        scaleY.setDuration(1000);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.start();
        scaleY.start();
    }
    
    private void typeText(TextView textView, String text, Runnable onComplete) {
        textView.setAlpha(1f);
        
        ValueAnimator typeAnimator = ValueAnimator.ofInt(0, text.length());
        typeAnimator.setDuration(text.length() * 80); // 80ms per character
        typeAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        typeAnimator.addUpdateListener(animation -> {
            int currentLength = (int) animation.getAnimatedValue();
            String currentText = text.substring(0, currentLength);
            
            // Add cursor effect
            if (currentLength < text.length()) {
                currentText += "_";
            }
            
            textView.setText(currentText);
        });
        
        typeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Remove cursor and show final text
                textView.setText(text);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        
        typeAnimator.start();
    }
    
    private void showLoadingSection() {
        // Fade in loading container
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(loadingContainer, "alpha", 0f, 1f);
        fadeIn.setDuration(600);
        fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeIn.start();
        
        // Start typing loading text
        handler.postDelayed(() -> {
            typeText(loadingText, loadingTextStr, () -> {
                // Start progress animation
                handler.postDelayed(this::startProgressAnimation, 500);
            });
        }, 300);
    }
    
    private void startProgressAnimation() {
        // Animate progress bar
        ValueAnimator progressAnimator = ValueAnimator.ofFloat(0f, 1f);
        progressAnimator.setDuration(3000); // 3 seconds
        progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        progressAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            
            // Update progress bar width
            progressBar.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                (int) (280 * getResources().getDisplayMetrics().density * progress),
                (int) (4 * getResources().getDisplayMetrics().density)
            ));
            
            // Update progress text
            int percentage = (int) (progress * 100);
            progressText.setText(percentage + "%");
        });
        
        progressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Show completion and navigate
                handler.postDelayed(() -> {
                    navigateToNextScreen();
                }, 500);
            }
        });
        
        progressAnimator.start();
        
        // Show status messages during loading
        showStatusMessages();
    }
    
    private void showStatusMessages() {
        for (int i = 0; i < statusMessages.length; i++) {
            final int index = i;
            handler.postDelayed(() -> {
                // Fade in status text
                statusText.setAlpha(0f);
                statusText.setText(statusMessages[index]);
                
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(statusText, "alpha", 0f, 1f);
                fadeIn.setDuration(400);
                fadeIn.start();
                
                // Fade out after showing
                handler.postDelayed(() -> {
                    ObjectAnimator fadeOut = ObjectAnimator.ofFloat(statusText, "alpha", 1f, 0f);
                    fadeOut.setDuration(300);
                    fadeOut.start();
                }, 500);
                
            }, i * 600); // Show each message for 600ms
        }
    }
    
    private void navigateToNextScreen() {
        // Add exit animation
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(findViewById(R.id.logoContainer), "alpha", 1f, 0f);
        fadeOut.setDuration(500);
        fadeOut.start();
        
        ObjectAnimator fadeOutLoading = ObjectAnimator.ofFloat(loadingContainer, "alpha", 1f, 0f);
        fadeOutLoading.setDuration(500);
        fadeOutLoading.start();
        
        handler.postDelayed(() -> {
            // Check if user is authenticated
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();
            
            Intent intent;
            if (currentUser != null) {
                // User is signed in, go to dashboard
                intent = new Intent(SplashActivity.this, DashboardActivity.class);
            } else {
                // No user is signed in, go to sign in
                intent = new Intent(SplashActivity.this, SignInActivity.class);
            }
            
            startActivity(intent);
            // Removed annoying transition animation
            finish();
        }, 500);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
} 