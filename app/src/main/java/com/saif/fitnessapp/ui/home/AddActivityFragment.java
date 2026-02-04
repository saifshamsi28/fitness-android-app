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
                double distanceKm = round(duration * (0.12 + random.nextDouble() * 0.04), 2); // 0.12-0.16 km/min
                double avgSpeed = round((distanceKm / duration) * 60, 2); // km/hr
                int steps = random.nextInt(2000) + (duration * 120); // 120–140 steps/min approx

                metrics.put("distanceKm", distanceKm);
                metrics.put("avgSpeedKmh", avgSpeed);
                metrics.put("estimatedSteps", steps);
                metrics.put("cadenceSpm", 150 + random.nextInt(30)); // steps per minute
                break;

            case "WALKING":
                double walkDistance = round(duration * (0.06 + random.nextDouble() * 0.02), 2);
                int walkSteps = random.nextInt(1000) + (duration * 90);

                metrics.put("distanceKm", walkDistance);
                metrics.put("estimatedSteps", walkSteps);
                metrics.put("paceMinPerKm", round(duration / walkDistance, 2));
                break;

            case "CYCLING":
                double rideDistance = round(duration * (0.25 + random.nextDouble() * 0.15), 2);
                int avgPower = 120 + random.nextInt(120); // 120–240 watts

                metrics.put("distanceKm", rideDistance);
                metrics.put("avgSpeedKmh", round((rideDistance / duration) * 60, 2));
                metrics.put("estimatedPowerWatts", avgPower);
                break;

            case "SWIMMING":
                int laps = random.nextInt(10) + (duration / 2);
                metrics.put("laps", laps);
                metrics.put("avgStrokeRate", 30 + random.nextInt(20));
                metrics.put("distanceMeters", laps * 25);
                break;

            case "WEIGHT_LIFTING":
                metrics.put("sets", 3 + random.nextInt(3));      // 3–5
                metrics.put("repsPerSet", 8 + random.nextInt(5)); // 8–12
                metrics.put("estimatedLoadKg", 40 + random.nextInt(40)); // 40–80 kg
                break;

            case "CARDIO":
                metrics.put("avgHeartRate", 120 + random.nextInt(40));
                metrics.put("intensityScore", 6 + random.nextInt(4));
                metrics.put("estimatedVo2Score", 35 + random.nextInt(15));
                break;

            case "BOXING":
                metrics.put("punchesThrown", random.nextInt(200) + (duration * 10));
                metrics.put("rounds", 1 + random.nextInt(3));
                metrics.put("avgIntensity", 7 + random.nextInt(3));
                break;

            case "STRETCHING":
            case "YOGA":
                metrics.put("flexibilityScore", 60 + random.nextInt(25));
                metrics.put("breathingScore", 70 + random.nextInt(20));
                metrics.put("mindfulnessScore", 65 + random.nextInt(25));
                break;

            default:
                metrics.put("confidenceScore", 70 + random.nextInt(20));
                break;
        }

        return metrics;
    }

    private double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }



}
