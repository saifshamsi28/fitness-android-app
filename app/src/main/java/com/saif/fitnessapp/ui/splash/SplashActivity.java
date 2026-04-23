package com.saif.fitnessapp.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

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
    private static final int SPLASH_DELAY_MS = 2200;

    @Inject
    TokenManager tokenManager;

    @Inject
    AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        animateSplash();

        new Handler(Looper.getMainLooper()).postDelayed(
                this::checkAuthenticationStatus, SPLASH_DELAY_MS);
    }

    private void animateSplash() {
        View logo    = findViewById(R.id.splash_logo);
        View title   = findViewById(R.id.splash_title);
        View tagline = findViewById(R.id.splash_tagline);
        View bottom  = findViewById(R.id.splash_bottom);

        // 1. Logo: pop-scale from 0.5 → 1 + fade in
        if (logo != null) {
            logo.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(650)
                    .setInterpolator(new OvershootInterpolator(1.1f))
                    .start();
        }

        // 2. App name: slide up + fade in
        if (title != null) {
            title.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(350)
                    .setDuration(450)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // 3. Tagline: gentle fade in
        if (tagline != null) {
            tagline.animate()
                    .alpha(1f)
                    .setStartDelay(600)
                    .setDuration(350)
                    .start();
        }

        // 4. Bottom bar: fade in last
        if (bottom != null) {
            bottom.animate()
                    .alpha(1f)
                    .setStartDelay(800)
                    .setDuration(400)
                    .start();
        }
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
                // If our tokens were NOT cleared it means the failure was a network
                // error (e.g. Keycloak on Render is cold-starting). The user is still
                // logically authenticated — let them into the app. API calls will
                // succeed once the server wakes up.
                if (tokenManager.isLoggedIn()) {
                    Log.w(TAG, "Network error during refresh but tokens intact — navigating to main");
                    runOnUiThread(() -> navigateToMain());
                } else {
                    Log.d(TAG, "Refresh token invalid/expired — user must re-login");
                    runOnUiThread(() -> navigateToLogin());
                }
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