package com.saif.fitnessapp.ui.home;

import static java.lang.Math.round;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.saif.fitnessapp.R;
import com.saif.fitnessapp.activity.ActivityViewModel;
import com.saif.fitnessapp.auth.TokenManager;
import com.saif.fitnessapp.network.dto.ActivityRequest;
import com.saif.fitnessapp.ui.activity.utils.FitnessMetricCalculator;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

@AndroidEntryPoint
public class AddActivityFragment extends Fragment {

    @Inject
    TokenManager tokenManager;

    private ActivityViewModel activityViewModel;
    private Spinner activityTypeSpinner;
    private EditText durationInput;
    private EditText caloriesInput;
    private Button submitButton;
    private ProgressBar progressBar;


    private static final String[] ACTIVITY_TYPES = {
            "RUNNING", "SWIMMING", "WALKING", "BOXING", "CYCLING",
            "WEIGHT_LIFTING", "CARDIO", "STRETCHING", "YOGA"
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
                            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_activity, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activityTypeSpinner = view.findViewById(R.id.activity_type_spinner);
        durationInput = view.findViewById(R.id.duration_input);
        caloriesInput = view.findViewById(R.id.calories_input);
        submitButton = view.findViewById(R.id.submit_button);
        progressBar = view.findViewById(R.id.progressBar);


        activityViewModel = new ViewModelProvider(this).get(ActivityViewModel.class);

        // Setup spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), 
                android.R.layout.simple_spinner_item, 
                ACTIVITY_TYPES
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activityTypeSpinner.setAdapter(adapter);

        submitButton.setOnClickListener(v -> submitActivity());

    }

