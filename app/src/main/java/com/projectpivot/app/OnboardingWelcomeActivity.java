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
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class OnboardingWelcomeActivity extends AppCompatActivity {

    private ImageView logoImage;
    private TextView welcomeTitle;
    private TextView welcomeSubtitle;
    private LinearLayout featuresContainer;
    private LinearLayout buttonSection;
    private CardView getStartedButton;
    private TextView skipButton;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private String titleText = "WELCOME_TO_PROJECT_PIVOT";
    private String subtitleText = "your_ai_boxing_coach";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_welcome);
        
        initViews();
        setupClickListeners();
        startAnimationSequence();
    }
    
    private void initViews() {
        logoImage = findViewById(R.id.logoImage);
        welcomeTitle = findViewById(R.id.welcomeTitle);
        welcomeSubtitle = findViewById(R.id.welcomeSubtitle);
        featuresContainer = findViewById(R.id.featuresContainer);
        buttonSection = findViewById(R.id.buttonSection);
        getStartedButton = findViewById(R.id.getStartedButton);
        skipButton = findViewById(R.id.skipButton);
    }
    
    private void setupClickListeners() {
        getStartedButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticSelect();
            animateButtonPress(getStartedButton, () -> {
                // Navigate to next onboarding step or main app
                Intent intent = new Intent(OnboardingWelcomeActivity.this, DashboardActivity.class);
                startActivity(intent);
                // Removed annoying transition animation
                finish();
            });
        });
        
        skipButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            // Navigate directly to main app
            Intent intent = new Intent(OnboardingWelcomeActivity.this, DashboardActivity.class);
            startActivity(intent);
            // Removed annoying transition animation
            finish();
        });
    }
    
    private void startAnimationSequence() {
        // Step 1: Fade in logo
        handler.postDelayed(this::fadeInLogo, 300);
        
        // Step 2: Type welcome title
        handler.postDelayed(() -> {
            typeText(welcomeTitle, titleText, () -> {
                // Step 3: Type subtitle
                handler.postDelayed(() -> {
                    typeText(welcomeSubtitle, subtitleText, () -> {
                        // Step 4: Show features with stagger
                        handler.postDelayed(this::showFeatures, 500);
                    });
                }, 200);
            });
        }, 800);
    }
    
    private void fadeInLogo() {
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(logoImage, "alpha", 0f, 1f);
        fadeIn.setDuration(800);
        fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeIn.start();
        
        // Add gentle bounce
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logoImage, "scaleX", 0.3f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logoImage, "scaleY", 0.3f, 1f);
        scaleX.setDuration(800);
        scaleY.setDuration(800);
        scaleX.setInterpolator(new OvershootInterpolator(0.5f));
        scaleY.setInterpolator(new OvershootInterpolator(0.5f));
        scaleX.start();
        scaleY.start();
    }
    
    private void typeText(TextView textView, String text, Runnable onComplete) {
        // First fade in the subtitle container
        if (textView == welcomeSubtitle) {
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f);
            fadeIn.setDuration(300);
            fadeIn.start();
        }
        
        ValueAnimator typeAnimator = ValueAnimator.ofInt(0, text.length());
        typeAnimator.setDuration(text.length() * 60); // Slightly faster for welcome screen
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
                textView.setText(text);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        
        typeAnimator.start();
    }
    
    private void showFeatures() {
        // Fade in features container
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(featuresContainer, "alpha", 0f, 1f);
        fadeIn.setDuration(600);
        fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeIn.start();
        
        // Animate each feature card with stagger
        for (int i = 0; i < featuresContainer.getChildCount(); i++) {
            final View card = featuresContainer.getChildAt(i);
            final int index = i;
            
            handler.postDelayed(() -> {
                // Slide up and fade in
                card.setTranslationY(0); // Reset from initial 50dp
                ObjectAnimator slideUp = ObjectAnimator.ofFloat(card, "translationY", 50f, 0f);
                ObjectAnimator fadeInCard = ObjectAnimator.ofFloat(card, "alpha", 0f, 1f);
                
                slideUp.setDuration(500);
                fadeInCard.setDuration(500);
                slideUp.setInterpolator(new AccelerateDecelerateInterpolator());
                fadeInCard.setInterpolator(new AccelerateDecelerateInterpolator());
                
                slideUp.start();
                fadeInCard.start();
                
                // Add subtle bounce to the last card
                if (index == featuresContainer.getChildCount() - 1) {
                    ObjectAnimator bounce = ObjectAnimator.ofFloat(card, "scaleX", 1f, 1.05f, 1f);
                    ObjectAnimator bounceY = ObjectAnimator.ofFloat(card, "scaleY", 1f, 1.05f, 1f);
                    bounce.setDuration(300);
                    bounceY.setDuration(300);
                    bounce.setStartDelay(500);
                    bounceY.setStartDelay(500);
                    bounce.start();
                    bounceY.start();
                    
                    // Show buttons after last card animation
                    handler.postDelayed(this::showButtons, 800);
                }
                
            }, index * 150); // 150ms stagger between cards
        }
    }
    
    private void showButtons() {
        // Slide up and fade in button section
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(buttonSection, "translationY", 100f, 0f);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(buttonSection, "alpha", 0f, 1f);
        
        slideUp.setDuration(600);
        fadeIn.setDuration(600);
        slideUp.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());
        
        slideUp.start();
        fadeIn.start();
        
        // Add gentle pulse to get started button
        handler.postDelayed(() -> {
            ObjectAnimator pulse = ObjectAnimator.ofFloat(getStartedButton, "scaleX", 1f, 1.03f, 1f);
            ObjectAnimator pulseY = ObjectAnimator.ofFloat(getStartedButton, "scaleY", 1f, 1.03f, 1f);
            pulse.setDuration(1000);
            pulseY.setDuration(1000);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulseY.setRepeatCount(ValueAnimator.INFINITE);
            pulse.setRepeatMode(ValueAnimator.REVERSE);
            pulseY.setRepeatMode(ValueAnimator.REVERSE);
            pulse.start();
            pulseY.start();
        }, 1000);
    }
    
    private void animateButtonPress(View button, Runnable onComplete) {
        // Scale down
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.95f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.95f);
        scaleDownX.setDuration(100);
        scaleDownY.setDuration(100);
        
        scaleDownX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Scale back up
                ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(button, "scaleX", 0.95f, 1f);
                ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(button, "scaleY", 0.95f, 1f);
                scaleUpX.setDuration(100);
                scaleUpY.setDuration(100);
                scaleUpX.setInterpolator(new OvershootInterpolator(0.3f));
                scaleUpY.setInterpolator(new OvershootInterpolator(0.3f));
                
                scaleUpX.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }
                });
                
                scaleUpX.start();
                scaleUpY.start();
            }
        });
        
        scaleDownX.start();
        scaleDownY.start();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
} 