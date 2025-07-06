package com.projectpivot.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ComboAdapter extends RecyclerView.Adapter<ComboAdapter.ComboViewHolder> {
    
    private List<ComboManager.BoxingCombo> combos;
    private OnComboSelectedListener listener;
    
    public interface OnComboSelectedListener {
        void onComboSelected(ComboManager.BoxingCombo combo);
    }
    
    public ComboAdapter(List<ComboManager.BoxingCombo> combos, OnComboSelectedListener listener) {
        this.combos = combos;
        this.listener = listener;
    }
    
    public void updateCombos(List<ComboManager.BoxingCombo> newCombos) {
        this.combos = newCombos;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ComboViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_combo, parent, false);
        return new ComboViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ComboViewHolder holder, int position) {
        ComboManager.BoxingCombo combo = combos.get(position);
        holder.bind(combo, listener);
    }
    
    @Override
    public int getItemCount() {
        return combos.size();
    }
    
    static class ComboViewHolder extends RecyclerView.ViewHolder {
        
        private CardView comboCard;
        private TextView comboIdText;
        private TextView comboSequenceText;
        private CardView playButton;
        private TextView difficultyText;
        private TextView punchCountText;
        
        public ComboViewHolder(@NonNull View itemView) {
            super(itemView);
            
            comboCard = itemView.findViewById(R.id.comboCard);
            comboIdText = itemView.findViewById(R.id.comboIdText);
            comboSequenceText = itemView.findViewById(R.id.comboSequenceText);
            playButton = itemView.findViewById(R.id.playButton);
            difficultyText = itemView.findViewById(R.id.difficultyText);
            punchCountText = itemView.findViewById(R.id.punchCountText);
        }
        
        public void bind(ComboManager.BoxingCombo combo, OnComboSelectedListener listener) {
            // Set combo info
            comboIdText.setText(combo.id);
            comboSequenceText.setText(formatComboSequence(combo.sequence));
            
            // Calculate difficulty and punch count
            int punchCount = calculatePunchCount(combo.sequence);
            String difficulty = getDifficultyLevel(combo.sequence, punchCount);
            
            punchCountText.setText(punchCount + " punches");
            difficultyText.setText(difficulty);
            
            // Set difficulty color
            setDifficultyColor(difficulty);
            
            // Set click listeners
            comboCard.setOnClickListener(v -> {
                SoundManager.getInstance(v.getContext()).hapticSelect();
                if (listener != null) {
                    listener.onComboSelected(combo);
                }
            });
            
            playButton.setOnClickListener(v -> {
                SoundManager.getInstance(v.getContext()).hapticSelect();
                if (listener != null) {
                    listener.onComboSelected(combo);
                }
            });
        }
        
        private String formatComboSequence(String sequence) {
            // Convert numbers to punch names for better readability
            return sequence
                .replace("1", "Jab")
                .replace("2", "Cross")
                .replace("3", "L-Hook")
                .replace("4", "R-Hook")
                .replace("5", "L-Uppercut")
                .replace("6", "R-Uppercut")
                .replace("7", "L-Overhand")
                .replace("8", "R-Overhand")
                .replace(",", " → ")
                .replace("-", " → ");
        }
        
        private int calculatePunchCount(String sequence) {
            // Count the number of punches in the sequence
            String[] parts = sequence.split("[,-]");
            int count = 0;
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty() && 
                    !trimmed.toLowerCase().contains("slip") &&
                    !trimmed.toLowerCase().contains("duck") &&
                    !trimmed.toLowerCase().contains("pivot") &&
                    !trimmed.toLowerCase().contains("step")) {
                    count++;
                }
            }
            return count;
        }
        
        private String getDifficultyLevel(String sequence, int punchCount) {
            // Determine difficulty based on complexity
            String lowerSequence = sequence.toLowerCase();
            
            if (punchCount <= 2) {
                return "BEGINNER";
            } else if (punchCount <= 4 && !containsAdvancedMoves(lowerSequence)) {
                return "INTERMEDIATE";
            } else if (punchCount <= 6 && !containsAdvancedMoves(lowerSequence)) {
                return "ADVANCED";
            } else {
                return "EXPERT";
            }
        }
        
        private boolean containsAdvancedMoves(String sequence) {
            return sequence.contains("slip") || 
                   sequence.contains("duck") || 
                   sequence.contains("pivot") ||
                   sequence.contains("overhand") ||
                   sequence.contains("7") ||
                   sequence.contains("8");
        }
        
        private void setDifficultyColor(String difficulty) {
            int color;
            switch (difficulty) {
                case "BEGINNER":
                    color = R.color.accent_green;
                    break;
                case "INTERMEDIATE":
                    color = R.color.teal_200;
                    break;
                case "ADVANCED":
                    color = R.color.purple_500;
                    break;
                case "EXPERT":
                    color = R.color.error_red;
                    break;
                default:
                    color = R.color.text_secondary;
                    break;
            }
            difficultyText.setTextColor(itemView.getContext().getResources().getColor(color));
        }
    }
} 