    private void submitActivity() {
        String userId = tokenManager.getUserId();
        String activityType = (String) activityTypeSpinner.getSelectedItem();
        String durationStr = durationInput.getText().toString().trim();
        String caloriesStr = caloriesInput.getText().toString().trim();

        if (durationStr.isEmpty() || caloriesStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int duration;
        int calories;

        try {
            duration = Integer.parseInt(durationStr);
            calories = Integer.parseInt(caloriesStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid input", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loader and diable button
        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);

        String startTime;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startTime = LocalDateTime.now()
                    .format(DateTimeFormatter.ISO_DATE_TIME);
        } else {
            startTime = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS",
                    Locale.US
            ).format(new Date());

        }
        Map<String, Object> additionalMetrics =
                generateAdditionalMetrics(activityType, duration, calories);

        ActivityRequest request = new ActivityRequest(
                userId,
                activityType,
                duration,
                calories,
                startTime,
                additionalMetrics
        );

        activityViewModel.trackActivity(request)
                .observe(getViewLifecycleOwner(), response -> {

                    // Hide loader and enable button
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);

                    if (response != null) {
                        Toast.makeText(requireContext(),
                                "Activity tracked successfully!",
                                Toast.LENGTH_SHORT).show();

                        // Navigate only after success
                        Navigation.findNavController(requireView()).popBackStack();

                    } else {
                        Toast.makeText(requireContext(),
                                "Failed to track activity",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Map<String, Object> generateAdditionalMetrics(
            String activityType,
            int duration,
            int calories
    ) {
        Map<String, Object> metrics = new HashMap<>();
        Random random = new Random();

        switch (activityType) {

            case "RUNNING":
                double paceMinPerKm = FitnessMetricCalculator.randomInRange(4.5, 7.5); // realistic range
                double distanceKm = FitnessMetricCalculator.round(duration / paceMinPerKm, 2);
                double avgSpeed = FitnessMetricCalculator.round((distanceKm / duration) * 60, 2);
                int steps = (int) (distanceKm * 1300); // Avg 1300 steps per km

                metrics.put("distanceKm", distanceKm);
                metrics.put("avgSpeedKmh", avgSpeed);
                metrics.put("estimatedSteps", steps);
                metrics.put("paceMinPerKm", FitnessMetricCalculator.round(paceMinPerKm, 2));
                metrics.put("cadenceSpm", FitnessMetricCalculator.randomIntInRange(150, 180));
                break;

            case "WALKING":
                double paceWalk = FitnessMetricCalculator.randomInRange(10, 15); // min/km
                double walkDistance = FitnessMetricCalculator.round(duration / paceWalk, 2);
                int walkSteps = (int) (walkDistance * 1200); // avg 1200 steps/km

                metrics.put("distanceKm", walkDistance);
                metrics.put("estimatedSteps", walkSteps);
                metrics.put("paceMinPerKm", FitnessMetricCalculator.round(paceWalk, 2));
                metrics.put("intensityLevel", FitnessMetricCalculator.randomIntInRange(3, 6));
                break;

            case "CYCLING":
                double avgSpeedKmh = FitnessMetricCalculator.randomInRange(18, 28);
                double rideDistance = FitnessMetricCalculator.round((avgSpeedKmh * duration) / 60, 2);
                int avgPower = FitnessMetricCalculator.randomIntInRange(120, 240);

                metrics.put("distanceKm", rideDistance);
                metrics.put("avgSpeedKmh", avgSpeedKmh);
                metrics.put("estimatedPowerWatts", avgPower);
                metrics.put("cadenceRpm", FitnessMetricCalculator.randomIntInRange(70, 95));
                break;

            case "SWIMMING":
                int laps = duration * 2; // approx 2 laps per minute
                int strokeRate = FitnessMetricCalculator.randomIntInRange(30, 50);
                int distanceMeters = laps * 25;

                metrics.put("laps", laps);
                metrics.put("avgStrokeRate", strokeRate);
                metrics.put("distanceMeters", distanceMeters);
                metrics.put("efficiencyScore", FitnessMetricCalculator.randomIntInRange(60, 85));
                break;

            case "WEIGHT_LIFTING":
                int sets = FitnessMetricCalculator.randomIntInRange(3, 5);
                int repsPerSet = FitnessMetricCalculator.randomIntInRange(8, 12);
                int loadKg = FitnessMetricCalculator.randomIntInRange(40, 80);

                metrics.put("sets", sets);
                metrics.put("repsPerSet", repsPerSet);
                metrics.put("estimatedLoadKg", loadKg);
                metrics.put("totalVolumeKg", sets * repsPerSet * loadKg);
                break;

            case "CARDIO":
                int avgHr = FitnessMetricCalculator.randomIntInRange(120, 160);
                int intensity = FitnessMetricCalculator.randomIntInRange(6, 9);
                int vo2 = FitnessMetricCalculator.randomIntInRange(35, 50);

                metrics.put("avgHeartRate", avgHr);
                metrics.put("intensityScore", intensity);
                metrics.put("estimatedVo2Score", vo2);
                metrics.put("trainingZone",
                        avgHr > 150 ? "High" : avgHr > 130 ? "Moderate" : "Low"
                );
                break;

            case "BOXING":
                int punches = duration * FitnessMetricCalculator.randomIntInRange(12, 18);
                int rounds = Math.max(1, duration / 3);

                metrics.put("punchesThrown", punches);
                metrics.put("rounds", rounds);
                metrics.put("avgIntensity", FitnessMetricCalculator.randomIntInRange(7, 9));
                metrics.put("reactionScore", FitnessMetricCalculator.randomIntInRange(60, 85));
                break;

            case "YOGA":
            case "STRETCHING":
                metrics.put("flexibilityScore", FitnessMetricCalculator.randomIntInRange(60, 85));
                metrics.put("breathingScore", FitnessMetricCalculator.randomIntInRange(70, 90));
                metrics.put("mindfulnessScore", FitnessMetricCalculator.randomIntInRange(65, 90));
                metrics.put("calmnessLevel", FitnessMetricCalculator.randomIntInRange(6, 9));
                break;


            default:
                metrics.put("confidenceScore", 70 + random.nextInt(20));
                break;
        }

        return metrics;
    }
}
