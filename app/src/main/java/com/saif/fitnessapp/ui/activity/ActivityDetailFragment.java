package com.saif.fitnessapp.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.card.MaterialCardView;
import com.saif.fitnessapp.R;
import com.saif.fitnessapp.network.dto.ActivityResponse;
import com.saif.fitnessapp.network.dto.Recommendation;
import com.saif.fitnessapp.ui.TitleController;
import com.saif.fitnessapp.ui.activity.utils.MetricsHelper;

import java.util.ArrayList;
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

    // UI Components - Hero Card
    private TextView activityTypeText;
    private TextView intensityBadge;
    private TextView durationText;
    private TextView caloriesText;
    private TextView startTimeText;

    // Lottie Animation
    private MaterialCardView lottieCard;
    private LottieAnimationView lottieAnimation;
    private TextView lottieLoadingText;

    // Metrics Grid
    private LinearLayout metricsContainer;
    private GridLayout metricsGrid;

    // Chart
    private MaterialCardView chartCard;
    private LineChart caloriesChart;

    // AI Recommendation Section
    private MaterialCardView recommendationCard;
    private TextView recommendationText;
    private LinearLayout improvementsContainer;
    private LinearLayout suggestionsContainer;
    private LinearLayout safetyContainer;

    // Error State
    private LinearLayout errorContainer;
    private TextView errorText;

    private ActivityDetailViewModel viewModel;
    private String activityId;
    private String activityType;
    private int duration;
    private int calories;
    private String startTime;
    private ActivityResponse currentActivity;
    private Recommendation currentRecommendation;

    private boolean recommendationLoaded = false;
    private boolean activityLoaded = false;


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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // Enable options menu for share button
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
        loadData();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_activity_detail, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            shareWorkout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeViews(View view) {
        // Hero Header
        activityTypeText = view.findViewById(R.id.activity_type_header);
        intensityBadge = view.findViewById(R.id.intensity_badge);
        durationText = view.findViewById(R.id.duration_value);
        caloriesText = view.findViewById(R.id.calories_value);
        startTimeText = view.findViewById(R.id.start_time_value);

        // Lottie Animation
        lottieCard = view.findViewById(R.id.lottie_card);
        lottieAnimation = view.findViewById(R.id.lottie_animation);
        lottieLoadingText = view.findViewById(R.id.lottie_loading_text);

        // Metrics Grid
        metricsContainer = view.findViewById(R.id.metrics_grid_linear);
        metricsGrid = view.findViewById(R.id.metrics_grid);


        // Chart
        chartCard = view.findViewById(R.id.chart_card);
        caloriesChart = view.findViewById(R.id.calories_chart);

        // Recommendation Section
        recommendationCard = view.findViewById(R.id.recommendation_card);
        recommendationText = view.findViewById(R.id.recommendation_text);
        improvementsContainer = view.findViewById(R.id.improvements_container);
        suggestionsContainer = view.findViewById(R.id.suggestions_container);
        safetyContainer = view.findViewById(R.id.safety_container);

        // Error
        errorContainer = view.findViewById(R.id.error_container);
        errorText = view.findViewById(R.id.error_text);
    }

    private void extractArguments() {
        if (getArguments() != null) {
            activityId = getArguments().getString(ARG_ACTIVITY_ID);
            activityType = getArguments().getString(ARG_ACTIVITY_TYPE);
            duration = getArguments().getInt(ARG_DURATION, 0);
            calories = getArguments().getInt(ARG_CALORIES, 0);
            startTime = getArguments().getString(ARG_START_TIME, "");
        }
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ActivityDetailViewModel.class);
    }

    private void displayBasicInfo() {

        if (calories > 300) {
            intensityBadge.setText("🔥");
//            intensityBadge.setTextColor(Color.);
        } else if (calories > 150) {
            intensityBadge.setText("⚡");
            intensityBadge.setTextColor(Color.YELLOW);
        } else {
            intensityBadge.setText("💙");
            intensityBadge.setTextColor(Color.GREEN);
        }

        activityTypeText.setText((activityType));
        durationText.setText(duration + " min");
        caloriesText.setText(calories + " kcal");
        startTimeText.setText(ActivityAdapter.formatDateTime(startTime));

        // Set Lottie animation based on activity type
        setupLottieAnimation(activityType);
    }

    private void setupLottieAnimation(String type) {
        if (type == null) {
            lottieAnimation.setAnimation(R.raw.running);
            lottieLoadingText.setText("Generating personalized insights... 🧠");
            return;
        }

        int animationRes;
        String message;

        switch (type.toUpperCase()) {
            case "RUNNING":
                animationRes = R.raw.running;
                message = "Analyzing your run with AI... 🧠🏃";
                break;
            case "CYCLING":
                animationRes = R.raw.cycling;
                message = "Analyzing your ride with AI... 🧠🚴";
                break;
            case "SWIMMING":
                animationRes = R.raw.swimming;
                message = "Analyzing your swim with AI... 🧠🏊";
                break;
            case "WALKING":
                animationRes = R.raw.walking;
                message = "Analyzing your walk with AI... 🧠🚶";
                break;
            case "YOGA":
                animationRes = R.raw.yoga;
                message = "Analyzing your yoga session with AI... 🧠🧘";
                break;
//            case "BOXING":
//                animationRes = R.raw.boxing;
//                message = "Analyzing your boxing session with AI... 🧠🥊";
//                break;
            case "WEIGHT_LIFTING":
            case "WEIGHTLIFTING":
                animationRes = R.raw.weight_lifting;
                message = "Analyzing your workout with AI... 🧠🏋️";
                break;
//            case "CARDIO":
//                animationRes = R.raw.cardio;
//                message = "Analyzing your cardio with AI... 🧠💓";
//                break;
//            case "STRETCHING":
//                animationRes = R.raw.stretching;
//                message = "Analyzing your stretching with AI... 🧠🤸";
//                break;
            default:
                animationRes = R.raw.running;
                message = "Generating personalized insights... 🧠";
                break;
        }

        lottieAnimation.setAnimation(animationRes);
        lottieLoadingText.setText(message);
        lottieAnimation.playAnimation();
    }

    private void loadData() {
        if (activityId == null) {
            showError("Activity ID not found");
            return;
        }

        // Show Lottie loading
        showLottieLoading();

        // Fetch full activity details (for metrics)
        viewModel.getActivityDetails(activityId).observe(getViewLifecycleOwner(), activity -> {
            if (activity != null) {
                currentActivity = activity;
                activityLoaded = true;
//                tryShowContentAfterLottie();   // <-- IMPORTANT
            }
        });

// Fetch AI recommendation
        viewModel.getRecommendation(activityId).observe(getViewLifecycleOwner(), recommendation -> {
            if (recommendation != null) {
                currentRecommendation = recommendation;
                recommendationLoaded = true;
                hideLottieLoading();           // first hide lottie
//                tryShowContentAfterLottie();   // then show metrics
                displayRecommendation(recommendation);
            }
        });


        // Observe errors
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                hideLottieLoading();
                showError(error);
            }
        });
    }

    private void tryShowContentAfterLottie() {
        if (activityLoaded && recommendationLoaded) {
            // Now and ONLY now show metrics & chart
            displayMetrics(currentActivity);
            displayChart(currentActivity);
        }
    }


    private void showLottieLoading() {
        lottieCard.setVisibility(View.VISIBLE);
        recommendationCard.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
    }

    private void hideLottieLoading() {
        // Smooth fade out Lottie
        lottieCard.animate()
                .alpha(0f)
                .setDuration(400)
                .withEndAction(() -> {
                    lottieCard.setVisibility(View.GONE);
                    lottieCard.setAlpha(1f);
                    lottieAnimation.pauseAnimation();

                    // Fade in recommendation card
                    recommendationCard.setAlpha(0f);
                    recommendationCard.setVisibility(View.VISIBLE);
                    recommendationCard.animate()
                            .alpha(1f)
                            .setDuration(400)
                            .start();

                    // NOW trigger metrics animation AFTER lottie is gone
                    if (activityLoaded) {
                        displayMetrics(currentActivity);
                        displayChart(currentActivity);
                    }
                })
                .start();
    }

    private void displayMetrics(ActivityResponse activity) {

        LinearLayout metricsContainer =
                requireView().findViewById(R.id.metrics_grid_linear);

        metricsGrid.removeAllViews();

        if (activity.getAdditionalMetrics() == null ||
                activity.getAdditionalMetrics().isEmpty()) {

            metricsContainer.setVisibility(View.GONE);
            return;
        }

        Map<String, MetricsHelper.Metric> metrics =
                MetricsHelper.parseMetrics(activity.getAdditionalMetrics());

        if (metrics.isEmpty()) {
            metricsContainer.setVisibility(View.GONE);
            return;
        }

        metricsContainer.setVisibility(View.VISIBLE);

        // Now add cards
        for (MetricsHelper.Metric metric : metrics.values()) {
            View metricCard = createMetricCard(metric);
            metricsGrid.addView(metricCard);

        }

        // Fade in animation
//        metricsGrid.setAlpha(0f);
//        metricsGrid.animate()
//                .alpha(1f)
//                .setDuration(400)
//                .setStartDelay(200)
//                .start();
        for (int i = 0; i < metricsGrid.getChildCount(); i++) {
            View card = metricsGrid.getChildAt(i);
            animateMetricCard(card, i);
        }
    }

    private void animateMetricCard(View card, int index) {
        View container = card.findViewById(R.id.metric_container);

        container.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(index * 80L)   // Stagger effect
                .setDuration(350)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }



    private View createMetricCard(MetricsHelper.Metric metric) {
        View cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_metric_card, metricsGrid, false);

        TextView emojiText = cardView.findViewById(R.id.metric_emoji);
        TextView labelText = cardView.findViewById(R.id.metric_label);
        TextView valueText = cardView.findViewById(R.id.metric_value);
        View container = cardView.findViewById(R.id.metric_container);

        emojiText.setText(metric.emoji);
        labelText.setText(metric.label);
        valueText.setText(metric.value);

        // Apply color accent
        int color = Color.parseColor(metric.colorHex);
        valueText.setTextColor(color);

        MaterialCardView card = (MaterialCardView) cardView;
        card.setStrokeColor(color);

        // TAP POP ANIMATION
        card.setOnClickListener(v -> {
            container.animate()
                    .scaleX(0.97f)
                    .scaleY(0.97f)
                    .setDuration(80)
                    .withEndAction(() ->
                            container.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(120)
                                    .start()
                    ).start();
        });
        return cardView;
    }


    private void displayChart(ActivityResponse activity) {
        if (activity.getDuration() == null || activity.getCaloriesBurned() == null ||
                activity.getDuration() <= 0 || activity.getCaloriesBurned() <= 0) {
            chartCard.setVisibility(View.GONE);
            return;
        }

        chartCard.setVisibility(View.VISIBLE);
        caloriesChart.setHighlightPerTapEnabled(true);


        // Generate smooth calorie curve
        List<Entry> entries = generateCalorieCurve(
                activity.getDuration(),
                activity.getCaloriesBurned()
        );

        // Configure chart
        LineDataSet dataSet = new LineDataSet(entries, "Calories Burned");
        dataSet.setColor(getResources().getColor(R.color.primary, null));
        dataSet.setLineWidth(3f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth curve
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getResources().getColor(R.color.primary_light, null));
        dataSet.setFillAlpha(50);

        LineData lineData = new LineData(dataSet);
        caloriesChart.setData(lineData);

        // Customize chart appearance
        Description description = new Description();
        description.setText("Calories vs Time");
        description.setTextSize(12f);
        caloriesChart.setDescription(description);

        caloriesChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        caloriesChart.getXAxis().setGranularity(1f);
        caloriesChart.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return ((int) value) + " min";
            }
        });


        caloriesChart.getAxisRight().setEnabled(false);
        caloriesChart.getAxisLeft().setAxisMinimum(0f);

        caloriesChart.getLegend().setEnabled(false);
        caloriesChart.setTouchEnabled(true);
        caloriesChart.setDragEnabled(true);
        caloriesChart.setScaleEnabled(false);


        float avgCalories = (float) calories / duration;

        LimitLine avgLine = new LimitLine(avgCalories, "Avg");
        avgLine.setLineWidth(1.5f);
        caloriesChart.getAxisLeft().addLimitLine(avgLine);


        // Animate chart
        caloriesChart.animateY(800);
        caloriesChart.invalidate();

        // Fade in animation
        chartCard.setAlpha(0f);
        chartCard.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(400)
                .start();
    }

    /**
     * Generate smooth calorie burn curve
     * Simulates realistic calorie burn over time
     */
    private List<Entry> generateCalorieCurve(int duration, int totalCalories) {
        List<Entry> entries = new ArrayList<>();

        // Create smooth curve with 20 data points
        int points = Math.min(duration, 20);

        for (int i = 0; i <= points; i++) {
            float time = (duration * i) / (float) points;

            // Non-linear calorie burn (faster at start, slower at end)
            float progress = i / (float) points;
            float calories = (float) (totalCalories * (1 - Math.pow(1 - progress, 1.2)));

            entries.add(new Entry(time, calories));
        }

        return entries;
    }

    private void displayRecommendation(Recommendation rec) {
        if (rec.getRecommendation() != null && !rec.getRecommendation().isEmpty()) {
            recommendationText.setText(rec.getRecommendation());
            recommendationText.setVisibility(View.VISIBLE);

            recommendationText.setAlpha(0f);
            recommendationText.animate()
                    .alpha(1f)
                    .setDuration(400)
                    .start();
        }

        //display improvements
        displayBulletList(improvementsContainer, rec.getImprovements(), "✅ ");
        //display suggestions
        displayBulletList(suggestionsContainer, rec.getSuggestions(), "💡 ");
        //display safety-guidelines
        displayBulletList(safetyContainer, rec.getSafety(), "⚠️ ");
    }


