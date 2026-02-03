package com.saif.fitnessapp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.saif.fitnessapp.R;
import com.saif.fitnessapp.auth.TokenManager;
import com.saif.fitnessapp.ui.TitleController;
import com.saif.fitnessapp.user.UserViewModel;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    @Inject
    TokenManager tokenManager;

    private UserViewModel userViewModel;
    private TextView welcomeText;
    private TextView userEmailText;
    private Button addActivityButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
                            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        welcomeText = view.findViewById(R.id.welcome_text);
        userEmailText = view.findViewById(R.id.user_email_text);
        addActivityButton = view.findViewById(R.id.add_activity_button);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        String userId = tokenManager.getUserId();
        if (userId != null) {
            loadUserProfile(userId);

            addActivityButton.setOnClickListener(v -> {
                Navigation.findNavController(v)
                        .navigate(R.id.action_home_to_addActivity);
            });

        }
    }

    private void loadUserProfile(String userId) {
        userViewModel.getUserProfile(userId).observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                welcomeText.setText("Welcome, " + user.getFirstName() + "!");
                userEmailText.setText(user.getEmail());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requireActivity() instanceof TitleController) {
            ((TitleController) requireActivity()).setTitle("Fitness Tracker");
        }
    }
}
