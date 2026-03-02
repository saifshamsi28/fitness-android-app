package com.saif.fitnessapp.ui.recommendations;

import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.saif.fitnessapp.R;
import com.saif.fitnessapp.network.dto.Recommendation;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class RecommendationAdapter extends ListAdapter<Recommendation, RecommendationAdapter.RecommendationViewHolder> {

    private final Set<Integer> expandedPositions = new HashSet<>();

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
        boolean isExpanded = expandedPositions.contains(position);
        holder.bind(recommendation, isExpanded);

        holder.headerContainer.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            boolean expanding = !expandedPositions.contains(pos);
            if (expanding) {
                expandedPositions.add(pos);
            } else {
                expandedPositions.remove(pos);
            }

            // Animate the expand/collapse transition
            if (holder.itemView.getParent() instanceof ViewGroup) {
                TransitionManager.beginDelayedTransition(
                        (ViewGroup) holder.itemView.getParent(),
                        new AutoTransition().setDuration(250)
                );
            }

            holder.expandableContent.setVisibility(expanding ? View.VISIBLE : View.GONE);
            holder.recommendationText.setMaxLines(expanding ? Integer.MAX_VALUE : 3);
            holder.expandIcon.animate()
                    .rotation(expanding ? 180f : 0f)
                    .setDuration(200)
                    .start();
        });
    }

    static class RecommendationViewHolder extends RecyclerView.ViewHolder {
        final View headerContainer;
        final TextView activityEmoji;
        final TextView activityType;
        final ImageView expandIcon;
        final TextView recommendationText;
        final LinearLayout expandableContent;
        final LinearLayout improvementsSection;
        final LinearLayout suggestionsSection;
        final LinearLayout safetySection;
        final TextView improvementsText;
        final TextView suggestionsText;
        final TextView safetyText;

        public RecommendationViewHolder(@NonNull View itemView) {
            super(itemView);
            headerContainer = itemView.findViewById(R.id.header_container);
            activityEmoji = itemView.findViewById(R.id.activity_emoji);
            activityType = itemView.findViewById(R.id.activity_type);
            expandIcon = itemView.findViewById(R.id.expand_icon);
            recommendationText = itemView.findViewById(R.id.recommendation_text);
            expandableContent = itemView.findViewById(R.id.expandable_content);
            improvementsSection = itemView.findViewById(R.id.improvements_section);
            suggestionsSection = itemView.findViewById(R.id.suggestions_section);
            safetySection = itemView.findViewById(R.id.safety_section);
            improvementsText = itemView.findViewById(R.id.improvements_text);
            suggestionsText = itemView.findViewById(R.id.suggestions_text);
            safetyText = itemView.findViewById(R.id.safety_text);
        }

        public void bind(Recommendation recommendation, boolean isExpanded) {
            // Activity type header with emoji
            activityType.setText(formatActivityType(recommendation.getActivityType()));
            activityEmoji.setText(getActivityEmoji(recommendation.getActivityType()));

            // Recommendation text (preview when collapsed, full when expanded)
            if (recommendation.getRecommendation() != null) {
                recommendationText.setText(recommendation.getRecommendation());
                recommendationText.setVisibility(View.VISIBLE);
            } else {
                recommendationText.setVisibility(View.GONE);
            }
            recommendationText.setMaxLines(isExpanded ? Integer.MAX_VALUE : 3);

            // Expand/collapse state
            expandableContent.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            expandIcon.setRotation(isExpanded ? 180f : 0f);

            // Improvements section
            if (recommendation.getImprovements() != null && !recommendation.getImprovements().isEmpty()) {
                improvementsText.setText(formatBulletList(recommendation.getImprovements()));
                improvementsSection.setVisibility(View.VISIBLE);
            } else {
                improvementsSection.setVisibility(View.GONE);
            }

            // Suggestions section
            if (recommendation.getSuggestions() != null && !recommendation.getSuggestions().isEmpty()) {
                suggestionsText.setText(formatBulletList(recommendation.getSuggestions()));
                suggestionsSection.setVisibility(View.VISIBLE);
            } else {
                suggestionsSection.setVisibility(View.GONE);
            }

            // Safety tips section
            if (recommendation.getSafety() != null && !recommendation.getSafety().isEmpty()) {
                safetyText.setText(formatBulletList(recommendation.getSafety()));
                safetySection.setVisibility(View.VISIBLE);
            } else {
                safetySection.setVisibility(View.GONE);
            }
        }

        private String formatBulletList(List<String> items) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                sb.append("\u2022 ").append(items.get(i));
                if (i < items.size() - 1) sb.append("\n");
            }
            return sb.toString();
        }

        private String formatActivityType(String type) {
            if (type == null) return "Activity";
            String formatted = type.replace("_", " ");
            return formatted.substring(0, 1).toUpperCase()
                    + formatted.substring(1).toLowerCase();
        }

        private String getActivityEmoji(String type) {
            if (type == null) return "\uD83C\uDFC3";
            switch (type.toUpperCase()) {
                case "RUNNING": return "\uD83C\uDFC3";
                case "SWIMMING": return "\uD83C\uDFCA";
                case "WALKING": return "\uD83D\uDEB6";
                case "CYCLING": return "\uD83D\uDEB4";
                case "YOGA": return "\uD83E\uDDD8";
                case "WEIGHT_LIFTING": return "\uD83C\uDFCB\uFE0F";
                case "BOXING": return "\uD83E\uDD4A";
                case "CARDIO": return "\u2764\uFE0F";
                case "STRETCHING": return "\uD83E\uDD38";
                default: return "\uD83C\uDFC3";
            }
        }
    }
}
