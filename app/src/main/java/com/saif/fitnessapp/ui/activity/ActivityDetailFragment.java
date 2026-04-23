package com.saif.fitnessapp.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
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
    // Chart stats strip
    private TextView chartCaloriesBadge;
    private TextView chartStatDuration;
    private TextView chartStatAvgRate;
    private TextView chartStatTotal;

    // AI Recommendation Section
    private MaterialCardView recommendationCard;
    private TextView recommendationText;
    // Expand/collapse for the AI insight body text
    private LinearLayout aiRecExpandRow;
    private TextView aiRecExpandLabel;
    private boolean aiRecExpanded = false;

    // Section cards (hidden when empty)
    private MaterialCardView improvementsCard;
    private MaterialCardView suggestionsCard;
    private MaterialCardView safetyCard;

    // Collapsible headers
    private LinearLayout improvementsHeader;
    private LinearLayout suggestionsHeader;
    private LinearLayout safetyHeader;

    // Collapsible bodies
    private LinearLayout improvementsBody;
    private LinearLayout suggestionsBody;
    private LinearLayout safetyBody;

    // Chevrons
    private TextView improvementsChevron;
    private TextView suggestionsChevron;
    private TextView safetyChevron;

    // Item count labels
    private TextView improvementsCount;
    private TextView suggestionsCount;
    private TextView safetyCount;

    // Containers (populated dynamically)
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
        chartCaloriesBadge = view.findViewById(R.id.chart_calories_badge);
        chartStatDuration = view.findViewById(R.id.chart_stat_duration);
        chartStatAvgRate = view.findViewById(R.id.chart_stat_avgrate);
        chartStatTotal = view.findViewById(R.id.chart_stat_total);

        // Recommendation Section
        recommendationCard = view.findViewById(R.id.recommendation_card);
        recommendationText = view.findViewById(R.id.recommendation_text);
        aiRecExpandRow   = view.findViewById(R.id.ai_rec_expand_row);
        aiRecExpandLabel = view.findViewById(R.id.ai_rec_expand_label);

        improvementsCard = view.findViewById(R.id.improvements_card);
        suggestionsCard  = view.findViewById(R.id.suggestions_card);
        safetyCard       = view.findViewById(R.id.safety_card);

        improvementsHeader = view.findViewById(R.id.improvements_header);
        suggestionsHeader  = view.findViewById(R.id.suggestions_header);
        safetyHeader       = view.findViewById(R.id.safety_header);

        improvementsBody = view.findViewById(R.id.improvements_body);
        suggestionsBody  = view.findViewById(R.id.suggestions_body);
        safetyBody       = view.findViewById(R.id.safety_body);

        improvementsChevron = view.findViewById(R.id.improvements_chevron);
        suggestionsChevron  = view.findViewById(R.id.suggestions_chevron);
        safetyChevron       = view.findViewById(R.id.safety_chevron);

        improvementsCount = view.findViewById(R.id.improvements_count);
        suggestionsCount  = view.findViewById(R.id.suggestions_count);
        safetyCount       = view.findViewById(R.id.safety_count);

        improvementsContainer = view.findViewById(R.id.improvements_container);
        suggestionsContainer  = view.findViewById(R.id.suggestions_container);
        safetyContainer       = view.findViewById(R.id.safety_container);

        setupExpandCollapse(improvementsHeader, improvementsBody, improvementsChevron);
        setupExpandCollapse(suggestionsHeader,  suggestionsBody,  suggestionsChevron);
        setupExpandCollapse(safetyHeader,       safetyBody,       safetyChevron);

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

        int dur = activity.getDuration();
        int cal = activity.getCaloriesBurned();

        // ── Populate bottom stats strip ───────────────────────────────
        if (chartCaloriesBadge != null)  chartCaloriesBadge.setText(cal + " kcal");
        if (chartStatDuration != null)   chartStatDuration.setText(dur + " min");
        if (chartStatTotal != null)      chartStatTotal.setText(cal + " kcal");
        if (chartStatAvgRate != null) {
            float avg = (float) cal / dur;
            chartStatAvgRate.setText(String.format(java.util.Locale.US, "%.1f", avg));
        }

        // ── Build data ────────────────────────────────────────────────
        List<Entry> entries = generateCalorieCurve(dur, cal);

        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setColor(Color.parseColor("#64B5F6"));
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#1E88E5"));
        dataSet.setFillAlpha(55);
        dataSet.setHighlightEnabled(true);
        dataSet.setHighLightColor(Color.parseColor("#FFB74D"));
        dataSet.setHighlightLineWidth(1.5f);

        LineData lineData = new LineData(dataSet);
        caloriesChart.setData(lineData);

        // ── Dark-mode chart styling ───────────────────────────────────
        // Background transparent (card is dark)
        caloriesChart.setBackgroundColor(Color.TRANSPARENT);
        caloriesChart.setDrawGridBackground(false);
        caloriesChart.setDrawBorders(false);
        caloriesChart.setHighlightPerTapEnabled(true);
        caloriesChart.setTouchEnabled(true);
        caloriesChart.setDragEnabled(true);
        caloriesChart.setScaleEnabled(false);

        // No description label
        Description description = new Description();
        description.setText("");
        caloriesChart.setDescription(description);

        // Legend off
        caloriesChart.getLegend().setEnabled(false);

        // X axis
        XAxis xAxis = caloriesChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#80FFFFFF"));
        xAxis.setTextSize(10f);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#15FFFFFF"));
        xAxis.setAxisLineColor(Color.parseColor("#25FFFFFF"));
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return ((int) value) + " m";
            }
        });

        // Left Y axis
        caloriesChart.getAxisLeft().setAxisMinimum(0f);
        caloriesChart.getAxisLeft().setTextColor(Color.parseColor("#80FFFFFF"));
        caloriesChart.getAxisLeft().setTextSize(10f);
        caloriesChart.getAxisLeft().setDrawGridLines(true);
        caloriesChart.getAxisLeft().setGridColor(Color.parseColor("#15FFFFFF"));
        caloriesChart.getAxisLeft().setAxisLineColor(Color.parseColor("#25FFFFFF"));
        caloriesChart.getAxisLeft().setDrawTopYLabelEntry(false);

        // Right Y axis off
        caloriesChart.getAxisRight().setEnabled(false);

        // Avg calorie/min limit line
        caloriesChart.getAxisLeft().removeAllLimitLines();
        float avgCalPerMin = (float) cal / dur;
        LimitLine avgLine = new LimitLine(avgCalPerMin * (dur / 2f), "Avg");
        avgLine.setLineWidth(1f);
        avgLine.setLineColor(Color.parseColor("#80FFB74D"));
        avgLine.enableDashedLine(10f, 6f, 0f);
        avgLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        avgLine.setTextColor(Color.parseColor("#FFB74D"));
        avgLine.setTextSize(9f);
        caloriesChart.getAxisLeft().addLimitLine(avgLine);

        // ── Animate ───────────────────────────────────────────────────
        caloriesChart.animateY(900, com.github.mikephil.charting.animation.Easing.EaseInOutQuart);
        caloriesChart.invalidate();

        // Fade-in the card
        chartCard.setAlpha(0f);
        chartCard.animate()
                .alpha(1f)
                .setDuration(450)
                .setStartDelay(350)
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
        // Display the main AI recommendation text (normalize spacing)
        if (rec.getRecommendation() != null && !rec.getRecommendation().isEmpty()) {
            // Collapse 2+ consecutive blank lines → single newline, trim edges
            String cleanText = rec.getRecommendation()
                    .replaceAll("\\r\\n", "\n")
                    .replaceAll("\\n{2,}", "\n")
                    .trim();
            recommendationText.setText(cleanText);
            recommendationText.setVisibility(View.VISIBLE);

            recommendationText.setAlpha(0f);
            recommendationText.animate()
                    .alpha(1f)
                    .setDuration(400)
                    .start();

            // Once laid out, show expand row only if text exceeds 4 lines
            recommendationText.post(() -> {
                if (!isAdded() || aiRecExpandRow == null) return;
                if (recommendationText.getLineCount() > 4) {
                    aiRecExpanded = false;
                    aiRecExpandRow.setVisibility(View.VISIBLE);
                    aiRecExpandRow.setOnClickListener(v -> toggleAiRecExpansion());
                } else {
                    aiRecExpandRow.setVisibility(View.GONE);
                }
            });
        }

        // Display improvements, suggestions, and safety tips
        displayBulletList(improvementsContainer, improvementsCard, improvementsCount, rec.getImprovements(), "I");
        displayBulletList(suggestionsContainer,  suggestionsCard,  suggestionsCount,  rec.getSuggestions(),  "T");
        displayBulletList(safetyContainer,       safetyCard,       safetyCount,       rec.getSafety(),       "S");
    }

    private void displayBulletList(LinearLayout container, MaterialCardView sectionCard,
                                    TextView countLabel, List<String> items, String sectionTag) {
        container.removeAllViews();

        if (items == null || items.isEmpty()) {
            if (sectionCard != null) sectionCard.setVisibility(View.GONE);
            return;
        }

        if (sectionCard != null) sectionCard.setVisibility(View.VISIBLE);
        if (countLabel != null) {
            countLabel.setText(items.size() + (items.size() == 1 ? " item" : " items"));
        }
        container.setVisibility(View.VISIBLE);

        for (int i = 0; i < items.size(); i++) {
            String fullText = items.get(i);
            String label = "";
            String description = "";

            // ========================================
            // STEP 1: Parse "Label: Description" format
            // ========================================
            if (fullText.contains(":")) {
                // Split at first colon only
                String[] parts = fullText.split(":", 2);
                label = parts[0].trim();           // "Stroke Rate"
                description = parts.length > 1 ? parts[1].trim() : ""; // "Incorporate..."
            } else {
                // Fallback if no colon found - use entire text as label
                label = fullText;
                description = "";
            }

            // ========================================
            // STEP 2: Inflate the layout
            // ========================================
            View bulletCard = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_recommendation_bullet, container, false);

            // ========================================
            // STEP 3: Get view references
            // ========================================
            TextView icon = bulletCard.findViewById(R.id.bullet_icon);
            TextView labelText = bulletCard.findViewById(R.id.bullet_label);
            TextView descriptionText = bulletCard.findViewById(R.id.bullet_text);
            View innerContainer = bulletCard.findViewById(R.id.bullet_container);
            MaterialCardView card = bulletCard.findViewById(R.id.bullet_card);

            // ========================================
            // STEP 4: Set text content
            // ========================================
            icon.setText("\u25CF");                // filled circle ● — no emoji
            labelText.setText(label);
            descriptionText.setText(description);

            // ========================================
            // STEP 5: Apply color coding by section
            // ========================================
            int color;
            int colorLight;
            if ("I".equals(sectionTag)) {          // Improvements — deep blue
                color      = Color.parseColor("#1565C0");
                colorLight = Color.parseColor("#E3F2FD");
            } else if ("T".equals(sectionTag)) {   // Training Tips — teal
                color      = Color.parseColor("#00695C");
                colorLight = Color.parseColor("#E0F2F1");
            } else if ("S".equals(sectionTag)) {   // Safety — warm amber
                color      = Color.parseColor("#E65100");
                colorLight = Color.parseColor("#FFF3E0");
            } else {                               // Default — deep purple
                color      = Color.parseColor("#4527A0");
                colorLight = Color.parseColor("#EDE7F6");
            }

            // Set accent bar color
            View accentBar = bulletCard.findViewById(R.id.accent_bar);
            if (accentBar != null) accentBar.setBackgroundColor(color);

            // Set icon circle background (light tint)
            View iconCircle = bulletCard.findViewById(R.id.icon_circle);
            if (iconCircle != null) {
                android.graphics.drawable.GradientDrawable circleBg =
                        new android.graphics.drawable.GradientDrawable();
                circleBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                circleBg.setColor(colorLight);
                iconCircle.setBackground(circleBg);
            }

            icon.setTextColor(color);
            labelText.setTextColor(color);

            // ========================================
            // STEP 6: Entry animation (staggered)
            // ========================================
            innerContainer.setAlpha(0f);
            innerContainer.setTranslationY(20f);

            innerContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(i * 80L)        // Stagger each item by 80ms
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();

            // ========================================
            // STEP 7: Tap animation (feedback)
            // ========================================
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

            // Add the card to the container
            container.addView(bulletCard);
        }
    }


    // ─────────────────────────────────────────────────────────────────────
    // Expand / collapse helper for the AI insight body text
    // (AnimoTransition gives the same slide-in effect as recommendation cards)
    // ─────────────────────────────────────────────────────────────────────
    private void toggleAiRecExpansion() {
        if (aiRecExpandRow == null || aiRecExpandLabel == null) return;
        aiRecExpanded = !aiRecExpanded;

        // Animate the height change of the parent card
        View parent = (View) recommendationText.getParent();
        if (parent instanceof ViewGroup) {
            TransitionManager.beginDelayedTransition((ViewGroup) parent,
                    new AutoTransition().setDuration(250));
        }

        if (aiRecExpanded) {
            recommendationText.setMaxLines(Integer.MAX_VALUE);
            recommendationText.setEllipsize(null);
            aiRecExpandLabel.setText("Show less \u25B2");
        } else {
            recommendationText.setMaxLines(4);
            recommendationText.setEllipsize(android.text.TextUtils.TruncateAt.END);
            aiRecExpandLabel.setText("Show more \u25BC");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Expand / collapse helper
    // ─────────────────────────────────────────────────────────────────────
    private void setupExpandCollapse(LinearLayout header, LinearLayout body, TextView chevron) {
        if (header == null || body == null) return;
        header.setOnClickListener(v -> {
            boolean isExpanded = body.getVisibility() == View.VISIBLE;
            if (isExpanded) {
                // Collapse with fade
                body.animate()
                        .alpha(0f)
                        .setDuration(180)
                        .withEndAction(() -> body.setVisibility(View.GONE))
                        .start();
                if (chevron != null) chevron.setText("\u25B6"); // ▶
            } else {
                // Expand with fade
                body.setAlpha(0f);
                body.setVisibility(View.VISIBLE);
                body.animate()
                        .alpha(1f)
                        .setDuration(220)
                        .start();
                if (chevron != null) chevron.setText("\u25BC"); // ▼
            }
        });
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