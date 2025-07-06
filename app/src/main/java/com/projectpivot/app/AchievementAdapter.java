package com.projectpivot.app;

import android.animation.ValueAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder> {
    
    private List<AchievementManager.Achievement> achievements;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    
    public AchievementAdapter(List<AchievementManager.Achievement> achievements) {
        this.achievements = achievements;
    }
    
    public void updateAchievements(List<AchievementManager.Achievement> newAchievements) {
        this.achievements = newAchievements;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public AchievementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_achievement, parent, false);
        return new AchievementViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull AchievementViewHolder holder, int position) {
        AchievementManager.Achievement achievement = achievements.get(position);
        holder.bind(achievement);
    }
    
    @Override
    public int getItemCount() {
        return achievements.size();
    }
    
    class AchievementViewHolder extends RecyclerView.ViewHolder {
        
        private CardView achievementCard;
        private TextView iconText;
        private TextView titleText;
        private TextView descriptionText;
        private TextView categoryText;
        private TextView progressText;
        private ProgressBar progressBar;
        private TextView rewardText;
        private TextView unlockedDateText;
        private View unlockedIndicator;
        private View lockedOverlay;
        
        public AchievementViewHolder(@NonNull View itemView) {
            super(itemView);
            
            achievementCard = itemView.findViewById(R.id.achievementCard);
            iconText = itemView.findViewById(R.id.iconText);
            titleText = itemView.findViewById(R.id.titleText);
            descriptionText = itemView.findViewById(R.id.descriptionText);
            categoryText = itemView.findViewById(R.id.categoryText);
            progressText = itemView.findViewById(R.id.progressText);
            progressBar = itemView.findViewById(R.id.progressBar);
            rewardText = itemView.findViewById(R.id.rewardText);
            unlockedDateText = itemView.findViewById(R.id.unlockedDateText);
            unlockedIndicator = itemView.findViewById(R.id.unlockedIndicator);
            lockedOverlay = itemView.findViewById(R.id.lockedOverlay);
        }
        
        public void bind(AchievementManager.Achievement achievement) {
            // Set basic info
            iconText.setText(achievement.icon);
            titleText.setText(achievement.title);
            descriptionText.setText(achievement.description);
            categoryText.setText(achievement.category.toUpperCase());
            rewardText.setText(achievement.reward);
            
            // Set progress
            int progressPercentage = achievement.getProgressPercentage();
            progressText.setText(String.format("%d / %d", achievement.progress, achievement.targetValue));
            progressBar.setProgress(progressPercentage);
            
            // Handle unlock status
            if (achievement.isUnlocked) {
                // Achievement is unlocked
                achievementCard.setCardBackgroundColor(itemView.getContext().getResources()
                    .getColor(R.color.background_secondary));
                
                unlockedIndicator.setVisibility(View.VISIBLE);
                lockedOverlay.setVisibility(View.GONE);
                
                // Show unlock date
                if (achievement.unlockedDate > 0) {
                    String dateStr = dateFormat.format(new Date(achievement.unlockedDate));
                    unlockedDateText.setText("Unlocked " + dateStr);
                    unlockedDateText.setVisibility(View.VISIBLE);
                } else {
                    unlockedDateText.setVisibility(View.GONE);
                }
                
                // Full opacity for unlocked achievements
                setViewAlpha(itemView, 1.0f);
                
            } else {
                // Achievement is locked
                achievementCard.setCardBackgroundColor(itemView.getContext().getResources()
                    .getColor(R.color.background_tertiary));
                
                unlockedIndicator.setVisibility(View.GONE);
                lockedOverlay.setVisibility(View.VISIBLE);
                unlockedDateText.setVisibility(View.GONE);
                
                // Reduced opacity for locked achievements
                setViewAlpha(itemView, 0.6f);
            }
            
            // Set category color
            setCategoryColor(achievement.category);
            
            // Animate progress bar
            animateProgressBar(progressBar, progressPercentage);
        }
        
        private void setCategoryColor(String category) {
            int color;
            switch (category) {
                case "consistency":
                    color = R.color.accent_green;
                    break;
                case "volume":
                    color = R.color.purple_500;
                    break;
                case "accuracy":
                    color = R.color.teal_200;
                    break;
                case "milestone":
                    color = R.color.accent_green_dark;
                    break;
                case "special":
                    color = R.color.purple_700;
                    break;
                default:
                    color = R.color.text_secondary;
                    break;
            }
            categoryText.setTextColor(itemView.getContext().getResources().getColor(color));
        }
        
        private void setViewAlpha(View view, float alpha) {
            titleText.setAlpha(alpha);
            descriptionText.setAlpha(alpha);
            iconText.setAlpha(alpha);
            progressText.setAlpha(alpha);
            categoryText.setAlpha(alpha);
            rewardText.setAlpha(alpha);
        }
        
        private void animateProgressBar(ProgressBar progressBar, int targetProgress) {
            ValueAnimator animator = ValueAnimator.ofInt(0, targetProgress);
            animator.setDuration(800);
            animator.addUpdateListener(animation -> {
                int animatedValue = (int) animation.getAnimatedValue();
                progressBar.setProgress(animatedValue);
            });
            
            // Delay animation based on position for staggered effect
            animator.setStartDelay(getAdapterPosition() * 100L);
            animator.start();
        }
    }
} 