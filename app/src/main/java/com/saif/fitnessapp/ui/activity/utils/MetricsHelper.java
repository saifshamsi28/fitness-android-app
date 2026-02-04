package com.saif.fitnessapp.ui.activity.utils;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for parsing and formatting additional metrics
 * SAFE: Will never crash even if backend sends unexpected data
 */
public class MetricsHelper {

    // ----- UI COLOR PALETTE -----
    private static final String PRIMARY = "#4F46E5";   // Indigo
    private static final String SUCCESS = "#16A34A";   // Green
    private static final String WARNING = "#F59E0B";   // Amber
    private static final String DANGER  = "#EF4444";   // Red
    private static final String INFO    = "#0EA5E9";   // Sky Blue
    private static final String NEUTRAL = "#6B7280";   // Gray
    private static final String CALM    = "#22C55E";   // Calm Green
    private static final String ENERGY  = "#F97316";   // Orange


    public static class Metric {
        public String label;
        public String value;
        public String emoji;
        public String colorHex;   // <-- NEW

        public Metric(String label, String value, String emoji, String colorHex) {
            this.label = label;
            this.value = value;
            this.emoji = emoji;
            this.colorHex = colorHex;
        }
    }


    public static Map<String, Metric> parseMetrics(Map<String, Object> additionalMetrics) {
        Map<String, Metric> metrics = new HashMap<>();

        if (additionalMetrics == null || additionalMetrics.isEmpty()) {
            return metrics;
        }

        // -------- COMMON --------
        addIfExists(metrics, additionalMetrics, "avgHeartRate",
                "Average Heart Rate", " bpm", "❤️", DANGER);

        addIfExists(metrics, additionalMetrics, "intensityScore",
                "Workout Intensity", "/10", "🔥", ENERGY);

        addIfExists(metrics, additionalMetrics, "confidenceScore",
                "Session Quality", "%", "⭐", PRIMARY);

        addIfExists(metrics, additionalMetrics, "trainingZone",
                "Training Zone", "", "🎯", INFO);

        // -------- RUNNING / WALKING --------
        addIfExists(metrics, additionalMetrics, "distanceKm",
                "Total Distance", " km", "📏", INFO);

        addIfExists(metrics, additionalMetrics, "estimatedSteps",
                "Total Steps", "", "👟", SUCCESS);

        addIfExists(metrics, additionalMetrics, "avgSpeedKmh",
                "Average Speed", " km/h", "⚡", ENERGY);

        addIfExists(metrics, additionalMetrics, "cadenceSpm",
                "Cadence", " spm", "🎵", PRIMARY);

        addIfExists(metrics, additionalMetrics, "paceMinPerKm",
                "Pace", " min/km", "⏱️", WARNING);

        // -------- CYCLING --------
        addIfExists(metrics, additionalMetrics, "estimatedPowerWatts",
                "Average Power", " W", "⚡", DANGER);

        addIfExists(metrics, additionalMetrics, "cadenceRpm",
                "Pedal Cadence", " rpm", "🚴", PRIMARY);

        // -------- SWIMMING --------
        addIfExists(metrics, additionalMetrics, "laps",
                "Total Laps", "", "🏊", INFO);

        addIfExists(metrics, additionalMetrics, "distanceMeters",
                "Swim Distance", " m", "📏", INFO);

        addIfExists(metrics, additionalMetrics, "avgStrokeRate",
                "Stroke Rate", " spm", "🌊", PRIMARY);

        addIfExists(metrics, additionalMetrics, "efficiencyScore",
                "Swim Efficiency", "%", "🏅", SUCCESS);

        // -------- WEIGHT LIFTING --------
        addIfExists(metrics, additionalMetrics, "sets",
                "Total Sets", "", "🏋️", PRIMARY);

        addIfExists(metrics, additionalMetrics, "repsPerSet",
                "Reps per Set", "", "🔁", INFO);

        addIfExists(metrics, additionalMetrics, "estimatedLoadKg",
                "Average Load", " kg", "🏋️", DANGER);

        addIfExists(metrics, additionalMetrics, "totalVolumeKg",
                "Total Volume", " kg", "📦", WARNING);

        // -------- BOXING --------
        addIfExists(metrics, additionalMetrics, "punchesThrown",
                "Total Punches", "", "🥊", ENERGY);

        addIfExists(metrics, additionalMetrics, "rounds",
                "Rounds", "", "🥊", PRIMARY);

        addIfExists(metrics, additionalMetrics, "avgIntensity",
                "Fight Intensity", "/10", "🔥", DANGER);

        addIfExists(metrics, additionalMetrics, "reactionScore",
                "Reaction Score", "%", "⚡", INFO);

        // -------- YOGA / STRETCHING --------
        addIfExists(metrics, additionalMetrics, "flexibilityScore",
                "Flexibility", "%", "🧘", CALM);

        addIfExists(metrics, additionalMetrics, "breathingScore",
                "Breathing Control", "%", "🌬️", INFO);

        addIfExists(metrics, additionalMetrics, "mindfulnessScore",
                "Mindfulness", "%", "🧠", PRIMARY);

        addIfExists(metrics, additionalMetrics, "calmnessLevel",
                "Calmness Level", "/10", "🕊️", CALM);

        return metrics;
    }



    /**
     * Safe helper method — will not crash if data type is weird
     */
    private static void addIfExists(
            Map<String, Metric> metrics,
            Map<String, Object> data,
            String key,
            String label,
            String unit,
            String emoji,
            String colorHex
    ) {
        if (!data.containsKey(key) || data.get(key) == null) return;

        Object value = data.get(key);
        String formatted = formatNumber(value) + unit;
        metrics.put(key, new Metric(label, formatted, emoji, colorHex));
    }


    private static String formatNumber(Object value) {
        if (value == null) return "0";

        try {
            if (value instanceof Integer) {
                return String.valueOf(value);
            } else if (value instanceof Double) {
                return String.format("%.1f", (Double) value);
            } else if (value instanceof Float) {
                return String.format("%.1f", (Float) value);
            } else {
                double parsed = Double.parseDouble(value.toString());
                return String.format("%.1f", parsed);
            }
        } catch (Exception e) {
            return value.toString();
        }
    }
}
