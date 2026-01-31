package com.saif.fitnessapp.ui.home;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.saif.fitnessapp.R;
import com.saif.fitnessapp.activity.ActivityViewModel;
import com.saif.fitnessapp.auth.TokenManager;
import com.saif.fitnessapp.network.dto.ActivityRequest;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@AndroidEntryPoint
public class AddActivityFragment extends Fragment {

    @Inject
    TokenManager tokenManager;

    private ActivityViewModel activityViewModel;
    private Spinner activityTypeSpinner;
    private EditText durationInput;
    private EditText caloriesInput;
    private Button submitButton;

    private static final String[] ACTIVITY_TYPES = {
            "RUNNING", "SWIMMING", "WALKING", "BOXING", 
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
        String durationStr = durationInput.getText().toString();
        String caloriesStr = caloriesInput.getText().toString();

        if (durationStr.isEmpty() || caloriesStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int duration = Integer.parseInt(durationStr);
            int calories = Integer.parseInt(caloriesStr);

            ActivityRequest request = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                request = new ActivityRequest(
                        userId,
                        activityType,
                        duration,
                        calories,
                        LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                        null
                );
            }

            activityViewModel.trackActivity(request).observe(getViewLifecycleOwner(), response -> {
                if (response != null) {
                    Toast.makeText(requireContext(), "Activity tracked successfully!", 
                            Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                } else {
                    Toast.makeText(requireContext(), "Failed to track activity", 
                            Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid input", Toast.LENGTH_SHORT).show();
        }
    }
}
