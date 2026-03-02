package com.saif.fitnessapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.saif.fitnessapp.R;
import com.saif.fitnessapp.auth.AuthManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Forgot-password flow — 3 steps:
 *   1. Enter email  → POST /api/auth/forgot-password/send-otp
 *   2. Enter OTP    → POST /api/auth/forgot-password/verify-otp  (receives resetToken)
 *   3. New password → POST /api/auth/forgot-password/reset       (uses resetToken)
 */
@AndroidEntryPoint
public class ForgotPasswordActivity extends AppCompatActivity {

    @Inject AuthManager authManager;

    // Step panels
    private LinearLayout stepEmailPanel;
    private LinearLayout stepOtpPanel;
    private LinearLayout stepResetPanel;
    private LinearLayout successPanel;

    // Step title / subtitle
    private TextView stepTitle;
    private TextView stepSubtitle;

    // Step 1 views
    private TextInputLayout  emailLayout;
    private TextInputEditText emailInput;
    private MaterialButton   sendOtpButton;

    // Step 2 views
    private TextInputLayout  otpLayout;
    private TextInputEditText otpInput;
    private MaterialButton   verifyOtpButton;
    private TextView         resendOtpText;

    // Step 3 views
    private TextInputLayout  newPasswordLayout;
    private TextInputEditText newPasswordInput;
    private TextInputLayout  confirmPasswordLayout;
    private TextInputEditText confirmPasswordInput;
    private MaterialButton   resetPasswordButton;

    // Shared
    private TextView    errorMessage;
    private FrameLayout loadingOverlay;
    private TextView    backToLoginButton;

