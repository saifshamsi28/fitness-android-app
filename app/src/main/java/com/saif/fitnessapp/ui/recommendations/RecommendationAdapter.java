package com.saif.fitnessapp.ui.recommendations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.saif.fitnessapp.R;
import com.saif.fitnessapp.network.dto.Recommendation;

import java.util.Objects;

public class RecommendationAdapter extends ListAdapter<Recommendation, RecommendationAdapter.RecommendationViewHolder> {

    public RecommendationAdapter() {
        super(new DiffUtil.ItemCallback<Recommendation>() {
            @Override
            public boolean areItemsTheSame(@NonNull Recommendation oldItem, @NonNull Recommendation newItem) {
                if (oldItem.getId() == null || newItem.getId() == null) {
                    return false;
                }
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Recommendation oldItem, @NonNull Recommendation newItem) {
                // Compare all relevant fields to check if content has changed
                return Objects.equals(oldItem.getId(), newItem.getId()) &&
                        Objects.equals(oldItem.getUserId(), newItem.getUserId()) &&
                        Objects.equals(oldItem.getActivityId(), newItem.getActivityId()) &&
                        Objects.equals(oldItem.getActivityType(), newItem.getActivityType()) &&
                        Objects.equals(oldItem.getRecommendation(), newItem.getRecommendation()) &&
                        Objects.equals(oldItem.getImprovements(), newItem.getImprovements()) &&
                        Objects.equals(oldItem.getSuggestions(), newItem.getSuggestions()) &&
                        Objects.equals(oldItem.getSafety(), newItem.getSafety()) &&
                        Objects.equals(oldItem.getCreatedAt(), newItem.getCreatedAt());
            }
        });
    }

    @NonNull
    @Override
    public RecommendationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommendation, parent, false);
        return new RecommendationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecommendationViewHolder holder, int position) {
        Recommendation recommendation = getItem(position);
        holder.bind(recommendation);
    }

    static class RecommendationViewHolder extends RecyclerView.ViewHolder {
        private final TextView activityType;
        private final TextView recommendationText;
        private final TextView improvementsText;
        private final TextView suggestionsText;
        private final TextView safetyText;

        public RecommendationViewHolder(@NonNull View itemView) {
            super(itemView);
            activityType = itemView.findViewById(R.id.activity_type);
            recommendationText = itemView.findViewById(R.id.recommendation_text);
            improvementsText = itemView.findViewById(R.id.improvements_text);
            suggestionsText = itemView.findViewById(R.id.suggestions_text);
            safetyText = itemView.findViewById(R.id.safety_text);
        }

        public void bind(Recommendation recommendation) {
            activityType.setText(recommendation.getActivityType());
            recommendationText.setText(recommendation.getRecommendation());

            // Format improvements
            if (recommendation.getImprovements() != null && !recommendation.getImprovements().isEmpty()) {
                improvementsText.setText("Improvements:\n" + String.join("\n", recommendation.getImprovements()));
            }

            // Format suggestions
            if (recommendation.getSuggestions() != null && !recommendation.getSuggestions().isEmpty()) {
                suggestionsText.setText("Suggestions:\n" + String.join("\n", recommendation.getSuggestions()));
            }

            // Format safety tips
            if (recommendation.getSafety() != null && !recommendation.getSafety().isEmpty()) {
                safetyText.setText("Safety:\n" + String.join("\n", recommendation.getSafety()));
            }
        }
    }
}
