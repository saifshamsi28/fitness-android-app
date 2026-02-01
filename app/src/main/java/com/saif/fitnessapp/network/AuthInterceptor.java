package com.saif.fitnessapp.network;

import android.util.Log;

import androidx.annotation.NonNull;

import com.saif.fitnessapp.auth.AuthManager;
import com.saif.fitnessapp.auth.TokenManager;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * AuthInterceptor with automatic token refresh
 * Features:
 * - Automatically adds access token to all API requests
 * - Detects expired access tokens before making requests
 * - Refreshes tokens automatically using refresh token
 * - Handles 401 responses and retries with fresh token
 * - Thread-safe token refresh (prevents multiple simultaneous refreshes)
 */
public class AuthInterceptor implements Interceptor {
    private static final String TAG = "AuthInterceptor";

    private final TokenManager tokenManager;
    private final AuthManager authManager;

    // Prevent multiple simultaneous refresh attempts
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);

    public AuthInterceptor(TokenManager tokenManager, AuthManager authManager) {
        this.tokenManager = tokenManager;
        this.authManager = authManager;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // Skip authentication for certain endpoints
        if (shouldSkipAuth(originalRequest)) {
            return chain.proceed(originalRequest);
        }

        // Get fresh access token (will refresh if needed)
        String accessToken = getFreshAccessTokenSync();

        if (accessToken == null) {
            Log.w(TAG, "No access token available, proceeding without auth");
            return chain.proceed(originalRequest);
        }

        // Add access token to request
        Request authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .build();

        Response response = chain.proceed(authenticatedRequest);

        // If we get 401 Unauthorized, try refreshing token once and retry
        if (response.code() == 401 && !hasRetryHeader(originalRequest)) {
            Log.d(TAG, "Got 401 Unauthorized, attempting token refresh and retry");
            response.close();

            // Try to refresh token
            String newAccessToken = refreshTokenSync();

            if (newAccessToken != null) {
                Log.d(TAG, "Token refresh successful, retrying request");
                // Retry request with new token
                Request retryRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer " + newAccessToken)
                        .header("X-Token-Retry", "true") // Prevent infinite retry loop
                        .build();
                return chain.proceed(retryRequest);
            } else {
                Log.e(TAG, "Token refresh failed, returning 401");
                // Refresh failed, return the 401 response
                // Your app should handle this by redirecting to login
            }
        }

        return response;
    }

    /**
     * Check if request should skip authentication
     */
    private boolean shouldSkipAuth(Request request) {
        String url = request.url().toString();
        // Skip auth for login/token endpoints
        return url.contains("/token") ||
                url.contains("/auth/realms/") ||
                url.contains("/protocol/openid-connect");
    }

    /**
     * Check if request has already been retried
     * Prevents infinite retry loops
     */
    private boolean hasRetryHeader(Request request) {
        return request.header("X-Token-Retry") != null;
    }

    /**
     * Get fresh access token, refreshing if needed
     * This is synchronous because OkHttp interceptors must be synchronous
     */
    private String getFreshAccessTokenSync() {
        // If access token is still valid, use it
        if (tokenManager.hasValidAccessToken()) {
            return tokenManager.getAccessToken();
        }

        // Access token expired, refresh it
        Log.d(TAG, "Access token expired, refreshing...");
        return refreshTokenSync();
    }

    /**
     * Synchronously refresh the access token
     */
    private String refreshTokenSync() {
        // Prevent multiple simultaneous refresh attempts
        if (!isRefreshing.compareAndSet(false, true)) {
            Log.d(TAG, "Token refresh already in progress, waiting...");
            return waitForRefreshToComplete();
        }

        try {
            Log.d(TAG, "Starting token refresh...");

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> newAccessToken = new AtomicReference<>(null);

            authManager.refreshAccessToken(new AuthManager.TokenRefreshCallback() {
                @Override
                public void onRefreshSuccess() {
                    Log.d(TAG, "Token refresh successful in interceptor");
                    newAccessToken.set(tokenManager.getAccessToken());
                    latch.countDown();
                }

                @Override
                public void onRefreshFailed(String error) {
                    Log.e(TAG, "Token refresh failed in interceptor: " + error);
                    latch.countDown();
                }
            });

            // Wait for refresh to complete (with timeout)
            try {
                boolean completed = latch.await(10, TimeUnit.SECONDS);
                if (!completed) {
                    Log.e(TAG, "Token refresh timeout");
                }
            } catch (InterruptedException e){
                Log.e(TAG, "Token refresh interrupted", e);
                Thread.currentThread().interrupt();
            }

            return newAccessToken.get();
        } finally {
            isRefreshing.set(false);
        }
    }

    /**
     * Wait for another thread's refresh to complete
     */
    private String waitForRefreshToComplete() {
        // Wait up to 10 seconds for the other refresh to complete
        for (int i = 0; i < 20; i++) {
            if (!isRefreshing.get()) {
                // Refresh completed
                return tokenManager.getAccessToken();
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        Log.w(TAG, "Timeout waiting for token refresh");
        return tokenManager.getAccessToken();
    }
}