package com.projectpivot.app;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class BoxingTipsActivity extends AppCompatActivity {

    private TextView titleText;
    private TextView backButton;
    private LinearLayout tipsContainer;
    private CardView[] tipCards;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    
    private String[] tipTitles = {
        "basic_stance",
        "guard_position", 
        "footwork_fundamentals",
        "jab_technique",
        "cross_power",
        "hook_mechanics",
        "uppercut_form",
        "defense_basics",
        "breathing_rhythm",
        "training_tips"
    };
    
    private String[] tipDescriptions = {
        "feet_shoulder_width_apart_non_dominant_foot_forward_knees_slightly_bent",
        "hands_up_elbows_close_to_body_chin_tucked_eyes_forward",
        "stay_on_balls_of_feet_small_steps_maintain_balance_pivot_on_lead_foot",
        "extend_straight_from_guard_rotate_fist_snap_back_quickly",
        "rotate_hips_and_shoulders_step_forward_with_rear_foot_full_extension",
        "turn_lead_foot_rotate_hips_elbow_parallel_to_ground_short_arc_motion",
        "bend_knees_drive_up_through_legs_rotate_fist_palm_facing_you",
        "slip_duck_block_parry_always_keep_hands_up_move_head_off_centerline",
        "exhale_on_punches_steady_rhythm_never_hold_breath_during_combinations",
        "shadowbox_daily_use_mirror_practice_slowly_first_build_muscle_memory"
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boxing_tips);
        
        initViews();
        setupClickListeners();
        startAnimations();
    }
    
    private void initViews() {
        titleText = findViewById(R.id.titleText);
        backButton = findViewById(R.id.backButton);
        tipsContainer = findViewById(R.id.tipsContainer);
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            SoundManager.getInstance(this).hapticClick();
            finish();
            // Removed annoying transition animation
        });
    }
    
    private void startAnimations() {
        // Fade in title
        ObjectAnimator titleFade = ObjectAnimator.ofFloat(titleText, "alpha", 0f, 1f);
        titleFade.setDuration(800);
        titleFade.start();
        
        // Animate tip cards with stagger
        handler.postDelayed(() -> {
            animateTipCards();
        }, 300);
    }
    
    private void animateTipCards() {
        int childCount = tipsContainer.getChildCount();
        
        for (int i = 0; i < childCount; i++) {
            final View card = tipsContainer.getChildAt(i);
            final int index = i;
            
            handler.postDelayed(() -> {
                // Slide up and fade in
                ObjectAnimator slideUp = ObjectAnimator.ofFloat(card, "translationY", 100f, 0f);
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(card, "alpha", 0f, 1f);
                
                slideUp.setDuration(600);
                fadeIn.setDuration(600);
                slideUp.setInterpolator(new AccelerateDecelerateInterpolator());
                fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());
                
                slideUp.start();
                fadeIn.start();
                
            }, index * 100); // 100ms stagger between cards
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
} 