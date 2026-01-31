package com.saif.fitnessapp.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

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

    private final SharedPreferences encryptedSharedPreferences;

    @Inject
    public TokenManager(@ApplicationContext Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            this.encryptedSharedPreferences = EncryptedSharedPreferences.create(
                    PREF_NAME,
                    masterKeyAlias,
                    context,
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

    public boolean isTokenExpired() {
        long expiresAt = encryptedSharedPreferences.getLong(KEY_EXPIRES_IN, 0);
        return System.currentTimeMillis() >= expiresAt;
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null && !isTokenExpired();
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
