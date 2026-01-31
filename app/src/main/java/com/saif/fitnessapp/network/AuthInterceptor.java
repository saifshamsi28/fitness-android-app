package com.saif.fitnessapp.network;

import android.util.Log;

import com.saif.fitnessapp.auth.TokenManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private static final String TAG = "AuthInterceptor";
    private final TokenManager tokenManager;

    public AuthInterceptor(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // Add Authorization header if token exists
        String accessToken = tokenManager.getAccessToken();
        if (accessToken != null) {
            Log.d(TAG, "[v0] Adding Authorization header with access token");
            Request authenticatedRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + accessToken)
                    .build();
            return chain.proceed(authenticatedRequest);
        }

        return chain.proceed(originalRequest);
    }
}
