package com.saif.fitnessapp.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.saif.fitnessapp.R;
import com.saif.fitnessapp.auth.AuthManager;
import com.saif.fitnessapp.auth.TokenManager;
import com.saif.fitnessapp.ui.auth.LoginActivity;
import com.saif.fitnessapp.user.UserViewModel;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    @Inject
    TokenManager tokenManager;

    @Inject
    AuthManager authManager;

    private UserViewModel userViewModel;
    private TextView nameText;
    private TextView emailText;
    private TextView createdAtText;
    private Button logoutButton;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
                            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nameText = view.findViewById(R.id.name_text);
        emailText = view.findViewById(R.id.email_text);
        createdAtText = view.findViewById(R.id.created_at_text);
        logoutButton = view.findViewById(R.id.logout_button);
        progressBar = view.findViewById(R.id.progress_bar);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        logoutButton.setOnClickListener(v -> logout());

        String userId = tokenManager.getUserId();
        if (userId != null) {
            loadUserProfile(userId);
        }
    }

    private void loadUserProfile(String userId) {
        progressBar.setVisibility(View.VISIBLE);
        userViewModel.getUserProfile(userId).observe(getViewLifecycleOwner(), user -> {
            progressBar.setVisibility(View.GONE);
            if (user != null) {
                nameText.setText(user.getFirstName() + " " + user.getLastName());
                emailText.setText(user.getEmail());
                createdAtText.setText("Member since: " + user.getCreatedAt());
            }
        });
    }

    private void logout() {
        tokenManager.clearTokens();
        authManager.logout();
        startActivity(new Intent(requireContext(), LoginActivity.class));
        requireActivity().finish();
    }
}
