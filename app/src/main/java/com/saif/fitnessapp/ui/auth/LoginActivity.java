package com.saif.fitnessapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.saif.fitnessapp.MainActivity;
import com.saif.fitnessapp.R;
import com.saif.fitnessapp.auth.AuthManager;
import com.saif.fitnessapp.auth.TokenManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    @Inject AuthManager authManager;
    @Inject TokenManager tokenManager;

    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private CheckBox rememberMeCheckbox;
    private MaterialButton loginButton;
    private TextView signupButton;
    private TextView forgotPasswordButton;
    private TextView errorMessage;
    private FrameLayout loadingOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (tokenManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        emailLayout        = findViewById(R.id.email_layout);
        passwordLayout     = findViewById(R.id.password_layout);
        emailInput         = findViewById(R.id.email_input);
        passwordInput      = findViewById(R.id.password_input);
        rememberMeCheckbox = findViewById(R.id.remember_me_checkbox);
        loginButton        = findViewById(R.id.login_button);
        signupButton       = findViewById(R.id.signup_button);
        forgotPasswordButton = findViewById(R.id.forgot_password_button);
        errorMessage       = findViewById(R.id.error_message);
        loadingOverlay     = findViewById(R.id.loading_overlay);

        if (tokenManager.isRememberMe()) {
            emailInput.setText(tokenManager.getSavedEmail());
            passwordInput.setText(tokenManager.getSavedPassword());
            rememberMeCheckbox.setChecked(true);
        }

        Intent incoming = getIntent();
        if (incoming != null) {
            String prefillEmail = incoming.getStringExtra("EMAIL");
            if (prefillEmail != null && !prefillEmail.isEmpty()) {
                emailInput.setText(prefillEmail);
            }
        }

        loginButton.setOnClickListener(v -> attemptLogin());
        passwordInput.setOnEditorActionListener((v, actionId, event) -> { attemptLogin(); return true; });
        signupButton.setOnClickListener(v -> startActivity(new Intent(this, SignupActivity.class)));
        forgotPasswordButton.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void attemptLogin() {
        clearErrors();
        hideKeyboard();

        String email    = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString() : "";

        if (!validateInputs(email, password)) return;

        setLoading(true);

        authManager.loginWithCredentials(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess() {
                if (rememberMeCheckbox.isChecked()) {
                    tokenManager.saveCredentials(email, password);
                } else {
                    tokenManager.clearSavedCredentials();
                }
                runOnUiThread(() -> { setLoading(false); navigateToMain(); });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Login error: " + error);
                runOnUiThread(() -> { setLoading(false); showError(friendlyError(error)); });
            }
        });
    }

    private boolean validateInputs(String email, String password) {
        boolean valid = true;
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email or username is required"); valid = false;
        }
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required"); valid = false;
        }
        return valid;
    }

    private void clearErrors() {
        emailLayout.setError(null);
        passwordLayout.setError(null);
        errorMessage.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        errorMessage.setText(msg);
        errorMessage.setVisibility(View.VISIBLE);
        errorMessage.animate().translationX(-8f).setDuration(50)
                .withEndAction(() -> errorMessage.animate().translationX(8f).setDuration(50)
                        .withEndAction(() -> errorMessage.animate().translationX(0f).setDuration(50).start()).start()).start();
    }

    private void setLoading(boolean loading) {
        loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading);
        emailInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
        rememberMeCheckbox.setEnabled(!loading);
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    private String friendlyError(String error) {
        if (error == null) return "Login failed. Please try again.";
        String lower = error.toLowerCase();
        if (lower.contains("credentials") || lower.contains("invalid_grant") || lower.contains("invalid user")) {
            return "Incorrect username/email or password. Please try again.";
        }
        if (lower.contains("network") || lower.contains("connect")) {
            return "Network error. Check your internet connection.";
        }
        if (lower.contains("disabled")) return "Your account has been disabled.";
        if (lower.contains("locked")) return "Account temporarily locked. Try again later.";
        return "Login failed. Please try again.";
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
