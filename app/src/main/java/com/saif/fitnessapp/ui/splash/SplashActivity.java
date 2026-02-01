package com.saif.fitnessapp.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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
    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY_MS = 2000; // 2 seconds

    @Inject
    TokenManager tokenManager;

    @Inject
    AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Check authentication status after splash delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkAuthenticationStatus();
        }, SPLASH_DELAY_MS);
    }

    /**
     * UPDATED: Smart authentication check
     * - If user has refresh token → They're logged in
     * - If access token expired → Refresh it automatically
     * - If refresh fails → Redirect to login
     */
    private void checkAuthenticationStatus() {
        Log.d(TAG, "Checking authentication status...");

        if (!tokenManager.isLoggedIn()) {
            // No refresh token, user needs to login
            Log.d(TAG, "User not logged in (no refresh token)");
            navigateToLogin();
            return;
        }

        // User has refresh token (is logged in)
        if (tokenManager.hasValidAccessToken()) {
            // Access token is still valid, go directly to main
            Log.d(TAG, "User logged in with valid access token");
            navigateToMain();
            return;
        }

        // Access token expired, but we have refresh token
        // Refresh it in background before entering app
        Log.d(TAG, "User logged in but access token expired, refreshing...");
        refreshTokenAndNavigate();
    }

    /**
     * Refresh access token using refresh token
     * This happens automatically and transparently to the user
     */
    private void refreshTokenAndNavigate() {
        authManager.refreshAccessToken(new AuthManager.TokenRefreshCallback() {
            @Override
            public void onRefreshSuccess() {
                Log.d(TAG, "Token refresh successful, navigating to main");
                runOnUiThread(() -> navigateToMain());
            }

            @Override
            public void onRefreshFailed(String error) {
                Log.e(TAG, "Token refresh failed: " + error);
                Log.d(TAG, "User needs to login again");
                // Refresh token expired or invalid, user needs to login
                runOnUiThread(() -> navigateToLogin());
            }
        });
    }

    private void navigateToMain() {
        Log.d(TAG, "Navigating to MainActivity");
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Log.d(TAG, "Navigating to LoginActivity");
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}