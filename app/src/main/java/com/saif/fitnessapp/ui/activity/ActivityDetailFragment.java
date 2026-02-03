package com.saif.fitnessapp.ui.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;
import com.saif.fitnessapp.R;
import com.saif.fitnessapp.network.dto.ActivityResponse;
import com.saif.fitnessapp.network.dto.Recommendation;
import com.saif.fitnessapp.ui.TitleController;

import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ActivityDetailFragment extends Fragment {

    private static final String ARG_ACTIVITY_ID = "activity_id";
    private static final String ARG_ACTIVITY_TYPE = "activity_type";
    private static final String ARG_DURATION = "duration";
    private static final String ARG_CALORIES = "calories";
    private static final String ARG_START_TIME = "start_time";

    // UI Components
    private TextView activityTypeText;
    private TextView durationText;
    private TextView caloriesText;
    private TextView startTimeText;
    private LinearLayout metricsContainer;
    private TextView metricsTitle;
    private TextView noMetricsText;
    
    // AI Recommendation Section
    private MaterialCardView recommendationCard;
    private TextView recommendationText;
    private LinearLayout improvementsContainer;
    private LinearLayout suggestionsContainer;
    private LinearLayout safetyContainer;
    
    // Loading Animation
    private LinearLayout loadingContainer;
    private ProgressBar loadingProgress;
    private TextView loadingText;
    
    // Error State
    private LinearLayout errorContainer;
    private TextView errorText;

    private ActivityDetailViewModel viewModel;
    private String activityId;
    private String activityType;

    public static ActivityDetailFragment newInstance(
            String activityId, 
            String activityType, 
            int duration, 
            int calories, 
            String startTime
    ) {
        ActivityDetailFragment fragment = new ActivityDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ACTIVITY_ID, activityId);
        args.putString(ARG_ACTIVITY_TYPE, activityType);
        args.putInt(ARG_DURATION, duration);
        args.putInt(ARG_CALORIES, calories);
        args.putString(ARG_START_TIME, startTime);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_activity_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        extractArguments();
        setupViewModel();
        displayBasicInfo();
        loadRecommendation();
    }

    private void initializeViews(View view) {
        // Hero Header
        activityTypeText = view.findViewById(R.id.activity_type_header);
        durationText = view.findViewById(R.id.duration_value);
        caloriesText = view.findViewById(R.id.calories_value);
        startTimeText = view.findViewById(R.id.start_time_value);
        
        // Metrics Section
        metricsContainer = view.findViewById(R.id.metrics_container);
        metricsTitle = view.findViewById(R.id.metrics_title);
        noMetricsText = view.findViewById(R.id.no_metrics_text);
        
        // Recommendation Section
        recommendationCard = view.findViewById(R.id.recommendation_card);
        recommendationText = view.findViewById(R.id.recommendation_text);
        improvementsContainer = view.findViewById(R.id.improvements_container);
        suggestionsContainer = view.findViewById(R.id.suggestions_container);
        safetyContainer = view.findViewById(R.id.safety_container);
        
        // Loading
        loadingContainer = view.findViewById(R.id.loading_container);
        loadingProgress = view.findViewById(R.id.loading_progress);
        loadingText = view.findViewById(R.id.loading_text);
        
        // Error
        errorContainer = view.findViewById(R.id.error_container);
        errorText = view.findViewById(R.id.error_text);
    }

    private void extractArguments() {
        if (getArguments() != null) {
            activityId = getArguments().getString(ARG_ACTIVITY_ID);
            activityType = getArguments().getString(ARG_ACTIVITY_TYPE);
        }
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ActivityDetailViewModel.class);
    }

    private void displayBasicInfo() {
        if (getArguments() == null) return;

        String type = getArguments().getString(ARG_ACTIVITY_TYPE, "Activity");
        int duration = getArguments().getInt(ARG_DURATION, 0);
        int calories = getArguments().getInt(ARG_CALORIES, 0);
        String startTime = getArguments().getString(ARG_START_TIME, "");

        activityTypeText.setText(formatActivityType(type));
        durationText.setText(duration + " min");
        caloriesText.setText(calories + " kcal");
        startTimeText.setText(ActivityAdapter.formatDateTime(startTime));
        
        // Set loading text based on activity type
        setLoadingMessage(type);
    }

    private void setLoadingMessage(String type) {
        if (type == null) {
            loadingText.setText("Generating personalized insights...");
            return;
        }
        
        switch (type.toUpperCase()) {
            case "RUNNING":
                loadingText.setText("Analyzing your run with AI... 🧠🏃‍♂️");
                break;
            case "CYCLING":
                loadingText.setText("Analyzing your ride with AI... 🧠🚴‍♂️");
                break;
            case "SWIMMING":
                loadingText.setText("Analyzing your swim with AI... 🧠🏊‍♂️");
                break;
            case "WALKING":
                loadingText.setText("Analyzing your walk with AI... 🧠🚶‍♂️");
                break;
            case "YOGA":
                loadingText.setText("Analyzing your yoga session with AI... 🧠🧘‍♂️");
                break;
            case "GYM":
            case "WEIGHTLIFTING":
                loadingText.setText("Analyzing your workout with AI... 🧠🏋️‍♂️");
                break;
            default:
                loadingText.setText("Generating personalized insights... 🧠");
                break;
        }
    }

    private void loadRecommendation() {
        if (activityId == null) {
            showError("Activity ID not found");
            return;
        }

        // Show loading animation
        showLoading();

        // Observe recommendation
        viewModel.getRecommendation(activityId).observe(getViewLifecycleOwner(), recommendation -> {
            if (recommendation != null) {
                hideLoading();
                displayRecommendation(recommendation);
            }
        });

        // Observe errors
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                hideLoading();
                showError(error);
            }
        });
    }

    private void showLoading() {
        loadingContainer.setVisibility(View.VISIBLE);
        recommendationCard.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
    }

    private void hideLoading() {
        // Smooth fade out animation
        loadingContainer.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    loadingContainer.setVisibility(View.GONE);
                    loadingContainer.setAlpha(1f);
                    
                    // Fade in recommendation card
                    recommendationCard.setAlpha(0f);
                    recommendationCard.setVisibility(View.VISIBLE);
                    recommendationCard.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start();
                })
                .start();
    }

    private void displayRecommendation(Recommendation rec) {
        // Main recommendation text
        if (rec.getRecommendation() != null && !rec.getRecommendation().isEmpty()) {
            recommendationText.setText(rec.getRecommendation());
            recommendationText.setVisibility(View.VISIBLE);
        } else {
            recommendationText.setVisibility(View.GONE);
        }

        // Improvements
        displayBulletList(improvementsContainer, rec.getImprovements(), "✅ ");

        // Suggestions
        displayBulletList(suggestionsContainer, rec.getSuggestions(), "💡 ");

        // Safety Tips
        displayBulletList(safetyContainer, rec.getSafety(), "⚠️ ");
    }

    private void displayBulletList(LinearLayout container, List<String> items, String prefix) {
        container.removeAllViews();
        
        if (items == null || items.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }

        container.setVisibility(View.VISIBLE);

        for (String item : items) {
            TextView bulletPoint = new TextView(requireContext());
            bulletPoint.setText(prefix + item);
            bulletPoint.setTextSize(14);
            bulletPoint.setTextColor(getResources().getColor(R.color.text_primary, null));
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            bulletPoint.setLayoutParams(params);
            
            container.addView(bulletPoint);
        }
    }

    private void showError(String message) {
        loadingContainer.setVisibility(View.GONE);
        recommendationCard.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
        errorText.setText(message);
        
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private String formatActivityType(String type) {
        if (type == null) return "Activity";
        
        // Convert enum format to readable format
        // RUNNING → Running
        // WEIGHT_LIFTING → Weight Lifting
        return type.substring(0, 1).toUpperCase() + 
               type.substring(1).toLowerCase().replace("_", " ");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requireActivity() instanceof TitleController) {
            ((TitleController) requireActivity()).setTitle("Activity Details");
        }
    }
}