//    private void displayBulletList(LinearLayout container, List<String> items, String prefix) {
//        container.removeAllViews();
//
//        if (items == null || items.isEmpty()) {
//            container.setVisibility(View.GONE);
//            return;
//        }
//
//        container.setVisibility(View.VISIBLE);
//
//        for (String item : items) {
//            TextView bulletPoint = new TextView(requireContext());
//            bulletPoint.setText(prefix + item);
//            bulletPoint.setTextSize(14);
//            bulletPoint.setTextColor(getResources().getColor(R.color.text_primary, null));
//
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.MATCH_PARENT,
//                    LinearLayout.LayoutParams.WRAP_CONTENT
//            );
//            params.setMargins(0, 0, 0, 16);
//            bulletPoint.setLayoutParams(params);
//
//            container.addView(bulletPoint);
//        }
//    }

    private void displayBulletList(LinearLayout container, List<String> items, String prefix) {
        container.removeAllViews();

        if (items == null || items.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }

        container.setVisibility(View.VISIBLE);

        for (int i = 0; i < items.size(); i++) {
            View bulletCard = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_recommendation_bullet, container, false);

            TextView icon = bulletCard.findViewById(R.id.bullet_icon);
            TextView text = bulletCard.findViewById(R.id.bullet_text);
            View innerContainer = bulletCard.findViewById(R.id.bullet_container);
            MaterialCardView card = bulletCard.findViewById(R.id.bullet_card);

            icon.setText(prefix);
            text.setText(items.get(i));

            // Color accents based on type
            int color;
            if (prefix.contains("✅")) color = Color.parseColor("#2ECC71"); // Green
            else if (prefix.contains("💡")) color = Color.parseColor("#F1C40F"); // Yellow
            else color = Color.parseColor("#E74C3C"); // Red for safety

            card.setStrokeColor(color);
            icon.setTextColor(color);

            // ENTRY ANIMATION (staggered like metrics)
            innerContainer.setAlpha(0f);
            innerContainer.setTranslationY(20f);

            innerContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(i * 80L)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();

            // TAP ANIMATION
            innerContainer.setOnClickListener(v -> {
                innerContainer.animate()
                        .scaleX(0.97f)
                        .scaleY(0.97f)
                        .setDuration(80)
                        .withEndAction(() ->
                                innerContainer.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(120)
                                        .start()
                        ).start();
            });

            container.addView(bulletCard);
        }
    }


    /**
     * Share workout summary
     */
    private void shareWorkout() {
        if (activityType == null) {
            Toast.makeText(requireContext(), "No activity data to share", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder shareText = new StringBuilder();
        shareText.append("🏃 My Workout Summary\n\n");
        shareText.append("Activity: ").append((activityType)).append("\n");
        shareText.append("Duration: ").append(duration).append(" min\n");
        shareText.append("Calories: ").append(calories).append(" kcal\n");

        // Add AI insight if available
        if (currentRecommendation != null &&
                currentRecommendation.getRecommendation() != null &&
                !currentRecommendation.getRecommendation().isEmpty()) {
            shareText.append("\n🔥 AI Insight:\n");
            shareText.append("\"").append(currentRecommendation.getRecommendation()).append("\"\n");
        }

        shareText.append("\n#FitnessApp #AIWorkout");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Workout Summary");

        if (currentActivity != null &&
                currentActivity.getAdditionalMetrics() != null) {

            shareText.append("\n📊 Metrics:\n");

            for (Map.Entry<String, Object> entry :
                    currentActivity.getAdditionalMetrics().entrySet()) {

                shareText.append("• ")
                        .append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue())
                        .append("\n");
            }
        }


        startActivity(Intent.createChooser(shareIntent, "Share Workout"));
    }

    private void showError(String message) {
        lottieCard.setVisibility(View.GONE);
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