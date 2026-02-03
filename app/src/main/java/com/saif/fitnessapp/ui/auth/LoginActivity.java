package com.saif.fitnessapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.saif.fitnessapp.MainActivity;
import com.saif.fitnessapp.R;
import com.saif.fitnessapp.auth.AuthManager;
import com.saif.fitnessapp.auth.TokenManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_AUTH = 100;

    @Inject
    AuthManager authManager;

    @Inject
    TokenManager tokenManager;

    private Button loginButton;
    private Button signupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate called");

        // Check if already logged in
        if (tokenManager.isLoggedIn()) {
            Log.d(TAG, "User already logged in, navigating to MainActivity");
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        // Initialize views
        loginButton = findViewById(R.id.login_button);
        signupButton = findViewById(R.id.signup_button);

        // Setup click listeners
        loginButton.setOnClickListener(v -> {
            Log.d(TAG, "Login button clicked");
            authManager.startLogin(this);
        });

        signupButton.setOnClickListener(v -> {
            Log.d(TAG, "Signup button clicked");
            navigateToSignup();
        });

        // Handle auto-login after signup
        handleAutoLogin();

        // Handle the intent if this activity was launched with intent data
        handleIntent(getIntent());
    }

    private void navigateToSignup() {
        Intent intent = new Intent(this, SignupActivity.class);
        startActivity(intent);
    }

    /**
     * Handle auto-login after successful signup
     * SignupActivity will pass AUTO_LOGIN flag
     */
    private void handleAutoLogin() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("AUTO_LOGIN", false)) {
            String email = intent.getStringExtra("EMAIL");
            Log.d(TAG, "Auto-login requested for: " + email);
            Toast.makeText(this, "Please login with your new account", Toast.LENGTH_LONG).show();

            // Automatically trigger login
            // User will need to enter credentials in Keycloak login page
            authManager.startLogin(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent called");
        setIntent(intent); // IMPORTANT: Update the activity's intent
        handleIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult - requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (requestCode == RC_AUTH) {
            if (data != null) {
                handleAuthResponse(data);
            } else {
                Log.e(TAG, "onActivityResult - data is null");
                Toast.makeText(this, "Login cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            Log.d(TAG, "handleIntent - intent is null");
            return;
        }

        if (intent.getData() == null) {
            Log.d(TAG, "No intent data");
            return; // Normal launch, not a redirect
        }

        Log.d(TAG, "handleIntent - has data: " + intent.getData());
        handleAuthResponse(intent);
    }

    private void handleAuthResponse(Intent intent) {
        Log.d(TAG, "handleAuthResponse called");

        authManager.handleAuthResponse(intent, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Login successful");
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Login failed: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_LONG).show();
                });
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

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");

        // Check again in case user logged in elsewhere
        if (tokenManager.isLoggedIn() && !isFinishing()) {
            Log.d(TAG, "User logged in during resume, navigating to MainActivity");
            navigateToMain();
        }
    }
}