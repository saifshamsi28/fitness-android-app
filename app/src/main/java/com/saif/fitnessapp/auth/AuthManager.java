//package com.saif.fitnessapp.auth;
//
//import android.content.Context;
//import android.content.Intent;
//import android.net.Uri;
//
//import androidx.browser.customtabs.CustomTabsIntent;
//
//import net.openid.appauth.AuthorizationRequest;
//import net.openid.appauth.AuthorizationService;
//import net.openid.appauth.AuthorizationServiceConfiguration;
//import net.openid.appauth.ClientAuthentication;
//import net.openid.appauth.ClientSecretBasic;
//import net.openid.appauth.ResponseTypeValues;
//import net.openid.appauth.TokenRequest;
//
//import javax.inject.Inject;
//import javax.inject.Singleton;
//
//import dagger.hilt.android.qualifiers.ApplicationContext;

//@Singleton
//public class AuthManager {
//    private final Context context;
//    private final TokenManager tokenManager;
//    private AuthorizationService authorizationService;
//
//    @Inject
//    public AuthManager(@ApplicationContext Context context, TokenManager tokenManager) {
//        this.context = context;
//        this.tokenManager = tokenManager;
//    }
//
//    public void initializeAuthService() {
//        if (authorizationService == null) {
//            authorizationService = new AuthorizationService(context);
//        }
//    }
//
//    public Intent getLoginIntent() {
//        initializeAuthService();
//
//        AuthorizationServiceConfiguration authConfig = new AuthorizationServiceConfiguration(
//                Uri.parse(AuthConfig.AUTHORIZATION_ENDPOINT),
//                Uri.parse(AuthConfig.TOKEN_ENDPOINT),
//                null,
//                Uri.parse(AuthConfig.LOGOUT_ENDPOINT)
//        );
//
//        AuthorizationRequest authRequest = new AuthorizationRequest.Builder(
//                authConfig,
//                AuthConfig.CLIENT_ID,
//                ResponseTypeValues.CODE,
//                Uri.parse(AuthConfig.REDIRECT_URI)
//        )
//                .setScopes(AuthConfig.SCOPES.split(" "))
//                .build();
//
//        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
//        return authorizationService.getAuthorizationRequestIntent(authRequest, customTabsIntent);
//    }
//
//    public void exchangeCodeForToken(String authorizationCode, String clientSecret, AuthTokenCallback callback) {
//        initializeAuthService();
//
//        AuthorizationServiceConfiguration authConfig = new AuthorizationServiceConfiguration(
//                Uri.parse(AuthConfig.AUTHORIZATION_ENDPOINT),
//                Uri.parse(AuthConfig.TOKEN_ENDPOINT),
//                null,
//                Uri.parse(AuthConfig.LOGOUT_ENDPOINT)
//        );
//
//        TokenRequest tokenRequest = new TokenRequest.Builder(
//                authConfig,
//                AuthConfig.CLIENT_ID
//        )
//                .setAuthorizationCode(authorizationCode)
//                .setRedirectUri(Uri.parse(AuthConfig.REDIRECT_URI))
//                .build();
//
//        ClientAuthentication clientAuth = new ClientSecretBasic(clientSecret);
//
//        authorizationService.performTokenRequest(tokenRequest, clientAuth, (response, ex) -> {
//            if (response != null) {
//                String accessToken = response.accessToken;
//                String refreshToken = response.refreshToken;
//                String idToken = response.idToken;
//                long expiresIn = response.accessTokenExpirationTime != null ?
//                        (response.accessTokenExpirationTime - System.currentTimeMillis()) / 1000 : 3600;
//
//                // Extract userId from idToken (decode JWT)
//                String userId = extractUserIdFromToken(idToken);
//
//                tokenManager.saveTokens(accessToken, refreshToken, idToken, expiresIn, "Bearer", userId);
//                callback.onSuccess(userId);
//            } else {
//                callback.onError(ex != null ? ex.getMessage() : "Token exchange failed");
//            }
//        });
//    }
//
//    public void refreshAccessToken(String refreshToken, AuthTokenCallback callback) {
//        initializeAuthService();
//
//        AuthorizationServiceConfiguration authConfig = new AuthorizationServiceConfiguration(
//                Uri.parse(AuthConfig.AUTHORIZATION_ENDPOINT),
//                Uri.parse(AuthConfig.TOKEN_ENDPOINT),
//                null,
//                Uri.parse(AuthConfig.LOGOUT_ENDPOINT)
//        );
//
//        TokenRequest tokenRequest = new TokenRequest.Builder(
//                authConfig,
//                AuthConfig.CLIENT_ID
//        )
//                .setRefreshToken(refreshToken)
//                .build();
//
//        authorizationService.performTokenRequest(tokenRequest, (response, ex) -> {
//            if (response != null) {
//                String accessToken = response.accessToken;
//                String newRefreshToken = response.refreshToken != null ? response.refreshToken : refreshToken;
//                String idToken = response.idToken;
//                long expiresIn = response.accessTokenExpirationTime != null ?
//                        (response.accessTokenExpirationTime - System.currentTimeMillis()) / 1000 : 3600;
//
//                String userId = extractUserIdFromToken(idToken);
//                tokenManager.saveTokens(accessToken, newRefreshToken, idToken, expiresIn, "Bearer", userId);
//                callback.onSuccess(userId);
//            } else {
//                callback.onError(ex != null ? ex.getMessage() : "Token refresh failed");
//            }
//        });
//    }
//
//    public void logout() {
//        tokenManager.clearTokens();
//        if (authorizationService != null) {
//            authorizationService.dispose();
//            authorizationService = null;
//        }
//    }
//
//    private String extractUserIdFromToken(String idToken) {
//        if (idToken == null) return null;
//
//        try {
//            String[] parts = idToken.split("\\.");
//            if (parts.length == 3) {
//                String payload = new String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE));
//                com.google.gson.JsonObject json = new com.google.gson.JsonParser().parse(payload).getAsJsonObject();
//                if (json.has("sub")) {
//                    return json.get("sub").getAsString();
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    public interface AuthTokenCallback {
//        void onSuccess(String userId);
//        void onError(String error);
//    }
//
//}

//package com.saif.fitnessapp.auth;
//
//import android.app.Activity;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.net.Uri;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//
//import com.saif.fitnessapp.ui.auth.LoginActivity;
//
//import net.openid.appauth.AuthorizationException;
//import net.openid.appauth.AuthorizationRequest;
//import net.openid.appauth.AuthorizationResponse;
//import net.openid.appauth.AuthorizationService;
//import net.openid.appauth.AuthorizationServiceConfiguration;
//import net.openid.appauth.ResponseTypeValues;
//import net.openid.appauth.TokenResponse;
//
//import javax.inject.Inject;
//import javax.inject.Singleton;
//
//import dagger.hilt.android.qualifiers.ApplicationContext;
//
//@Singleton
//public class AuthManager {
//
//    private final AuthorizationService authService;
//    private final AuthorizationServiceConfiguration serviceConfig;
//    private final TokenManager tokenManager;
//
//    @Inject
//    public AuthManager(
//            @ApplicationContext Context context,
//            TokenManager tokenManager
//    ) {
//        this.tokenManager = tokenManager;
//        this.authService = new AuthorizationService(context);
//
//        this.serviceConfig = new AuthorizationServiceConfiguration(
//                Uri.parse(AuthConfig.AUTHORIZATION_ENDPOINT),
//                Uri.parse(AuthConfig.TOKEN_ENDPOINT),
//                Uri.parse(AuthConfig.USERINFO_ENDPOINT),
//                Uri.parse(AuthConfig.LOGOUT_ENDPOINT)
//        );
//        Log.d("AuthManager", "Service config: " + serviceConfig);
//    }
//
//    // ===================== LOGIN =====================
//    public void startLogin(Activity activity) {
//
//        AuthorizationRequest request =
//                new AuthorizationRequest.Builder(
//                        serviceConfig,
//                        AuthConfig.CLIENT_ID,
//                        ResponseTypeValues.CODE,
//                        Uri.parse(AuthConfig.REDIRECT_URI)
//                )
//                        .setScope(AuthConfig.SCOPES)
//                        .build();
//
//        Log.d("AuthManager", "Starting login with request");
//
//        Intent successIntent = new Intent(activity, LoginActivity.class);
//        successIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//
//        Intent cancelIntent = new Intent(activity, LoginActivity.class);
//        cancelIntent.putExtra("auth_cancelled", true);
//
//        authService.performAuthorizationRequest(
//                request,
//                PendingIntent.getActivity(
//                        activity,
//                        0,
//                        successIntent,
//                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//                ),
//                PendingIntent.getActivity(
//                        activity,
//                        0,
//                        cancelIntent,
//                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//                )
//        );
//    }
//
//
////    public Intent getLoginIntent() {
////        initializeAuthService();
////
////        AuthorizationServiceConfiguration serviceConfig =
////                new AuthorizationServiceConfiguration(
////                        Uri.parse(AuthConfig.AUTHORIZATION_ENDPOINT),
////                        Uri.parse(AuthConfig.TOKEN_ENDPOINT)
////                );
////
////        AuthorizationRequest authRequest =
////                new AuthorizationRequest.Builder(
////                        serviceConfig,
////                        AuthConfig.CLIENT_ID,
////                        ResponseTypeValues.CODE,
////                        Uri.parse(AuthConfig.REDIRECT_URI)
////                )
////                        .setScope(AuthConfig.SCOPES)
////                        .build();
////
////        return authorizationService.getAuthorizationRequestIntent(authRequest);
////    }
//
//
//    // ===================== CALLBACK =====================
//    public void handleAuthResponse(
//            @NonNull Intent intent,
//            @NonNull AuthCallback callback
//    ) {
//        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
//        AuthorizationException exception = AuthorizationException.fromIntent(intent);
//
//        if (response == null) {
//            callback.onError(
//                    exception != null ? exception.errorDescription : "Login cancelled"
//            );
//            return;
//        }
//
//        authService.performTokenRequest(
//                response.createTokenExchangeRequest(),
//                (TokenResponse tokenResponse, AuthorizationException tokenEx) -> {
//
//                    if (tokenResponse != null) {
//                        long expiresIn = tokenResponse.accessTokenExpirationTime != null
//                                ? (tokenResponse.accessTokenExpirationTime - System.currentTimeMillis()) / 1000
//                                : 3600;
//
//                        String userId = JwtUtils.extractSub(tokenResponse.idToken);
//                        Log.d("AuthManager", "User ID: " + userId);
//                        Log.d("AuthManager", "Token Response: " + tokenResponse.jsonSerializeString());
//
//                        tokenManager.saveTokens(
//                                tokenResponse.accessToken,
//                                tokenResponse.refreshToken,
//                                tokenResponse.idToken,
//                                expiresIn,
//                                tokenResponse.tokenType,
//                                userId
//                        );
//
//                        callback.onSuccess();
//                    } else {
//                        callback.onError(
//                                tokenEx != null ? tokenEx.errorDescription : "Token exchange failed"
//                        );
//                    }
//                }
//        );
//    }
//
//    public void logout() {
//        tokenManager.clearTokens();
//        authService.dispose();
//    }
//
//    public interface AuthCallback {
//        void onSuccess();
//        void onError(String error);
//    }
//}

package com.saif.fitnessapp.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.saif.fitnessapp.ui.auth.LoginActivity;

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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
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
        Log.d(TAG, "Service config initialized: " + serviceConfig);
        Log.d(TAG, "Authorization endpoint: " + AuthConfig.AUTHORIZATION_ENDPOINT);
        Log.d(TAG, "Token endpoint: " + AuthConfig.TOKEN_ENDPOINT);
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

        Log.d(TAG, "Starting login with redirect URI: " + AuthConfig.REDIRECT_URI);
        Log.d(TAG, "Client ID: " + AuthConfig.CLIENT_ID);
        Log.d(TAG, "Scopes: " + AuthConfig.SCOPES);
        Log.d(TAG, "Issuer: " + AuthConfig.ISSUER);

        // Use custom tab for authorization
        Intent authIntent = authService.getAuthorizationRequestIntent(lastAuthRequest);
        // Add this to force external browser instead of Chrome Custom Tabs
        authIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        activity.startActivityForResult(authIntent, 100);
    }

    // ===================== CALLBACK =====================
    public void handleAuthResponse(
            @NonNull Intent intent,
            @NonNull AuthCallback callback
    ) {
        Log.d(TAG, "handleAuthResponse called");
        Log.d(TAG, "Intent action: " + intent.getAction());
        Log.d(TAG, "Intent data: " + intent.getData());

        // Try to extract response
        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException exception = AuthorizationException.fromIntent(intent);

        // Log what we got
        Log.d(TAG, "Response from intent: " + (response != null ? "found" : "null"));
        Log.d(TAG, "Exception from intent: " + (exception != null ? exception.error : "null"));

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
        Log.d(TAG, "Auth code: " + (response.authorizationCode != null ? "present" : "null"));

        // Create token request
        TokenRequest tokenRequest = response.createTokenExchangeRequest();

        Log.d(TAG, "Token request created");
        Log.d(TAG, "Token endpoint: " + tokenRequest.configuration.tokenEndpoint);

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
                        long expiresIn = tokenResponse.accessTokenExpirationTime != null
                                ? (tokenResponse.accessTokenExpirationTime - System.currentTimeMillis()) / 1000
                                : 3600;

                        String userId = JwtUtils.extractSub(tokenResponse.idToken);

                        Log.d(TAG, "Token exchange successful");
                        Log.d(TAG, "User ID: " + userId);
                        Log.d(TAG, "Access token: " + (tokenResponse.accessToken != null ? "present (length: " + tokenResponse.accessToken.length() + ")" : "null"));
                        Log.d(TAG, "Refresh token: " + (tokenResponse.refreshToken != null ? "present" : "null"));
                        Log.d(TAG, "ID token: " + (tokenResponse.idToken != null ? "present" : "null"));

                        tokenManager.saveTokens(
                                tokenResponse.accessToken,
                                tokenResponse.refreshToken,
                                tokenResponse.idToken,
                                expiresIn,
                                tokenResponse.tokenType,
                                userId
                        );

                        callback.onSuccess();
                    } else {
                        Log.e(TAG, "Token response is null");
                        callback.onError("Token exchange failed - no response");
                    }
                }
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

            Log.d(TAG, "Manual extraction - code: " + (code != null ? "present" : "null"));
            Log.d(TAG, "Manual extraction - state: " + (state != null ? state : "null"));
            Log.d(TAG, "Manual extraction - error: " + (error != null ? error : "null"));

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
                Log.d(TAG, "Building response from last auth request");
                return new AuthorizationResponse.Builder(lastAuthRequest)
                        .setAuthorizationCode(code)
                        .setState(state)
                        .build();
            } else {
                Log.e(TAG, "No last auth request available");
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error manually extracting response: " + e.getMessage(), e);
            return null;
        }
    }

    public void logout() {
        tokenManager.clearTokens();
        authService.dispose();
    }

    public interface AuthCallback {
        void onSuccess();
        void onError(String error);
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