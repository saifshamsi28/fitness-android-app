package com.saif.fitnessapp.utils;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Manages light/dark theme preference across the app.
 *
 * Three modes:
 *  - MODE_NIGHT_FOLLOW_SYSTEM  (default) — respects OS setting
 *  - MODE_NIGHT_YES            — always dark
 *  - MODE_NIGHT_NO             — always light
 *
 * Usage:
 *   ThemeManager.applyTheme(app)       ← call once in Application.onCreate
 *   ThemeManager.toggle(context)       ← call from theme toggle button click
 *   ThemeManager.isDark(context)       ← check current effective mode
 */
public final class ThemeManager {

    private static final String PREFS  = "theme_prefs";
    private static final String KEY    = "night_mode";

    private ThemeManager() {}

    /** Apply the saved preference (call from Application.onCreate). */
    public static void applyTheme(Application app) {
        int saved = getSavedMode(app);
        AppCompatDelegate.setDefaultNightMode(saved);
    }

    /**
     * Toggle between DARK and LIGHT. If currently following system, start
     * from the effective current appearance so the transition feels natural.
     */
    public static void toggle(Context ctx) {
        int current = getSavedMode(ctx);
        int next;
        if (current == AppCompatDelegate.MODE_NIGHT_YES) {
            next = AppCompatDelegate.MODE_NIGHT_NO;
        } else {
            // Covers both MODE_NIGHT_NO and FOLLOW_SYSTEM
            next = AppCompatDelegate.MODE_NIGHT_YES;
        }
        save(ctx, next);
        AppCompatDelegate.setDefaultNightMode(next);
    }

    /** @return true if the current UI is rendered in dark mode. */
    public static boolean isDark(Context ctx) {
        int uiMode = ctx.getResources().getConfiguration().uiMode
                     & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == Configuration.UI_MODE_NIGHT_YES;
    }

    /** Raw stored integer mode (AppCompatDelegate constant). */
    public static int getSavedMode(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                  .getInt(KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    private static void save(Context ctx, int mode) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
           .edit().putInt(KEY, mode).apply();
    }
}
