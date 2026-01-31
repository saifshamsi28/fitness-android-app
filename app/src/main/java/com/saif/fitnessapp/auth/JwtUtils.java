package com.saif.fitnessapp.auth;

import android.util.Base64;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JwtUtils {

    public static String extractSub(String idToken) {
        if (idToken == null) return null;

        try {
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) return null;

            String payload =
                    new String(Base64.decode(parts[1], Base64.URL_SAFE));

            JsonObject json =
                    JsonParser.parseString(payload).getAsJsonObject();

            return json.has("sub") ? json.get("sub").getAsString() : null;

        } catch (Exception e) {
            return null;
        }
    }
}
