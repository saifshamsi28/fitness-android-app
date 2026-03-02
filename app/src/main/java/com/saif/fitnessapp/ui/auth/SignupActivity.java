package com.saif.fitnessapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

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

/**
 * Signup flow — 2 steps:
 *   1. Fill form → validate → POST /api/auth/signup/send-otp
 *   2. Enter OTP → POST /api/auth/signup/verify-otp → POST /api/auth/signup (create account)
 */
@AndroidEntryPoint
public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";

    // Step panels
    private LinearLayout stepFormPanel;
    private LinearLayout stepOtpPanel;

    // Step 1 — form
    private TextInputEditText firstNameInput;
    private TextInputEditText lastNameInput;
    private TextInputEditText usernameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputLayout   firstNameLayout;
    private TextInputLayout   lastNameLayout;
    private TextInputLayout   usernameLayout;
    private TextInputLayout   emailLayout;
    private TextInputLayout   passwordLayout;
    private Button            signupButton;      // "Continue" in step 1

    // Step 2 — OTP
    private TextInputLayout   otpLayout;
    private TextInputEditText otpInput;
    private Button            verifyOtpButton;
    private TextView          resendOtpText;
    private TextView          changeEmailText;
    private TextView          otpSentHint;

    // Shared
    private TextView    errorMessage;
    private TextView    loginLink;
    private FrameLayout loadingOverlay;

    @Inject ApiService  apiService;
    @Inject AuthManager authManager;

    // Captured form values held between steps
    private String pendingEmail;
    private String pendingFirstName;
    private String pendingLastName;
    private String pendingPassword;
    private String pendingUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        bindViews();
        setupListeners();
        showStep(1);
    }

    private void bindViews() {
        stepFormPanel = findViewById(R.id.step_form_panel);
        stepOtpPanel  = findViewById(R.id.step_otp_panel);

        firstNameInput  = findViewById(R.id.first_name_input);
        lastNameInput   = findViewById(R.id.last_name_input);
        usernameInput   = findViewById(R.id.username_input);
        emailInput      = findViewById(R.id.email_input);
        passwordInput   = findViewById(R.id.password_input);
        firstNameLayout = findViewById(R.id.first_name_layout);
        lastNameLayout  = findViewById(R.id.last_name_layout);
        usernameLayout  = findViewById(R.id.username_layout);
        emailLayout     = findViewById(R.id.email_layout);
        passwordLayout  = findViewById(R.id.password_layout);
        signupButton    = findViewById(R.id.signup_button);

        otpLayout       = findViewById(R.id.otp_layout);
        otpInput        = findViewById(R.id.otp_input);
        verifyOtpButton = findViewById(R.id.verify_otp_button);
        resendOtpText   = findViewById(R.id.resend_otp_text);
        changeEmailText = findViewById(R.id.change_email_text);
        otpSentHint     = findViewById(R.id.otp_sent_hint);

        errorMessage    = findViewById(R.id.error_message);
        loginLink       = findViewById(R.id.login_link);
        loadingOverlay  = findViewById(R.id.signup_loading_overlay);
    }

    private void setupListeners() {
        signupButton.setOnClickListener(v -> attemptContinue());
        verifyOtpButton.setOnClickListener(v -> attemptVerifyAndCreate());
        resendOtpText.setOnClickListener(v -> resendOtp());
        changeEmailText.setOnClickListener(v -> showStep(1));
        loginLink.setOnClickListener(v -> finish());
    }

    // ─────────────────────────── STEP 1 ───────────────────────────

    private void attemptContinue() {
        clearErrors();

        String firstName = text(firstNameInput);
        String lastName  = text(lastNameInput);
        String username  = text(usernameInput);
        String email     = text(emailInput);
        String password  = passwordInput.getText() != null ? passwordInput.getText().toString() : "";

        if (!validateForm(firstName, lastName, username, email, password)) return;

        pendingFirstName = firstName;
        pendingLastName  = lastName;
        pendingUsername  = username;
        pendingEmail     = email;
        pendingPassword  = password;

        setLoading(true, "Sending OTP…");

        authManager.sendSignupOtp(email, firstName, username, new AuthManager.AuthCallback() {
            @Override public void onSuccess() {
                runOnUiThread(() -> { setLoading(false, null); showStep(2); });
            }
            @Override public void onError(String error) {
                runOnUiThread(() -> { setLoading(false, null); showError(error); });
            }
        });
    }

    private void resendOtp() {
        if (pendingEmail == null) return;
        setLoading(true, "Resending OTP…");
        authManager.sendSignupOtp(pendingEmail, pendingFirstName, pendingUsername, new AuthManager.AuthCallback() {
            @Override public void onSuccess() {
                runOnUiThread(() -> { setLoading(false, null); showError("OTP resent!"); });
            }
            @Override public void onError(String error) {
                runOnUiThread(() -> { setLoading(false, null); showError(error); });
            }
        });
    }

    // ─────────────────────────── STEP 2 ───────────────────────────

    private void attemptVerifyAndCreate() {
        clearErrors();

        String otp = text(otpInput);
        if (TextUtils.isEmpty(otp) || otp.length() != 6) {
            otpLayout.setError("Enter the 6-digit OTP"); return;
        }
        otpLayout.setError(null);

        setLoading(true, "Verifying OTP…");

        authManager.verifySignupOtp(pendingEmail, otp, new AuthManager.AuthCallback() {
            @Override public void onSuccess() {
                runOnUiThread(() -> { setLoading(false, null); createAccount(); });
            }
            @Override public void onError(String error) {
                runOnUiThread(() -> { setLoading(false, null); showError(error); });
            }
        });
    }

    private void createAccount() {
        setLoading(true, "Creating account…");
        SignupRequest request = new SignupRequest(pendingEmail, pendingPassword,
                pendingFirstName, pendingLastName, pendingUsername);

        apiService.signup(request).enqueue(new Callback<SignupResponse>() {
            @Override
            public void onResponse(Call<SignupResponse> call, Response<SignupResponse> response) {
                setLoading(false, null);
                Log.d(TAG, "Signup response: " + response.code());

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    handleSignupSuccess(pendingEmail);
                } else if (response.body() != null) {
                    showError(response.body().getMessage());
                } else {
                    showError(errorMsgByCode(response.code()));
                }
            }

            @Override
            public void onFailure(Call<SignupResponse> call, Throwable t) {
                setLoading(false, null);
                Log.e(TAG, "Signup failure: " + t.getMessage(), t);
                showError("Network error. Please check your connection.");
            }
        });
    }

    private void handleSignupSuccess(String email) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("AUTO_LOGIN", true);
        intent.putExtra("EMAIL", email);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ─────────────────────────── VALIDATION ───────────────────────────

    private boolean validateForm(String firstName, String lastName,
                                 String username, String email, String password) {
        boolean valid = true;
        if (TextUtils.isEmpty(firstName) || firstName.length() < 2) {
            firstNameLayout.setError("First name must be at least 2 characters"); valid = false;
        }
        if (TextUtils.isEmpty(lastName) || lastName.length() < 2) {
            lastNameLayout.setError("Last name must be at least 2 characters"); valid = false;
        }
        if (TextUtils.isEmpty(username) || username.length() < 3) {
            usernameLayout.setError("Username must be at least 3 characters"); valid = false;
        } else if (username.length() > 30) {
            usernameLayout.setError("Username must be at most 30 characters"); valid = false;
        } else if (!username.matches("^[a-zA-Z0-9_]+$")) {
            usernameLayout.setError("Only letters, numbers and underscores allowed"); valid = false;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Enter a valid email address"); valid = false;
        }
        if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
            passwordLayout.setError("Min 8 chars with uppercase, lowercase, number and special character");
            valid = false;
        }
        return valid;
    }

    private boolean isPasswordValid(String password) {
        if (password.length() < 8) return false;
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if ("@#$%!&*".indexOf(c) >= 0) hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    // ─────────────────────────── UI HELPERS ───────────────────────────

    private void showStep(int step) {
        clearErrors();
        stepFormPanel.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        stepOtpPanel.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        if (step == 2) {
            otpSentHint.setText("We sent a 6-digit code to\n" + pendingEmail);
            otpInput.setText("");
            otpInput.requestFocus();
        }
    }

    private void clearErrors() {
        firstNameLayout.setError(null);
        lastNameLayout.setError(null);
        usernameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        if (otpLayout != null) otpLayout.setError(null);
        errorMessage.setVisibility(View.GONE);
    }

    private void showError(String message) {
        errorMessage.setText(message != null ? message : "Something went wrong. Please try again.");
        errorMessage.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading, String msg) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (loading && msg != null) {
                TextView t = loadingOverlay.findViewById(R.id.signup_loading_text);
                if (t != null) t.setText(msg);
            }
        }
        signupButton.setEnabled(!loading);
        verifyOtpButton.setEnabled(!loading);
    }

    private String text(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private String errorMsgByCode(int code) {
        switch (code) {
            case 409: return "Email already registered. Please login.";
            case 400: return "Invalid input. Please check your details.";
            case 500: return "Server error. Please try again later.";
            default:  return "Signup failed. Please try again.";
        }
    }
}