    // State
    private String         currentEmail;
    private String         resetToken;
    /** Live resend countdown timer — cancelled on activity destroy. */
    private CountDownTimer resendCountdown;
    /** OTP send-quota from the server — shown in step 2 subtitle. */
    private int            otpSendCount = 1;
    private int            otpMaxSends  = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        bindViews();
        setupListeners();
        showStep(1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendCountdown != null) resendCountdown.cancel();
    }

    private void bindViews() {
        stepEmailPanel   = findViewById(R.id.step_email_panel);
        stepOtpPanel     = findViewById(R.id.step_otp_panel);
        stepResetPanel   = findViewById(R.id.step_reset_panel);
        successPanel     = findViewById(R.id.success_panel);

        stepTitle        = findViewById(R.id.step_title);
        stepSubtitle     = findViewById(R.id.step_subtitle);

        emailLayout      = findViewById(R.id.reset_email_layout);
        emailInput       = findViewById(R.id.reset_email_input);
        sendOtpButton    = findViewById(R.id.send_otp_button);

        otpLayout        = findViewById(R.id.otp_layout);
        otpInput         = findViewById(R.id.otp_input);
        verifyOtpButton  = findViewById(R.id.verify_otp_button);
        resendOtpText    = findViewById(R.id.resend_otp_text);

        newPasswordLayout     = findViewById(R.id.new_password_layout);
        newPasswordInput      = findViewById(R.id.new_password_input);
        confirmPasswordLayout = findViewById(R.id.confirm_password_layout);
        confirmPasswordInput  = findViewById(R.id.confirm_password_input);
        resetPasswordButton   = findViewById(R.id.reset_password_button);

        errorMessage      = findViewById(R.id.reset_error_message);
        loadingOverlay    = findViewById(R.id.reset_loading_overlay);
        backToLoginButton = findViewById(R.id.back_to_login_button);

        findViewById(R.id.back_button).setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        sendOtpButton.setOnClickListener(v -> attemptSendOtp());
        verifyOtpButton.setOnClickListener(v -> attemptVerifyOtp());
        resetPasswordButton.setOnClickListener(v -> attemptResetPassword());
        resendOtpText.setOnClickListener(v -> resendOtp());
        backToLoginButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    // ─────────────────────────── STEP 1 ───────────────────────────

    private void attemptSendOtp() {
        clearError();
        hideKeyboard();

        String email = text(emailInput);
        if (TextUtils.isEmpty(email)) { emailLayout.setError("Email is required"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Enter a valid email address"); return;
        }
        emailLayout.setError(null);

        currentEmail = email;
        setLoading(true, "Sending OTP…");

        authManager.sendForgotPasswordOtp(email, new AuthManager.OtpSendCallback() {
            @Override public void onOtpSent(int sendCount, int maxSends) {
                otpSendCount = sendCount;
                otpMaxSends  = maxSends;
                runOnUiThread(() -> {
                    setLoading(false, null);
                    showStep(2);                    // showStep(2) reads otpSendCount/otpMaxSends
                    startResendCountdown(60_000L);  // 60s cooldown after each send
                });
            }
            @Override public void onError(String message, int sendCount, int maxSends, long retryAfterSeconds) {
                runOnUiThread(() -> { setLoading(false, null); showError(message); });
            }
        });
    }

    /**
     * Starts / restarts the resend cooldown timer.
     *
     * @param durationMs countdown duration in milliseconds;
     *                   normally 60 000 ms (60 s) between sends,
     *                   or server-provided {@code retryAfterSeconds * 1000} on a cooldown error.
     */
    private void startResendCountdown(long durationMs) {
        if (resendCountdown != null) resendCountdown.cancel();
        resendOtpText.setEnabled(false);
        resendCountdown = new CountDownTimer(durationMs, 1_000L) {
            @Override public void onTick(long ms) {
                runOnUiThread(() -> resendOtpText.setText("Resend in " + (ms / 1000L) + "s"));
            }
            @Override public void onFinish() {
                runOnUiThread(() -> {
                    resendOtpText.setEnabled(true);
                    resendOtpText.setText("Resend OTP");
                });
            }
        }.start();
    }

    private void resendOtp() {
        if (currentEmail == null) return;
        clearError();
        resendOtpText.setEnabled(false);
        resendOtpText.setText("Sending…");
        authManager.sendForgotPasswordOtp(currentEmail, new AuthManager.OtpSendCallback() {
            @Override public void onOtpSent(int sendCount, int maxSends) {
                otpSendCount = sendCount;
                otpMaxSends  = maxSends;
                runOnUiThread(() -> {
                    startResendCountdown(60_000L);
                    // Update subtitle with fresh count
                    stepSubtitle.setText("We sent a 6-digit code to\n" + currentEmail
                            + "\n(" + otpSendCount + "/" + otpMaxSends + " OTP requests used)");
                    showInfo("OTP resent to " + currentEmail);
                });
            }
            @Override public void onError(String message, int sendCount, int maxSends, long retryAfterSeconds) {
                runOnUiThread(() -> {
                    showError(message);
                    if (retryAfterSeconds > 0) {
                        // Server told us exactly how long to wait — start countdown with that duration
                        startResendCountdown(retryAfterSeconds * 1_000L);
                    } else {
                        resendOtpText.setEnabled(true);
                        resendOtpText.setText("Resend OTP");
                    }
                    if (sendCount > 0) {
                        otpSendCount = sendCount;
                        otpMaxSends  = maxSends;
                        stepSubtitle.setText("We sent a 6-digit code to\n" + currentEmail
                                + "\n(" + otpSendCount + "/" + otpMaxSends + " OTP requests used)");
                    }
                });
            }
        });
    }

    // ─────────────────────────── STEP 2 ───────────────────────────

    private void attemptVerifyOtp() {
        clearError();
        hideKeyboard();

        String otp = text(otpInput);
        if (TextUtils.isEmpty(otp) || otp.length() != 6) {
            otpLayout.setError("Enter the 6-digit OTP"); return;
        }
        otpLayout.setError(null);

        setLoading(true, "Verifying OTP…");

        authManager.verifyForgotPasswordOtp(currentEmail, otp, new AuthManager.OtpVerifyCallback() {
            @Override public void onSuccess(String token) {
                resetToken = token;
                runOnUiThread(() -> {
                    setLoading(false, null);
                    if (resendCountdown != null) resendCountdown.cancel();
                    showStep(3);
                });
            }
            @Override public void onError(String error) {
                runOnUiThread(() -> {
                    setLoading(false, null);
                    // Server includes attempts info in the message, e.g.:
                    //   "Incorrect OTP. 3/5 attempts remaining."
                    //   "Too many incorrect attempts. Please request a new OTP."
                    //   "OTP has expired. Please request a new one."
                    showError(error);
                    // If all attempts used — disable verify so user must resend
                    if (error != null && (error.contains("Too many") || error.contains("0/5")
                            || error.contains("expired"))) {
                        verifyOtpButton.setEnabled(false);
                    }
                });
            }
        });
    }

    // ─────────────────────────── STEP 3 ───────────────────────────

    private void attemptResetPassword() {
        clearError();
        hideKeyboard();

        String newPwd     = text(newPasswordInput);
        String confirmPwd = text(confirmPasswordInput);

        boolean valid = true;
        if (TextUtils.isEmpty(newPwd) || newPwd.length() < 8) {
            newPasswordLayout.setError("Min 8 characters required"); valid = false;
        } else { newPasswordLayout.setError(null); }

        if (TextUtils.isEmpty(confirmPwd)) {
            confirmPasswordLayout.setError("Please confirm your password"); valid = false;
        } else if (!newPwd.equals(confirmPwd)) {
            confirmPasswordLayout.setError("Passwords do not match"); valid = false;
        } else { confirmPasswordLayout.setError(null); }

        if (!valid) return;

        setLoading(true, "Resetting password…");

        authManager.resetPassword(resetToken, newPwd, new AuthManager.AuthCallback() {
            @Override public void onSuccess() {
                runOnUiThread(() -> { setLoading(false, null); showSuccess(); });
            }
            @Override public void onError(String error) {
                runOnUiThread(() -> { setLoading(false, null); showError(error); });
            }
        });
    }

    // ─────────────────────────── UI HELPERS ───────────────────────────

    private void showStep(int step) {
        clearError();
        stepEmailPanel.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        stepOtpPanel.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        stepResetPanel.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        successPanel.setVisibility(View.GONE);

        switch (step) {
            case 1:
                stepTitle.setText("Forgot Password?");
                stepSubtitle.setText("Enter your email and we'll send\nyou a 6-digit OTP");
                break;
            case 2:
                stepTitle.setText("Enter OTP");
                stepSubtitle.setText("We sent a 6-digit code to\n" + currentEmail
                        + "\n(" + otpSendCount + "/" + otpMaxSends + " OTP requests used)");
                otpInput.setText("");
                otpInput.requestFocus();
                verifyOtpButton.setEnabled(true); // re-enable in case previous session disabled it
                break;
            case 3:
                stepTitle.setText("New Password");
                stepSubtitle.setText("Choose a strong new password");
                newPasswordInput.setText("");
                confirmPasswordInput.setText("");
                newPasswordInput.requestFocus();
                break;
        }
    }

    private void showSuccess() {
        stepEmailPanel.setVisibility(View.GONE);
        stepOtpPanel.setVisibility(View.GONE);
        stepResetPanel.setVisibility(View.GONE);
        errorMessage.setVisibility(View.GONE);
        stepTitle.setText("All done!");
        stepSubtitle.setText("Your password has been reset.");
        successPanel.setVisibility(View.VISIBLE);
        successPanel.setAlpha(0f);
        successPanel.animate().alpha(1f).setDuration(300).start();
    }

    private void clearError() {
        errorMessage.setVisibility(View.GONE);
    }

    /** Shows a shaking error in red. */
    private void showError(String msg) {
        errorMessage.setText(msg != null ? msg : "Something went wrong. Please try again.");
        errorMessage.setVisibility(View.VISIBLE);
        errorMessage.animate().translationX(-8f).setDuration(50)
                .withEndAction(() -> errorMessage.animate().translationX(8f).setDuration(50)
                        .withEndAction(() -> errorMessage.animate().translationX(0f)
                                .setDuration(50).start()).start()).start();
    }

    /** Shows a non-error info in the same message view (no shake). */
    private void showInfo(String msg) {
        errorMessage.setText(msg);
        errorMessage.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading, String msg) {
        loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading && msg != null) {
            TextView loadingText = loadingOverlay.findViewById(R.id.loading_text);
            if (loadingText != null) loadingText.setText(msg);
        }
        sendOtpButton.setEnabled(!loading);
        // verifyOtpButton is managed here; resendOtpText is managed by the countdown timer
        verifyOtpButton.setEnabled(!loading);
        resetPasswordButton.setEnabled(!loading);
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    private String text(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}
