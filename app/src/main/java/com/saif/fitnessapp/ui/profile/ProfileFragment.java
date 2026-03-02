package com.saif.fitnessapp.ui.profile;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.saif.fitnessapp.R;
import com.saif.fitnessapp.activity.ActivityViewModel;
import com.saif.fitnessapp.auth.AuthManager;
import com.saif.fitnessapp.auth.TokenManager;
import com.saif.fitnessapp.network.dto.ActivityResponse;
import com.saif.fitnessapp.network.dto.ChangePasswordRequest;
import com.saif.fitnessapp.network.dto.UpdateProfileRequest;
import com.saif.fitnessapp.network.dto.UserResponse;
import com.saif.fitnessapp.ui.TitleController;
import com.saif.fitnessapp.ui.auth.LoginActivity;
import com.saif.fitnessapp.user.UserRepository;
import com.saif.fitnessapp.user.UserViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    @Inject
    TokenManager tokenManager;

    @Inject
    AuthManager authManager;

    private UserViewModel userViewModel;
    private ActivityViewModel activityViewModel;

    private TextView avatarInitials;
    private TextView nameText;
    private TextView emailText;
    private TextView createdAtText;
    private TextView memberDaysCount;
    private TextView totalActivitiesCount;
    private TextView totalCaloriesCount;
    private TextView infoFullName;
    private TextView infoEmail;
    private TextView infoMemberSince;
    private Button logoutButton;
    private ProgressBar progressBar;

    // Shimmer + real content container
    private ShimmerFrameLayout shimmerProfile;
    private LinearLayout profileRealContent;

    // Cached user for pre-filling edit sheets
    private UserResponse cachedUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Header views
        avatarInitials = view.findViewById(R.id.avatar_initials);
        nameText = view.findViewById(R.id.name_text);
        emailText = view.findViewById(R.id.email_text);

        // Stats
        memberDaysCount = view.findViewById(R.id.member_days_count);
        totalActivitiesCount = view.findViewById(R.id.total_activities_count);
        totalCaloriesCount = view.findViewById(R.id.total_calories_count);

        // Info section
        infoFullName = view.findViewById(R.id.info_full_name);
        infoEmail = view.findViewById(R.id.info_email);
        infoMemberSince = view.findViewById(R.id.info_member_since);

        // Other
        createdAtText = view.findViewById(R.id.created_at_text);
        logoutButton = view.findViewById(R.id.logout_button);
        progressBar = view.findViewById(R.id.progress_bar);

        // Shimmer
        shimmerProfile = view.findViewById(R.id.shimmer_profile);
        profileRealContent = view.findViewById(R.id.profile_real_content);
        shimmerProfile.startShimmer();

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        activityViewModel = new ViewModelProvider(this).get(ActivityViewModel.class);

        logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());

        View rowEdit   = view.findViewById(R.id.row_edit_profile);
        View rowPwd    = view.findViewById(R.id.row_change_password);
        if (rowEdit != null) rowEdit.setOnClickListener(v -> showEditProfileSheet());
        if (rowPwd  != null) rowPwd.setOnClickListener(v  -> showChangePasswordSheet());

        String userId = tokenManager.getUserId();
        if (userId != null) {
            loadUserProfile(userId);
            loadActivityStats(userId);
        }
    }

    private void loadUserProfile(String userId) {
        userViewModel.getUserProfile(userId).observe(getViewLifecycleOwner(), user -> {
            // Stop shimmer and reveal real content on first response
            if (shimmerProfile.getVisibility() == View.VISIBLE) {
                shimmerProfile.stopShimmer();
                shimmerProfile.setVisibility(View.GONE);
                profileRealContent.setVisibility(View.VISIBLE);
            }
            progressBar.setVisibility(View.GONE);
            if (user != null) {
                cachedUser = user;
                String fullName = user.getFirstName() + " " + user.getLastName();
                nameText.setText(fullName);
                emailText.setText(user.getEmail());

                // Avatar initials
                String initials = "";
                if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
                    initials += user.getFirstName().charAt(0);
                }
                if (user.getLastName() != null && !user.getLastName().isEmpty()) {
                    initials += user.getLastName().charAt(0);
                }
                avatarInitials.setText(initials.toUpperCase());

                // Info section
                if (infoFullName != null) infoFullName.setText(fullName);
                if (infoEmail != null) infoEmail.setText(user.getEmail());
                if (infoMemberSince != null) infoMemberSince.setText(formatDateTime(user.getCreatedAt()));

                // Member days
                long days = calculateMemberDays(user.getCreatedAt());
                if (memberDaysCount != null) memberDaysCount.setText(String.valueOf(days));

                // Hidden field for compatibility
                if (createdAtText != null) {
                    createdAtText.setText("Member since: " + formatDateTime(user.getCreatedAt()));
                }
            }
        });
    }

    private void loadActivityStats(String userId) {
        activityViewModel.getRecentActivities(userId).observe(getViewLifecycleOwner(), activities -> {
            if (activities != null && totalActivitiesCount != null) {
                totalActivitiesCount.setText(String.valueOf(activities.size()));

                int totalCalories = 0;
                for (ActivityResponse a : activities) {
                    if (a.getCaloriesBurned() != null) totalCalories += a.getCaloriesBurned();
                }
                if (totalCaloriesCount != null) {
                    totalCaloriesCount.setText(String.valueOf(totalCalories));
                }
            }
        });
    }

    private long calculateMemberDays(String isoTime) {
        if (isoTime == null) return 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
            Date date = sdf.parse(isoTime);
            if (date != null) {
                long diff = System.currentTimeMillis() - date.getTime();
                return Math.max(1, TimeUnit.MILLISECONDS.toDays(diff));
            }
        } catch (Exception e) {
            try {
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date date2 = sdf2.parse(isoTime);
                if (date2 != null) {
                    long diff = System.currentTimeMillis() - date2.getTime();
                    return Math.max(1, TimeUnit.MILLISECONDS.toDays(diff));
                }
            } catch (Exception e2) {
                // ignore
            }
        }
        return 1;
    }

    // =========================================================
    // EDIT PROFILE BOTTOM SHEET  (2-step: form → OTP if email changes)
    // =========================================================
    private void showEditProfileSheet() {
        if (getContext() == null) return;
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_edit_profile, null);
        sheet.setContentView(v);
        sheet.getBehavior().setPeekHeight(900);

        // ---- dynamic header ----
        TextView sheetTitle    = v.findViewById(R.id.edit_sheet_title);
        TextView sheetSubtitle = v.findViewById(R.id.edit_sheet_subtitle);

        // ---- step 1: form panel ----
        View              formPanel      = v.findViewById(R.id.edit_step_form_panel);
        TextInputLayout   firstNameLayout = v.findViewById(R.id.edit_first_name_layout);
        TextInputLayout   lastNameLayout  = v.findViewById(R.id.edit_last_name_layout);
        TextInputLayout   emailLayout     = v.findViewById(R.id.edit_email_layout);
        TextInputEditText firstNameInput  = v.findViewById(R.id.edit_first_name_input);
        TextInputEditText lastNameInput   = v.findViewById(R.id.edit_last_name_input);
        TextInputEditText emailInput      = v.findViewById(R.id.edit_email_input);
        TextView          formError       = v.findViewById(R.id.edit_profile_error);
        MaterialButton    saveBtn         = v.findViewById(R.id.btn_save_profile);

        // ---- step 2: OTP panel ----
        View              otpPanel      = v.findViewById(R.id.edit_step_otp_panel);
        TextInputLayout   otpLayout     = v.findViewById(R.id.edit_otp_layout);
        TextInputEditText otpInput      = v.findViewById(R.id.edit_otp_input);
        TextView          otpError      = v.findViewById(R.id.edit_otp_error);
        MaterialButton    verifyOtpBtn  = v.findViewById(R.id.btn_verify_otp);
        TextView          resendOtp     = v.findViewById(R.id.edit_resend_otp);
        TextView          changeEmail   = v.findViewById(R.id.edit_change_email);

        // Pre-fill with current values
        if (cachedUser != null) {
            firstNameInput.setText(cachedUser.getFirstName());
            lastNameInput.setText(cachedUser.getLastName());
            emailInput.setText(cachedUser.getEmail());
        }

        // Mutable holder for the pending profile data during OTP step
        final String[] pendingFirstName = {null};
        final String[] pendingLastName  = {null};
        final String[] pendingEmail     = {null};

        // Helper: switch between step 1 and step 2
        Runnable showStep1 = () -> {
            formPanel.setVisibility(View.VISIBLE);
            otpPanel.setVisibility(View.GONE);
            sheetTitle.setText("Edit Profile");
            sheetSubtitle.setVisibility(View.GONE);
        };

        // Helper: perform the actual profile update immediately (no email change)
        Runnable doUpdate = () -> {
            String userId = tokenManager.getUserId();
            UpdateProfileRequest request = new UpdateProfileRequest(
                    pendingFirstName[0], pendingLastName[0], pendingEmail[0]);
            userViewModel.updateUser(userId, request, new UserRepository.UpdateCallback() {
                @Override public void onSuccess(UserResponse updated) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        sheet.dismiss();
                        Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                        if (updated != null) {
                            cachedUser = updated;
                            String full = updated.getFirstName() + " " + updated.getLastName();
                            if (nameText     != null) nameText.setText(full);
                            if (emailText    != null) emailText.setText(updated.getEmail());
                            if (infoFullName != null) infoFullName.setText(full);
                            if (infoEmail    != null) infoEmail.setText(updated.getEmail());
                            String ini = "";
                            if (updated.getFirstName() != null && !updated.getFirstName().isEmpty())
                                ini += updated.getFirstName().charAt(0);
                            if (updated.getLastName() != null && !updated.getLastName().isEmpty())
                                ini += updated.getLastName().charAt(0);
                            if (avatarInitials != null) avatarInitials.setText(ini.toUpperCase());
                        }
                    });
                }
                @Override public void onError(String message) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        // Show error back in form panel
                        otpPanel.setVisibility(View.GONE);
                        formPanel.setVisibility(View.VISIBLE);
                        sheetTitle.setText("Edit Profile");
                        sheetSubtitle.setVisibility(View.GONE);
                        saveBtn.setEnabled(true);
                        saveBtn.setText("Save Changes");
                        formError.setText(message);
                        formError.setVisibility(View.VISIBLE);
                    });
                }
            });
        };

        // ── When the OTP panel becomes visible, start the 60-second resend cooldown
        // so the first send is treated the same as subsequent resends.
        final CountDownTimer[] editOtpTimer = {null};
        Runnable startEditOtpTimer = () -> {
            if (editOtpTimer[0] != null) editOtpTimer[0].cancel();
            editOtpTimer[0] = startResendCountdown(resendOtp, "Resend OTP", 60_000L);
        };

        // ---- STEP 1: Save button ----
        saveBtn.setOnClickListener(btn -> {
            String firstName = txt(firstNameInput);
            String lastName  = txt(lastNameInput);
            String email     = txt(emailInput);

            firstNameLayout.setError(null);
            lastNameLayout.setError(null);
            emailLayout.setError(null);
            formError.setVisibility(View.GONE);

            boolean valid = true;
            if (firstName.length() < 2) { firstNameLayout.setError("At least 2 characters"); valid = false; }
            if (lastName.length()  < 2) { lastNameLayout.setError("At least 2 characters");  valid = false; }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { emailLayout.setError("Invalid email"); valid = false; }
            if (!valid) return;

            pendingFirstName[0] = firstName;
            pendingLastName[0]  = lastName;
            pendingEmail[0]     = email;

            String currentEmail = cachedUser != null ? cachedUser.getEmail() : "";
            if (email.equalsIgnoreCase(currentEmail)) {
                saveBtn.setEnabled(false);
                saveBtn.setText("Saving…");
                doUpdate.run();
            } else {
                saveBtn.setEnabled(false);
                saveBtn.setText("Sending OTP…");
                authManager.sendEmailChangeOtp(email, firstName, new AuthManager.OtpSendCallback() {
                    @Override public void onOtpSent(int sendCount, int maxSends) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            formPanel.setVisibility(View.GONE);
                            otpPanel.setVisibility(View.VISIBLE);
                            saveBtn.setEnabled(true);
                            saveBtn.setText("Save Changes");
                            sheetTitle.setText("Verify New Email");
                            sheetSubtitle.setText("We sent a 6-digit code to " + email);
                            sheetSubtitle.setVisibility(View.VISIBLE);
                            otpInput.setText("");
                            otpError.setVisibility(View.GONE);
                            startEditOtpTimer.run();
                        });
                    }
                    @Override public void onError(String message, int sendCount, int maxSends, long retryAfterSeconds) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            saveBtn.setEnabled(true);
                            saveBtn.setText("Save Changes");
                            formError.setText(message != null ? message : "Failed to send OTP. Try again.");
                            formError.setVisibility(View.VISIBLE);
                        });
                    }
                });
            }
        });

        // ---- STEP 2: Verify OTP button ----
        verifyOtpBtn.setOnClickListener(btn -> {
            String otp = txt(otpInput);
            otpLayout.setError(null);
            otpError.setVisibility(View.GONE);

            if (otp.length() != 6) { otpLayout.setError("Enter the 6-digit code"); return; }

            verifyOtpBtn.setEnabled(false);
            verifyOtpBtn.setText("Verifying…");

            authManager.verifyEmailChangeOtp(pendingEmail[0], otp, new AuthManager.AuthCallback() {
                @Override public void onSuccess() {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        verifyOtpBtn.setEnabled(true);
                        verifyOtpBtn.setText("Verify & Save");
                        doUpdate.run();
                    });
                }
                @Override public void onError(String message) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        verifyOtpBtn.setEnabled(true);
                        verifyOtpBtn.setText("Verify & Save");
                        otpError.setText(message != null ? message : "Invalid or expired OTP.");
                        otpError.setVisibility(View.VISIBLE);
                    });
                }
            });
        });

        // ---- STEP 2: Resend OTP ----
        resendOtp.setOnClickListener(rv -> {
            otpError.setVisibility(View.GONE);
            authManager.sendEmailChangeOtp(pendingEmail[0], pendingFirstName[0],
                    new AuthManager.OtpSendCallback() {
                        @Override public void onOtpSent(int sendCount, int maxSends) {
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() -> {
                                startEditOtpTimer.run();
                                Toast.makeText(requireContext(), "OTP resent!", Toast.LENGTH_SHORT).show();
                            });
                        }
                        @Override public void onError(String message, int sendCount, int maxSends, long retryAfterSeconds) {
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() -> {
                                resendOtp.setEnabled(true);
                                otpError.setText(message != null ? message : "Failed to resend OTP.");
                                otpError.setVisibility(View.VISIBLE);
                                if (retryAfterSeconds > 0) {
                                    if (editOtpTimer[0] != null) editOtpTimer[0].cancel();
                                    editOtpTimer[0] = startResendCountdown(resendOtp, "Resend OTP",
                                            retryAfterSeconds * 1_000L);
                                }
                            });
                        }
                    });
        });

        // Cancel timer when sheet is dismissed
        sheet.setOnDismissListener(d -> { if (editOtpTimer[0] != null) editOtpTimer[0].cancel(); });

        // ---- STEP 2: Change email → go back to step 1 ----
        changeEmail.setOnClickListener(cv -> {
            if (editOtpTimer[0] != null) editOtpTimer[0].cancel();
            showStep1.run();
        });

        sheet.show();
    }

    // =========================================================
    // CHANGE PASSWORD BOTTOM SHEET
    // =========================================================
    private void showChangePasswordSheet() {
        if (getContext() == null) return;
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_change_password, null);
        sheet.setContentView(v);
        sheet.getBehavior().setPeekHeight(900);

        TextInputLayout   currentLayout  = v.findViewById(R.id.current_password_layout);
        TextInputLayout   newPwdLayout   = v.findViewById(R.id.new_password_layout);
        TextInputLayout   confirmLayout  = v.findViewById(R.id.confirm_password_layout);
        TextInputEditText currentInput   = v.findViewById(R.id.current_password_input);
        TextInputEditText newPwdInput    = v.findViewById(R.id.new_password_input);
        TextInputEditText confirmInput   = v.findViewById(R.id.confirm_password_input);
        TextView          errorText      = v.findViewById(R.id.change_password_error);
        MaterialButton    changeBtn      = v.findViewById(R.id.btn_change_password);

        changeBtn.setOnClickListener(btn -> {
            String current = txt(currentInput);
            String newPwd  = txt(newPwdInput);
            String confirm = txt(confirmInput);

            currentLayout.setError(null);
            newPwdLayout.setError(null);
            confirmLayout.setError(null);
            errorText.setVisibility(View.GONE);

            boolean valid = true;
            if (TextUtils.isEmpty(current)) { currentLayout.setError("Required"); valid = false; }
            if (!isPasswordValid(newPwd))   { newPwdLayout.setError("Min 8 chars, uppercase, lowercase, number & special char"); valid = false; }
            if (!newPwd.equals(confirm))    { confirmLayout.setError("Passwords do not match"); valid = false; }
            if (!valid) return;

            changeBtn.setEnabled(false);
            changeBtn.setText("Changing…");

            String userId = tokenManager.getUserId();
            ChangePasswordRequest request = new ChangePasswordRequest(current, newPwd);
            userViewModel.changePassword(userId, request, new UserRepository.ActionCallback() {
                @Override public void onSuccess() {
                    requireActivity().runOnUiThread(() -> {
                        sheet.dismiss();
                        Toast.makeText(requireContext(), "Password changed successfully!", Toast.LENGTH_SHORT).show();
                    });
                }
                @Override public void onError(String message) {
                    requireActivity().runOnUiThread(() -> {
                        changeBtn.setEnabled(true);
                        changeBtn.setText("Change Password");
                        if (message != null && message.toLowerCase().contains("current")) {
                            currentLayout.setError(message);
                        } else {
                            errorText.setText(message);
                            errorText.setVisibility(View.VISIBLE);
                        }
                    });
                }
            });
        });

        // Wire "Forgot current password?" link
        TextView forgotPwdLink = v.findViewById(R.id.forgot_current_password);
        if (forgotPwdLink != null) {
            forgotPwdLink.setOnClickListener(fv -> {
                sheet.dismiss();
                showResetPasswordOtpSheet();
            });
        }

        sheet.show();
    }

    // =========================================================
    // RESET PASSWORD VIA EMAIL OTP (from "Forgot current password?")
    // =========================================================
    private void showResetPasswordOtpSheet() {
        if (getContext() == null || cachedUser == null) return;
        String userEmail = cachedUser.getEmail();

        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_reset_password_otp, null);
        sheet.setContentView(v);
        sheet.getBehavior().setPeekHeight(900);

        TextView      resetTitle    = v.findViewById(R.id.otp_reset_title);
        TextView      resetSubtitle = v.findViewById(R.id.otp_reset_subtitle);

        // Step 1 — OTP
        View              otpStepView  = v.findViewById(R.id.otp_reset_step_otp);
        TextInputLayout   otpLayout    = v.findViewById(R.id.otp_reset_otp_layout);
        TextInputEditText otpInput     = v.findViewById(R.id.otp_reset_otp_input);
        TextView          otpError     = v.findViewById(R.id.otp_reset_otp_error);
        MaterialButton    verifyOtpBtn = v.findViewById(R.id.btn_verify_reset_otp);
        TextView          resendOtp    = v.findViewById(R.id.otp_reset_resend);

        // Step 2 — New password
        View              pwdStepView  = v.findViewById(R.id.otp_reset_step_new_password);
        TextInputLayout   newPwdLayout = v.findViewById(R.id.otp_reset_new_password_layout);
        TextInputLayout   confirmLayout= v.findViewById(R.id.otp_reset_confirm_layout);
        TextInputEditText newPwdInput  = v.findViewById(R.id.otp_reset_new_password_input);
        TextInputEditText confirmInput = v.findViewById(R.id.otp_reset_confirm_input);
        TextView          pwdError     = v.findViewById(R.id.otp_reset_new_password_error);
        MaterialButton    setNewPwdBtn = v.findViewById(R.id.btn_set_new_password);

        // Holds the reset token received from verifyForgotPasswordOtp
        final String[] resetToken = {null};

        resetSubtitle.setText("We'll send a 6-digit code to " + userEmail);

        // Helper to send/resend OTP — timer is started *inside* the callback so
        // the countdown duration always matches what the server reports.
        final CountDownTimer[] resetOtpTimer = {null};
        Runnable sendOtp = () -> authManager.sendForgotPasswordOtp(userEmail, new AuthManager.OtpSendCallback() {
            @Override public void onOtpSent(int sendCount, int maxSends) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (resetOtpTimer[0] != null) resetOtpTimer[0].cancel();
                    resetOtpTimer[0] = startResendCountdown(resendOtp, "Resend OTP", 60_000L);
                    Toast.makeText(requireContext(), "OTP sent to " + userEmail, Toast.LENGTH_SHORT).show();
                });
            }
            @Override public void onError(String message, int sendCount, int maxSends, long retryAfterSeconds) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    otpError.setText(message != null ? message : "Failed to send OTP.");
                    otpError.setVisibility(View.VISIBLE);
                    // Start countdown with server-provided remaining seconds
                    if (retryAfterSeconds > 0) {
                        if (resetOtpTimer[0] != null) resetOtpTimer[0].cancel();
                        resetOtpTimer[0] = startResendCountdown(resendOtp, "Resend OTP",
                                retryAfterSeconds * 1_000L);
                    }
                });
            }
        });
        sendOtp.run();  // auto-send on sheet open

        // Cancel timer on dismiss
        sheet.setOnDismissListener(d -> { if (resetOtpTimer[0] != null) resetOtpTimer[0].cancel(); });

        // ---- STEP 1: Verify OTP button ----
        verifyOtpBtn.setOnClickListener(btn -> {
            String otp = txt(otpInput);
            otpLayout.setError(null);
            otpError.setVisibility(View.GONE);

            if (otp.length() != 6) { otpLayout.setError("Enter the 6-digit code"); return; }

            verifyOtpBtn.setEnabled(false);
            verifyOtpBtn.setText("Verifying…");

            authManager.verifyForgotPasswordOtp(userEmail, otp, new AuthManager.OtpVerifyCallback() {
                @Override public void onSuccess(String token) {
                    if (!isAdded()) return;
                    resetToken[0] = token;
                    requireActivity().runOnUiThread(() -> {
                        verifyOtpBtn.setEnabled(true);
                        verifyOtpBtn.setText("Verify OTP");
                        // Transition to step 2
                        otpStepView.setVisibility(View.GONE);
                        pwdStepView.setVisibility(View.VISIBLE);
                        resetTitle.setText("Set New Password");
                        resetSubtitle.setText("Choose a strong new password");
                    });
                }
                @Override public void onError(String message) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        verifyOtpBtn.setEnabled(true);
                        verifyOtpBtn.setText("Verify OTP");
                        otpError.setText(message != null ? message : "Invalid or expired OTP.");
                        otpError.setVisibility(View.VISIBLE);
                    });
                }
            });
        });

        // ---- STEP 1: Resend OTP ----
        resendOtp.setOnClickListener(rv -> {
            otpError.setVisibility(View.GONE);
            authManager.sendForgotPasswordOtp(userEmail, new AuthManager.OtpSendCallback() {
                @Override public void onOtpSent(int sendCount, int maxSends) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (resetOtpTimer[0] != null) resetOtpTimer[0].cancel();
                        resetOtpTimer[0] = startResendCountdown(resendOtp, "Resend OTP", 60_000L);
                        Toast.makeText(requireContext(), "OTP resent!", Toast.LENGTH_SHORT).show();
                    });
                }
                @Override public void onError(String message, int sendCount, int maxSends, long retryAfterSeconds) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        resendOtp.setEnabled(true);
                        otpError.setText(message != null ? message : "Failed to resend OTP.");
                        otpError.setVisibility(View.VISIBLE);
                        if (retryAfterSeconds > 0) {
                            if (resetOtpTimer[0] != null) resetOtpTimer[0].cancel();
                            resetOtpTimer[0] = startResendCountdown(resendOtp, "Resend OTP",
                                    retryAfterSeconds * 1_000L);
                        }
                    });
                }
            });
        });

        // ---- STEP 2: Set new password button ----
        setNewPwdBtn.setOnClickListener(btn -> {
            String newPwd = txt(newPwdInput);
            String confirm = txt(confirmInput);

            newPwdLayout.setError(null);
            confirmLayout.setError(null);
            pwdError.setVisibility(View.GONE);

            boolean valid = true;
            if (!isPasswordValid(newPwd))     { newPwdLayout.setError("Min 8 chars, uppercase, lowercase, number & special char"); valid = false; }
            if (!newPwd.equals(confirm))      { confirmLayout.setError("Passwords do not match"); valid = false; }
            if (resetToken[0] == null)        { pwdError.setText("Session expired. Please restart."); pwdError.setVisibility(View.VISIBLE); valid = false; }
            if (!valid) return;

            setNewPwdBtn.setEnabled(false);
            setNewPwdBtn.setText("Saving…");

            authManager.resetPassword(resetToken[0], newPwd, new AuthManager.AuthCallback() {
                @Override public void onSuccess() {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        sheet.dismiss();
                        Toast.makeText(requireContext(), "Password changed!", Toast.LENGTH_SHORT).show();
                    });
                }
                @Override public void onError(String message) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        setNewPwdBtn.setEnabled(true);
                        setNewPwdBtn.setText("Set New Password");
                        pwdError.setText(message != null ? message : "Failed to set password. Try again.");
                        pwdError.setVisibility(View.VISIBLE);
                    });
                }
            });
        });

        sheet.show();
    }

    /**
     * Starts a 60-second countdown on a resend TextView.
     * Disables the view while counting, then re-enables it with the original label.
     * Returns the timer so callers can cancel it if needed (e.g. sheet dismiss).
     */
    private static CountDownTimer startResendCountdown(TextView resendView, String originalLabel,
                                                       long durationMs) {
        resendView.setEnabled(false);
        CountDownTimer timer = new CountDownTimer(durationMs, 1_000L) {
            @Override public void onTick(long millisUntilFinished) {
                long secs = millisUntilFinished / 1000L;
                resendView.setText("Resend in " + secs + "s");
            }
            @Override public void onFinish() {
                resendView.setEnabled(true);
                resendView.setText(originalLabel);
            }
        };
        timer.start();
        return timer;
    }

    private static String txt(TextInputEditText f) {
        return f.getText() != null ? f.getText().toString().trim() : "";
    }

    private static boolean isPasswordValid(String p) {
        if (p == null || p.length() < 8) return false;
        boolean up = false, lo = false, di = false, sp = false;
        for (char c : p.toCharArray()) {
            if (Character.isUpperCase(c)) up = true;
            else if (Character.isLowerCase(c)) lo = true;
            else if (Character.isDigit(c)) di = true;
            else if ("@#$%!&*".indexOf(c) >= 0) sp = true;
        }
        return up && lo && di && sp;
    }

    // =========================================================
    // BEAUTIFUL LOGOUT CONFIRMATION DIALOG
    // =========================================================
    private void showLogoutConfirmationDialog() {
        if (getContext() == null) return;

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_logout_confirm);

        // Rounded white background
        if (dialog.getWindow() != null) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(56f);
            dialog.getWindow().setBackgroundDrawable(bg);

            // Set dialog width to 92% of screen
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.copyFrom(dialog.getWindow().getAttributes());
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(params);
        }

        // Entrance animation: scale + fade in
        if (dialog.getWindow() != null) {
            final View decorView = dialog.getWindow().getDecorView();
            decorView.setScaleX(0.88f);
            decorView.setScaleY(0.88f);
            decorView.setAlpha(0f);
            decorView.animate()
                    .scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(220)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        MaterialButton btnStay = dialog.findViewById(R.id.btn_cancel);
        MaterialButton btnLogout = dialog.findViewById(R.id.btn_logout_confirm);

        // "Stay" dismisses with a shrink-out animation
        btnStay.setOnClickListener(v -> dismissDialogAnimated(dialog));

        // Tapping outside also dismisses
        dialog.setCanceledOnTouchOutside(true);

        // "Logout" closes dialog and performs logout
        btnLogout.setOnClickListener(v -> {
            dialog.dismiss();
            logout();
        });

        dialog.show();
    }

    private void dismissDialogAnimated(Dialog dialog) {
        if (dialog.getWindow() != null) {
            final View decorView = dialog.getWindow().getDecorView();
            decorView.animate()
                    .scaleX(0.88f).scaleY(0.88f).alpha(0f)
                    .setDuration(160)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(dialog::dismiss)
                    .start();
        } else {
            dialog.dismiss();
        }
    }

    private void logout() {
        tokenManager.clearTokens();
        authManager.logout();
        startActivity(new Intent(requireContext(), LoginActivity.class));
        requireActivity().finish();
    }

    public String formatDateTime(String isoTime) {
        if (isoTime == null) return "";
        try {
            SimpleDateFormat input = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS",
                    Locale.US
            );
            SimpleDateFormat output = new SimpleDateFormat(
                    "dd MMM yyyy",
                    Locale.US
            );
            return output.format(input.parse(isoTime));
        } catch (Exception e) {
            try {
                SimpleDateFormat input2 = new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss",
                        Locale.US
                );
                SimpleDateFormat output2 = new SimpleDateFormat(
                        "dd MMM yyyy",
                        Locale.US
                );
                return output2.format(input2.parse(isoTime));
            } catch (Exception e2) {
                return isoTime;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requireActivity() instanceof TitleController) {
            ((TitleController) requireActivity()).setTitle("Profile");
        }
    }
}