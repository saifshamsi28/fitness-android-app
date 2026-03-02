package com.saif.fitnessapp.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;
import net.openid.appauth.connectivity.ConnectionBuilder;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.HttpsURLConnection;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class AuthManager {

    private static final String TAG = "AuthManager";

    private final AuthorizationService authService;
    private final AuthorizationServiceConfiguration serviceConfig;
    private final TokenManager tokenManager;
    private final Context context;

    // Store the last request to validate response
    private AuthorizationRequest lastAuthRequest;

    @Inject
    public AuthManager(
            @ApplicationContext Context context,
            TokenManager tokenManager
    ) {
        this.context = context;
        this.tokenManager = tokenManager;

        // CRITICAL: Create AppAuthConfiguration with custom ConnectionBuilder that allows HTTP
        AppAuthConfiguration.Builder configBuilder = new AppAuthConfiguration.Builder();
        configBuilder.setConnectionBuilder(new UnsafeConnectionBuilder());
        AppAuthConfiguration appAuthConfig = configBuilder.build();

        // Create AuthorizationService with the configuration
        this.authService = new AuthorizationService(context, appAuthConfig);

        this.serviceConfig = new AuthorizationServiceConfiguration(
                Uri.parse(AuthConfig.AUTHORIZATION_ENDPOINT),
                Uri.parse(AuthConfig.TOKEN_ENDPOINT),
                null, // registrationEndpoint (not needed)
                Uri.parse(AuthConfig.LOGOUT_ENDPOINT)
        );
//        Log.d(TAG, "Service config initialized: " + serviceConfig);
//        Log.d(TAG, "Authorization endpoint: " + AuthConfig.AUTHORIZATION_ENDPOINT);
//        Log.d(TAG, "Token endpoint: " + AuthConfig.TOKEN_ENDPOINT);
    }

    // ===================== LOGIN =====================
    public void startLogin(Activity activity) {
        // Build additional parameters to handle issuer
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put("iss", AuthConfig.ISSUER);

        lastAuthRequest = new AuthorizationRequest.Builder(
                serviceConfig,
                AuthConfig.CLIENT_ID,
                ResponseTypeValues.CODE,
                Uri.parse(AuthConfig.REDIRECT_URI)
        )
                .setScope(AuthConfig.SCOPES)
                .setAdditionalParameters(additionalParams)
                .build();

//        Log.d(TAG, "Starting login with redirect URI: " + AuthConfig.REDIRECT_URI);
//        Log.d(TAG, "Client ID: " + AuthConfig.CLIENT_ID);
//        Log.d(TAG, "Scopes: " + AuthConfig.SCOPES);
//        Log.d(TAG, "Issuer: " + AuthConfig.ISSUER);

        // Use custom tab for authorization
        Intent authIntent = authService.getAuthorizationRequestIntent(lastAuthRequest);
        // Add this to force external browser instead of Chrome Custom Tabs
        authIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        activity.startActivityForResult(authIntent, 100);
    }

    // ===================== IN-APP LOGIN (ROPC) =====================

    /**
     * Direct credential login using Keycloak's Resource Owner Password Credentials grant.
     * No browser — stays fully inside the app.
     */
    public void loginWithCredentials(String email, String password, @NonNull AuthCallback callback) {
        new Thread(() -> {
            try {
                String tokenEndpoint = AuthConfig.TOKEN_ENDPOINT;
                String body = "grant_type=password"
                        + "&client_id=" + URLEncoder.encode(AuthConfig.CLIENT_ID, "UTF-8")
                        + "&username=" + URLEncoder.encode(email, "UTF-8")
                        + "&password=" + URLEncoder.encode(password, "UTF-8")
                        + "&scope=" + URLEncoder.encode(AuthConfig.SCOPES, "UTF-8");

                URL url = new URL(tokenEndpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "ROPC response code: " + responseCode);

                InputStream is = (responseCode == 200)
                        ? conn.getInputStream()
                        : conn.getErrorStream();

                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }

                String responseBody = sb.toString();
                Log.d(TAG, "ROPC response: " + responseBody);

                if (responseCode == 200) {
                    JSONObject json = new JSONObject(responseBody);
                    String accessToken  = json.optString("access_token", null);
                    String refreshToken = json.optString("refresh_token", null);
                    String idToken      = json.optString("id_token", null);
                    long expiresIn      = json.optLong("expires_in", 3600);
                    String tokenType    = json.optString("token_type", "Bearer");
                    String userId       = JwtUtils.extractSub(idToken != null ? idToken : accessToken);

                    tokenManager.saveTokens(accessToken, refreshToken, idToken,
                            expiresIn, tokenType, userId);
                    callback.onSuccess();
                } else {
                    String errorMsg = "Invalid credentials";
                    try {
                        JSONObject errJson = new JSONObject(responseBody);
                        String desc = errJson.optString("error_description", null);
                        if (desc != null && !desc.isEmpty()) errorMsg = desc;
                    } catch (Exception ignored) {}
                    callback.onError(errorMsg);
                }

            } catch (Exception e) {
                Log.e(TAG, "ROPC login error: " + e.getMessage(), e);
                callback.onError("Network error. Please check your connection.");
            }
        }).start();
    }

    // ===================== FORGOT PASSWORD (OTP-based) =====================

    /** Step 1 – request a 6-digit OTP for the given email.  Passes send-quota info to the UI. */
    public void sendForgotPasswordOtp(String email, @NonNull OtpSendCallback callback) {        new Thread(() -> {
            try {
                String json = "{\"email\":\"" + escape(email) + "\"}";
                String url  = AuthConfig.API_BASE_URL + "/api/auth/forgot-password/send-otp";
                HttpURLConnection conn = openJson(url, "POST");
                writeBody(conn, json);
                int    code = conn.getResponseCode();
                String body = readBody(conn, code);
                Log.d(TAG, "sendForgotPasswordOtp -> " + code + " : " + body);
                if (code == 200 || code == 201) {
                    JSONObject j = new JSONObject(body);
                    if (j.optBoolean("success", true)) {
                        int  sendCount = j.optInt("sendCount", 1);
                        int  maxSends  = j.optInt("maxSends",  5);
                        callback.onOtpSent(sendCount, maxSends);
                    } else {
                        callback.onError(j.optString("message", "Failed to send OTP"), 0, 0, 0);
                    }
                } else {
                    // 429 → CooldownException or RateLimitExceededException
                    JSONObject j    = new JSONObject(body);
                    String  msg     = j.optString("message", "Too many requests");
                    int  sendCount  = j.optInt("sendCount",  0);
                    int  maxSends   = j.optInt("maxSends",   5);
                    long retryAfter = j.optLong("retryAfterSeconds", 60);
                    callback.onError(msg, sendCount, maxSends, retryAfter);
                }
            } catch (Exception e) {
                Log.e(TAG, "sendForgotPasswordOtp error: " + e.getMessage(), e);
                callback.onError("Network error. Please check your connection.", 0, 0, 0);
            }
        }).start();
    }

    /**
     * Convenience overload for callers that don't need send-quota info (e.g. ProfileFragment).
     * Delegates to {@link #sendForgotPasswordOtp(String, OtpSendCallback)}.
     */
    public void sendForgotPasswordOtp(String email, @NonNull AuthCallback callback) {
        sendForgotPasswordOtp(email, new OtpSendCallback() {
            @Override public void onOtpSent(int sendCount, int maxSends) { callback.onSuccess(); }
            @Override public void onError(String msg, int s, int m, long r) { callback.onError(msg); }
        });
    }

    /** Step 2 – verify the OTP; {@link OtpVerifyCallback#onSuccess(String)} receives the resetToken. */
    public void verifyForgotPasswordOtp(String email, String otp,
                                        @NonNull OtpVerifyCallback callback) {
        new Thread(() -> {
            try {
                String json = "{\"email\":\"" + escape(email)
                        + "\",\"otp\":\"" + escape(otp) + "\"}";
                String url = AuthConfig.API_BASE_URL + "/api/auth/forgot-password/verify-otp";

                HttpURLConnection conn = openJson(url, "POST");
                writeBody(conn, json);
                int code = conn.getResponseCode();
                String body = readBody(conn, code);
                Log.d(TAG, "verifyForgotPasswordOtp -> " + code + " : " + body);

                if (code == 200) {
                    JSONObject j = new JSONObject(body);
                    boolean success = j.optBoolean("success", false);
                    if (success) {
                        String token = j.optString("resetToken", null);
                        callback.onSuccess(token);
                    } else {
                        callback.onError(j.optString("message", "Invalid OTP"));
                    }
                } else {
                    // 422: wrong OTP with attemptsRemaining, or expired/not-found
                    JSONObject j = new JSONObject(body);
                    String msg = j.optString("message", "Invalid or expired OTP");
                    callback.onError(msg);
                }
            } catch (Exception e) {
                Log.e(TAG, "verifyForgotPasswordOtp error: " + e.getMessage(), e);
                callback.onError("Network error. Please check your connection.");
            }
        }).start();
    }

    /** Step 3 – use the resetToken to set a new password. */
    public void resetPassword(String resetToken, String newPassword,
                              @NonNull AuthCallback callback) {
        new Thread(() -> {
            try {
                String json = "{\"resetToken\":\"" + escape(resetToken)
                        + "\",\"newPassword\":\"" + escape(newPassword) + "\"}";
                callApi(AuthConfig.API_BASE_URL + "/api/auth/forgot-password/reset",
                        "POST", json, callback);
            } catch (Exception e) {
                Log.e(TAG, "resetPassword error: " + e.getMessage(), e);
                callback.onError("Network error. Please check your connection.");
            }
        }).start();
    }

    // ===================== SIGNUP OTP =====================

    /** Step 1 – send verification OTP before account creation (checks email + username availability). */
    public void sendSignupOtp(String email, String firstName, String username,
                              @NonNull AuthCallback callback) {
        new Thread(() -> {
            try {
                String json = "{\"email\":\"" + escape(email)
                        + "\",\"firstName\":\"" + escape(firstName)
                        + "\",\"username\":\"" + escape(username != null ? username : "") + "\"}";
                callApi(AuthConfig.API_BASE_URL + "/api/auth/signup/send-otp",
                        "POST", json, callback);
            } catch (Exception e) {
                Log.e(TAG, "sendSignupOtp error: " + e.getMessage(), e);
                callback.onError("Network error. Please check your connection.");
            }
        }).start();
    }

    /** Backward-compatible overload (no username — skips username availability check). */
    public void sendSignupOtp(String email, String firstName, @NonNull AuthCallback callback) {
        sendSignupOtp(email, firstName, "", callback);
    }

    /** Step 2 – verify the signup OTP before creating the account. */
    public void verifySignupOtp(String email, String otp, @NonNull AuthCallback callback) {
        new Thread(() -> {
            try {
                String json = "{\"email\":\"" + escape(email)
                        + "\",\"otp\":\"" + escape(otp) + "\"}";
                callApi(AuthConfig.API_BASE_URL + "/api/auth/signup/verify-otp",
                        "POST", json, callback);
            } catch (Exception e) {
                Log.e(TAG, "verifySignupOtp error: " + e.getMessage(), e);
                callback.onError("Network error. Please check your connection.");
            }
        }).start();
    }

    // ===================== EMAIL CHANGE VERIFICATION (OTP) =====================

    /**
     * Send a 6-digit OTP to {@code newEmail} before allowing the email update.
     * Calls the authenticated {@code /api/users/{userId}/send-email-change-otp} endpoint,
     * which first checks that the new email is not already registered.
     */
    public void sendEmailChangeOtp(String newEmail, String firstName,
                                   @NonNull OtpSendCallback callback) {
        new Thread(() -> {
            try {
                String userId      = tokenManager.getUserId();
                String accessToken = tokenManager.getAccessToken();
                String json = "{\"email\":\"" + escape(newEmail) + "\"}";
                String url  = AuthConfig.API_BASE_URL + "/api/users/" + userId
                        + "/send-email-change-otp";
                HttpURLConnection conn = openJson(url, "POST");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                writeBody(conn, json);
                int    code = conn.getResponseCode();
                String body = readBody(conn, code);
                Log.d(TAG, "sendEmailChangeOtp -> " + code + " : " + body);
                if (code == 200 || code == 201) {
                    JSONObject j = new JSONObject(body);
                    if (j.optBoolean("success", true)) {
                        callback.onOtpSent(j.optInt("sendCount", 1), j.optInt("maxSends", 5));
                    } else {
                        callback.onError(j.optString("message", "Failed to send OTP"), 0, 0, 0);
                    }
                } else {
                    JSONObject j = new JSONObject(body);
                    callback.onError(
                            j.optString("message", "Request failed"),
                            j.optInt("sendCount", 0),
                            j.optInt("maxSends", 5),
                            j.optLong("retryAfterSeconds", 0));
                }
            } catch (Exception e) {
                Log.e(TAG, "sendEmailChangeOtp error: " + e.getMessage(), e);
                callback.onError("Network error. Please check your connection.", 0, 0, 0);
            }
        }).start();
    }

    /** Backward-compatible overload for callers that still use {@link AuthCallback}. */
    public void sendEmailChangeOtp(String newEmail, String firstName,
                                   @NonNull AuthCallback callback) {
        sendEmailChangeOtp(newEmail, firstName, new OtpSendCallback() {
            @Override public void onOtpSent(int s, int m) { callback.onSuccess(); }
            @Override public void onError(String msg, int s, int m, long r) { callback.onError(msg); }
        });
    }

    /**
     * Verify the OTP sent to the new email address.
     * Returns success if the OTP is valid — caller may then call updateUser.
     */
    public void verifyEmailChangeOtp(String newEmail, String otp,
                                     @NonNull AuthCallback callback) {
        new Thread(() -> {
            try {
                String json = "{\"email\":\"" + escape(newEmail)
                        + "\",\"otp\":\"" + escape(otp) + "\"}";
                callApi(AuthConfig.API_BASE_URL + "/api/auth/signup/verify-otp",
                        "POST", json, callback);
            } catch (Exception e) {
                Log.e(TAG, "verifyEmailChangeOtp error: " + e.getMessage(), e);
                callback.onError("Network error. Please check your connection.");
            }
        }).start();
    }

    // ===================== FORGOT PASSWORD (legacy – kept for safety) =====================

    /**
     * @deprecated Use {@link #sendForgotPasswordOtp} instead.
     */
    @Deprecated
    public void requestPasswordReset(String email, @NonNull AuthCallback callback) {
        new Thread(() -> {
            try {
                String apiUrl = AuthConfig.API_BASE_URL + "/api/auth/forgot-password";
                String body   = "{\"email\":\"" + email.replace("\"", "\\\"") + "\"}";

                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Forgot-password response code: " + responseCode);

                if (responseCode == 200 || responseCode == 204) {
                    callback.onSuccess();
                } else {
                    InputStream es = conn.getErrorStream();
                    String errorMsg = "Failed to send reset email. Please try again.";
                    if (es != null) {
                        StringBuilder sb = new StringBuilder();
                        try (BufferedReader br = new BufferedReader(
                                new InputStreamReader(es, StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = br.readLine()) != null) sb.append(line);
                        }
                        try {
                            JSONObject errJson = new JSONObject(sb.toString());
                            String msg = errJson.optString("message", null);
                            if (msg != null && !msg.isEmpty()) errorMsg = msg;
                        } catch (Exception ignored) {}
                    }
                    callback.onError(errorMsg);
                }
            } catch (Exception e) {
                Log.e(TAG, "Forgot-password error: " + e.getMessage(), e);
                callback.onError("Network error. Please check your connection.");
            }
        }).start();
    }

    // ===================== CALLBACK =====================
    public void handleAuthResponse(
            @NonNull Intent intent,
            @NonNull AuthCallback callback
    ) {
        Log.d(TAG, "handleAuthResponse called");
//        Log.d(TAG, "Intent action: " + intent.getAction());
//        Log.d(TAG, "Intent data: " + intent.getData());

        // Try to extract response
        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException exception = AuthorizationException.fromIntent(intent);

        // Log what we got
//        Log.d(TAG, "Response from intent: " + (response != null ? "found" : "null"));
//        Log.d(TAG, "Exception from intent: " + (exception != null ? exception.error : "null"));

        if (exception != null) {
            Log.e(TAG, "Authorization exception: " + exception.error);
            Log.e(TAG, "Error description: " + exception.errorDescription);
            callback.onError(exception.errorDescription != null ? exception.errorDescription : "Authorization failed");
            return;
        }

        if (response == null) {
            // Try manual extraction as fallback
            Log.w(TAG, "AppAuth didn't parse response, attempting manual extraction");
            response = manuallyExtractResponse(intent);

            if (response == null) {
                Log.e(TAG, "Failed to extract authorization response");
                callback.onError("Login cancelled or invalid response");
                return;
            }
        }

        Log.d(TAG, "Authorization successful, exchanging code for token");
//        Log.d(TAG, "Auth code: " + (response.authorizationCode != null ? "present" : "null"));

        // Create token request
        TokenRequest tokenRequest = response.createTokenExchangeRequest();

//        Log.d(TAG, "Token request created");
//        Log.d(TAG, "Token endpoint: " + tokenRequest.configuration.tokenEndpoint);

        authService.performTokenRequest(
                tokenRequest,
                (TokenResponse tokenResponse, AuthorizationException tokenEx) -> {
                    if (tokenEx != null) {
                        Log.e(TAG, "Token exchange error: " + tokenEx.error);
                        Log.e(TAG, "Error description: " + tokenEx.errorDescription);
                        if (tokenEx.getCause() != null) {
                            Log.e(TAG, "Cause: " + tokenEx.getCause().getMessage());
                            tokenEx.getCause().printStackTrace();
                        }
                        callback.onError(tokenEx.errorDescription != null ? tokenEx.errorDescription : "Token exchange failed");
                        return;
                    }

                    if (tokenResponse != null) {
                        handleTokenResponse(tokenResponse);
                        callback.onSuccess();
                    } else {
                        Log.e(TAG, "Token response is null");
                        callback.onError("Token exchange failed - no response");
                    }
                }
        );
    }

     //Refresh access token using refresh token
     //called when access token expires but refresh token is still valid
    public void refreshAccessToken(@NonNull TokenRefreshCallback callback) {
        String refreshToken = tokenManager.getRefreshToken();

        if (refreshToken == null || refreshToken.isEmpty()) {
            Log.e(TAG, "No refresh token available - user needs to login again");
            callback.onRefreshFailed("No refresh token available");
            return;
        }

//        Log.d(TAG, "Refreshing access token using refresh token");

        // Create token refresh request
        TokenRequest tokenRequest = new TokenRequest.Builder(
                serviceConfig,
                AuthConfig.CLIENT_ID
        )
                .setGrantType("refresh_token")
                .setRefreshToken(refreshToken)
                .setScope(AuthConfig.SCOPES)
                .build();

        authService.performTokenRequest(
                tokenRequest,
                (TokenResponse tokenResponse, AuthorizationException tokenEx) -> {
                    if (tokenEx != null) {
                        Log.e(TAG, "Token refresh failed: " + tokenEx.error);
                        Log.e(TAG, "Error description: " + tokenEx.errorDescription);

                        // If refresh token is invalid/expired, clear all tokens
                        // User will need to login again
                        tokenManager.clearTokens();
                        callback.onRefreshFailed(tokenEx.errorDescription != null ?
                                tokenEx.errorDescription : "Token refresh failed");
                        return;
                    }

                    if (tokenResponse != null) {
//                        Log.d(TAG, "Token refresh successful");
                        handleTokenResponse(tokenResponse);
                        callback.onRefreshSuccess();
                    } else {
                        Log.e(TAG, "Token refresh response is null");
                        callback.onRefreshFailed("Token refresh failed - no response");
                    }
                }
        );
    }

    /**
     * Handle token response from both initial login and refresh
     */
    private void handleTokenResponse(TokenResponse tokenResponse) {
        long expiresIn = tokenResponse.accessTokenExpirationTime != null
                ? (tokenResponse.accessTokenExpirationTime - System.currentTimeMillis()) / 1000
                : 3600;

        String userId = JwtUtils.extractSub(tokenResponse.idToken != null ?
                tokenResponse.idToken : tokenManager.getIdToken());

//        Log.d(TAG, "Token response processed");
//        Log.d(TAG, "User ID: " + userId);
//        Log.d(TAG, "Access token: " + (tokenResponse.accessToken != null ? "present (length: " + tokenResponse.accessToken.length() + ")" : "null"));
//        Log.d(TAG, "Refresh token: " + (tokenResponse.refreshToken != null ? "present" : "using existing"));
//        Log.d(TAG, "ID token: " + (tokenResponse.idToken != null ? "present" : "using existing"));
//        Log.d(TAG, "Expires in: " + expiresIn + " seconds");

        // Keycloak might not return a new refresh token on refresh
        // In that case, keep the existing refresh token
        String refreshTokenToSave = tokenResponse.refreshToken != null ?
                tokenResponse.refreshToken : tokenManager.getRefreshToken();

        tokenManager.saveTokens(
                tokenResponse.accessToken,
                refreshTokenToSave,
                tokenResponse.idToken != null ? tokenResponse.idToken : tokenManager.getIdToken(),
                expiresIn,
                tokenResponse.tokenType,
                userId != null ? userId : tokenManager.getUserId()
        );
    }

    /**
     * Manually extract authorization response when AppAuth fails to parse it
     */
    private AuthorizationResponse manuallyExtractResponse(Intent intent) {
        try {
            Uri uri = intent.getData();
            if (uri == null) {
                Log.e(TAG, "Intent data URI is null");
                return null;
            }

            String code = uri.getQueryParameter("code");
            String state = uri.getQueryParameter("state");
            String error = uri.getQueryParameter("error");

//            Log.d(TAG, "Manual extraction - code: " + (code != null ? "present" : "null"));
//            Log.d(TAG, "Manual extraction - state: " + (state != null ? state : "null"));
//            Log.d(TAG, "Manual extraction - error: " + (error != null ? error : "null"));

            if (error != null) {
                Log.e(TAG, "OAuth error in callback: " + error);
                return null;
            }

            if (code == null) {
                Log.e(TAG, "No authorization code in callback");
                return null;
            }

            // Build response using the builder
            if (lastAuthRequest != null) {
//                Log.d(TAG, "Building response from last auth request");
                return new AuthorizationResponse.Builder(lastAuthRequest)
                        .setAuthorizationCode(code)
                        .setState(state)
                        .build();
            } else {
//                Log.e(TAG, "No last auth request available");
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error manually extracting response: " + e.getMessage(), e);
            return null;
        }
    }

    public void logout() {
        tokenManager.clearTokens();
    }

    // ===================== PRIVATE HTTP HELPERS =====================

    private HttpURLConnection openJson(String urlStr, String method) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(15_000);
        return conn;
    }

    private void writeBody(HttpURLConnection conn, String json) throws Exception {
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String readBody(HttpURLConnection conn, int code) throws Exception {
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private String extractMessage(String json, String fallback) {
        try { return new JSONObject(json).optString("message", fallback); }
        catch (Exception e) { return fallback; }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Generic POST helper – calls callback on main thread is NOT guaranteed;
     * callers must use runOnUiThread as needed.
     */
    private void callApi(String urlStr, String method, String json,
                         @NonNull AuthCallback callback) throws Exception {
        HttpURLConnection conn = openJson(urlStr, method);
        writeBody(conn, json);
        int code = conn.getResponseCode();
        String body = readBody(conn, code);
        Log.d(TAG, "callApi " + urlStr + " -> " + code + " : " + body);
        if (code == 200 || code == 201) {
            JSONObject j = new JSONObject(body);
            if (j.optBoolean("success", true)) {
                callback.onSuccess();
            } else {
                callback.onError(j.optString("message", "Request failed"));
            }
        } else {
            callback.onError(extractMessage(body, "Request failed (HTTP " + code + ")"));
        }
    }

    // ===================== CALLBACK INTERFACES =====================

    public interface AuthCallback {
        void onSuccess();
        void onError(String error);
    }

    /**
     * Callback for OTP-send operations.
     * Carries per-request send-quota data so the UI can show "X/Y OTP requests used".
     */
    public interface OtpSendCallback {
        /**
         * OTP was sent successfully.
         *
         * @param sendCount how many OTPs have been requested in the current 2-hour window
         * @param maxSends  maximum allowed per window (usually 5)
         */
        void onOtpSent(int sendCount, int maxSends);

        /**
         * Request failed (rate-limited or network error).
         *
         * @param message          human-readable error from the server
         * @param sendCount        requests used so far (0 if unavailable)
         * @param maxSends         window limit (5 if unavailable)
         * @param retryAfterSeconds seconds the user must wait before retrying; 0 = not rate-limited
         */
        void onError(String message, int sendCount, int maxSends, long retryAfterSeconds);
    }

    /** Returned by OTP verify endpoints — carries the single-use token on success. */
    public interface OtpVerifyCallback {
        void onSuccess(String token);
        void onError(String error);
    }

    public interface TokenRefreshCallback {
        void onRefreshSuccess();
        void onRefreshFailed(String error);
    }

    public interface FreshTokenCallback {
        void onTokenReady(String accessToken);
        void onTokenError(String error);
    }

    /**
     * Custom ConnectionBuilder that allows HTTP connections for local development.
     *
     * WARNING: This should ONLY be used for local development with emulator.
     * In production, you MUST use HTTPS for security.
     */
    private static class UnsafeConnectionBuilder implements ConnectionBuilder {

        private static final int CONNECTION_TIMEOUT_MS = 15000;
        private static final int READ_TIMEOUT_MS = 10000;

        @NonNull
        @Override
        public HttpURLConnection openConnection(@NonNull Uri uri) throws IOException {
            Log.d(TAG, "Opening connection to: " + uri);

            URL url = new URL(uri.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(false);

            // For HTTPS, you might want to add SSL handling here
            if (connection instanceof HttpsURLConnection) {
                // Production HTTPS - secure by default
                Log.d(TAG, "Using HTTPS connection");
            } else {
                // HTTP - only for local development
                Log.w(TAG, "Using HTTP connection - NOT SECURE, only for local development!");
            }

            return connection;
        }
    }
}