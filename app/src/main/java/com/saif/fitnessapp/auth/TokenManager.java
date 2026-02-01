package com.saif.fitnessapp.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class TokenManager {
    private static final String PREF_NAME = "fitness_auth_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_ID_TOKEN = "id_token";
    private static final String KEY_EXPIRES_IN = "expires_in";
    private static final String KEY_TOKEN_TYPE = "token_type";
    private static final String KEY_USER_ID = "user_id";

    // Buffer time before actual expiry to proactively refresh token (5 minutes)
    private static final long TOKEN_REFRESH_BUFFER_MS = 5 * 60 * 1000;

    private final SharedPreferences encryptedSharedPreferences;

    @Inject
    public TokenManager(@ApplicationContext Context context) {
        try {
            // Using non-deprecated MasterKey API (fixes the deprecation warning)
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            // Using new EncryptedSharedPreferences.create() signature
            this.encryptedSharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize EncryptedSharedPreferences", e);
        }
    }

    public void saveTokens(String accessToken, String refreshToken, String idToken, long expiresIn, String tokenType, String userId) {
        encryptedSharedPreferences.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_ID_TOKEN, idToken)
                .putLong(KEY_EXPIRES_IN, System.currentTimeMillis() + (expiresIn * 1000))
                .putString(KEY_TOKEN_TYPE, tokenType)
                .putString(KEY_USER_ID, userId)
                .apply();
    }

    public String getAccessToken() {
        return encryptedSharedPreferences.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return encryptedSharedPreferences.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getIdToken() {
        return encryptedSharedPreferences.getString(KEY_ID_TOKEN, null);
    }

    public String getUserId() {
        return encryptedSharedPreferences.getString(KEY_USER_ID, null);
    }

    public String getTokenType() {
        return encryptedSharedPreferences.getString(KEY_TOKEN_TYPE, "Bearer");
    }

    //Check if access token is expired or about to expire soon
    public boolean isAccessTokenExpired() {
        long expiresAt = encryptedSharedPreferences.getLong(KEY_EXPIRES_IN, 0);
        // Add buffer time to refresh before actual expiry
        return System.currentTimeMillis() >= (expiresAt - TOKEN_REFRESH_BUFFER_MS);
    }


     //Now checks expiry with proper naming
    @Deprecated
    public boolean isTokenExpired() {
        return isAccessTokenExpired();
    }

    //Check if user is authenticated
    public boolean isLoggedIn() {
        String refreshToken = getRefreshToken();
        return refreshToken != null && !refreshToken.isEmpty();
    }

    /**
     * Check if we need to refresh the access token
     * Returns true if user is logged in but access token is expired/expiring soon
     */
    public boolean needsTokenRefresh() {
        return isLoggedIn() && isAccessTokenExpired();
    }

    /**
     * Check if we have a valid, non-expired access token
     * Use this when you need to verify the access token is immediately usable
     */
    public boolean hasValidAccessToken() {
        return getAccessToken() != null && !isAccessTokenExpired();
    }

    public void clearTokens() {
        encryptedSharedPreferences.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_ID_TOKEN)
                .remove(KEY_EXPIRES_IN)
                .remove(KEY_TOKEN_TYPE)
                .remove(KEY_USER_ID)
                .apply();
    }
}