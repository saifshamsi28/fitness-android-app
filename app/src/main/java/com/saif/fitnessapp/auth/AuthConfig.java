package com.saif.fitnessapp.auth;

import android.os.Build;
import android.util.Log;

import com.saif.fitnessapp.BuildConfig;

public final class AuthConfig {

    private static final String TAG = "AuthConfig";

    private AuthConfig() {}

    // ===============================
    // DEVICE DETECTION (IMPROVED)
    // ===============================

    private static boolean isEmulator() {
        boolean result = Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MODEL.toLowerCase().contains("droid4x")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.HARDWARE.contains("vbox86")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
                || Build.BOARD.toLowerCase().contains("nox")
                || Build.BOOTLOADER.toLowerCase().contains("nox")
                || Build.HARDWARE.toLowerCase().contains("nox")
                || Build.PRODUCT.toLowerCase().contains("nox")
                || Build.SERIAL.toLowerCase().contains("nox")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"));

        Log.d(TAG, "Emulator Detection Details:");
        Log.d(TAG, "  FINGERPRINT: " + Build.FINGERPRINT);
        Log.d(TAG, "  MODEL: " + Build.MODEL);
        Log.d(TAG, "  MANUFACTURER: " + Build.MANUFACTURER);
        Log.d(TAG, "  BRAND: " + Build.BRAND);
        Log.d(TAG, "  DEVICE: " + Build.DEVICE);
        Log.d(TAG, "  PRODUCT: " + Build.PRODUCT);
        Log.d(TAG, "  HARDWARE: " + Build.HARDWARE);
        Log.d(TAG, "  BOARD: " + Build.BOARD);
        Log.d(TAG, "  Result: " + (result ? "EMULATOR" : "PHYSICAL DEVICE"));

        return result;
    }

    // ===============================
    // DYNAMIC URLS
    // ===============================

    public static final String KEYCLOAK_BASE_URL = isEmulator()
            ? BuildConfig.KEYCLOAK_EMULATOR_URL
            : BuildConfig.KEYCLOAK_DEVICE_URL;

    public static final String API_BASE_URL = isEmulator()
            ? BuildConfig.API_EMULATOR_URL
            : BuildConfig.API_DEVICE_URL;

    public static final String REALM = BuildConfig.KEYCLOAK_REALM;

    public static final String CLIENT_ID = BuildConfig.KEYCLOAK_CLIENT_ID;

    public static final String REDIRECT_URI = "com.saif.fitnessapp://oauth-callback";

    // ===============================
    // OIDC ENDPOINTS
    // ===============================

    public static final String ISSUER = KEYCLOAK_BASE_URL + "/realms/" + REALM;

    public static final String AUTHORIZATION_ENDPOINT =
            ISSUER + "/protocol/openid-connect/auth";

    public static final String TOKEN_ENDPOINT =
            ISSUER + "/protocol/openid-connect/token";

    public static final String USERINFO_ENDPOINT =
            ISSUER + "/protocol/openid-connect/userinfo";

    public static final String LOGOUT_ENDPOINT =
            ISSUER + "/protocol/openid-connect/logout";

    // ===============================
    // SCOPES
    // ===============================

    public static final String SCOPES = "openid profile email offline_access";

    public static final int API_TIMEOUT_SECONDS = 30;

    // ===============================
    // DEBUG LOGGING
    // ===============================

    static {
        Log.d(TAG, "═══════════════════════════════════════════════════");
        Log.d(TAG, "Device Type: " + (isEmulator() ? "EMULATOR" : "PHYSICAL DEVICE"));
        Log.d(TAG, "Keycloak URL: " + KEYCLOAK_BASE_URL);
        Log.d(TAG, "API URL: " + API_BASE_URL);
        Log.d(TAG, "Realm: " + REALM);
        Log.d(TAG, "Client ID: " + CLIENT_ID);
        Log.d(TAG, "═══════════════════════════════════════════════════");
    }
}