package com.saif.fitnessapp.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.saif.fitnessapp.MainActivity;
import com.saif.fitnessapp.R;
import com.saif.fitnessapp.auth.AuthManager;
import com.saif.fitnessapp.auth.TokenManager;
import com.saif.fitnessapp.ui.auth.LoginActivity;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    @Inject
    TokenManager tokenManager;

    @Inject
    AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Check if user is already logged in
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (tokenManager.isLoggedIn()) {
                // User is logged in, go to main activity
                navigateToMain();
            } else {
                // User is not logged in, go to login
                navigateToLogin();
            }
        }, 2000); // 2 second splash screen delay
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
