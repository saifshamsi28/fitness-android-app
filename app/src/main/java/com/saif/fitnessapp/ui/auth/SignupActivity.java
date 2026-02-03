package com.saif.fitnessapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.saif.fitnessapp.R;
import com.saif.fitnessapp.auth.AuthManager;
import com.saif.fitnessapp.network.ApiService;
import com.saif.fitnessapp.network.dto.SignupRequest;
import com.saif.fitnessapp.network.dto.SignupResponse;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";

    // UI Components
    private TextInputEditText firstNameInput;
    private TextInputEditText lastNameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputLayout firstNameLayout;
    private TextInputLayout lastNameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private Button signupButton;
    private ProgressBar loadingProgress;
    private TextView errorMessage;
    private TextView loginLink;

    @Inject
    ApiService apiService;

    @Inject
    AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        firstNameInput = findViewById(R.id.first_name_input);
        lastNameInput = findViewById(R.id.last_name_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);

        firstNameLayout = findViewById(R.id.first_name_layout);
        lastNameLayout = findViewById(R.id.last_name_layout);
        emailLayout = findViewById(R.id.email_layout);
        passwordLayout = findViewById(R.id.password_layout);

        signupButton = findViewById(R.id.signup_button);
        loadingProgress = findViewById(R.id.loading_progress);
        errorMessage = findViewById(R.id.error_message);
        loginLink = findViewById(R.id.login_link);
    }

    private void setupClickListeners() {
        signupButton.setOnClickListener(v -> attemptSignup());

        loginLink.setOnClickListener(v -> {
            // Navigate back to login
            finish();
        });
    }

    private void attemptSignup() {
        // Clear previous errors
        clearErrors();

        // Get input values
        String firstName = firstNameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        // Validate inputs
        if (!validateInputs(firstName, lastName, email, password)) {
            return;
        }

        // Show loading
        setLoading(true);

        // Create signup request
        SignupRequest request = new SignupRequest(email, password, firstName, lastName);

        // Call API
        apiService.signup(request).enqueue(new Callback<SignupResponse>() {
            @Override
            public void onResponse(Call<SignupResponse> call, Response<SignupResponse> response) {
                setLoading(false);
                Log.d(TAG, "Signup response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    SignupResponse signupResponse = response.body();

                    if (signupResponse.isSuccess()) {
                        Log.d(TAG, "Signup successful for: " + signupResponse.getEmail());
                        handleSignupSuccess(email);
                    } else {
                        // Response is 200 OK but success=false in body
                        Log.w(TAG, "Signup failed: " + signupResponse.getMessage());
                        showError(signupResponse.getMessage());
                    }
                } else {
                    // HTTP error codes (400, 409, 500, etc.)
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<SignupResponse> call, Throwable t) {
                setLoading(false);
                Log.e(TAG, "Signup network failure: " + t.getMessage(), t);
                showError("Network error. Please check your connection and try again.");
            }
        });
    }

    private boolean validateInputs(String firstName, String lastName, String email, String password) {
        boolean valid = true;

        // Validate first name
        if (TextUtils.isEmpty(firstName)) {
            firstNameLayout.setError("First name is required");
            valid = false;
        } else if (firstName.length() < 2) {
            firstNameLayout.setError("First name must be at least 2 characters");
            valid = false;
        }

        // Validate last name
        if (TextUtils.isEmpty(lastName)) {
            lastNameLayout.setError("Last name is required");
            valid = false;
        } else if (lastName.length() < 2) {
            lastNameLayout.setError("Last name must be at least 2 characters");
            valid = false;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Invalid email format");
            valid = false;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required");
            valid = false;
        } else if (!isPasswordValid(password)) {
            passwordLayout.setError("Password must be at least 8 characters with uppercase, lowercase, number, and special character");
            valid = false;
        }

        return valid;
    }

    private boolean isPasswordValid(String password) {
        if (password.length() < 8) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if ("@#$%!&*".indexOf(c) >= 0) hasSpecial = true;
        }

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    private void handleSignupSuccess(String email) {
        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();

        // Auto-login the user
        Log.d(TAG, "Auto-logging in user: " + email);

        // Navigate to LoginActivity and trigger automatic login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("AUTO_LOGIN", true);
        intent.putExtra("EMAIL", email);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleErrorResponse(Response<SignupResponse> response) {
        String errorMsg;

        int statusCode = response.code();
        Log.d(TAG, "Error status code: " + statusCode);

        try {
            // Try to parse error body first
            if (response.body() != null && response.body().getMessage() != null) {
                // Backend returned error in SignupResponse format
                errorMsg = response.body().getMessage();
                Log.d(TAG, "Error from response body: " + errorMsg);

            } else if (response.errorBody() != null) {
                // Parse error body JSON
                String errorBodyString = response.errorBody().string();
                Log.d(TAG, "Error body: " + errorBodyString);

                // Try to parse as SignupResponse
                try {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    SignupResponse errorResponse = gson.fromJson(errorBodyString, SignupResponse.class);

                    if (errorResponse != null && errorResponse.getMessage() != null) {
                        errorMsg = errorResponse.getMessage();
                    } else {
                        errorMsg = getErrorMessageByStatusCode(statusCode, errorBodyString);
                    }
                } catch (Exception e) {
                    errorMsg = getErrorMessageByStatusCode(statusCode, errorBodyString);
                }

            } else {
                // No error body, use status code
                errorMsg = getErrorMessageByStatusCode(statusCode, null);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing error response", e);
            errorMsg = getErrorMessageByStatusCode(statusCode, null);
        }

        showError(errorMsg);
    }

    private String getErrorMessageByStatusCode(int statusCode, String errorBody) {
        switch (statusCode) {
            case 409: // Conflict
                return "Email already registered. Please login.";

            case 400: // Bad Request
                // Check error body for specific error
                if (errorBody != null) {
                    if (errorBody.contains("password")) {
                        return "Password does not meet requirements.";
                    } else if (errorBody.contains("email")) {
                        return "Invalid email address.";
                    } else if (errorBody.contains("already registered")) {
                        return "Email already registered. Please login.";
                    }
                }
                return "Invalid input. Please check your details.";

            case 500: // Internal Server Error
                return "Server error. Please try again later.";

            case 503: // Service Unavailable
                return "Service temporarily unavailable. Please try again later.";

            default:
                return "Signup failed. Please try again.";
        }
    }

    private void clearErrors() {
        firstNameLayout.setError(null);
        lastNameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        errorMessage.setVisibility(View.GONE);
    }

    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        if (loading) {
            signupButton.setEnabled(false);
            signupButton.setText("");
            loadingProgress.setVisibility(View.VISIBLE);
        } else {
            signupButton.setEnabled(true);
            signupButton.setText("Create Account");
            loadingProgress.setVisibility(View.GONE);
        }
    }
}