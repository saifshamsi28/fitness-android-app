//package com.saif.fitnessapp.ui.activity;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * Helper class for parsing and formatting additional metrics
// */
//public class MetricsHelper {
//
//    /**
//     * Metric data class
//     */
//    public static class Metric {
//        public String label;
//        public String value;
//        public String emoji;
//
//        public Metric(String label, String value, String emoji) {
//            this.label = label;
//            this.value = value;
//            this.emoji = emoji;
//        }
//    }
//
//    /**
//     * Parse additionalMetrics Map and return formatted metrics
//     * Handles different data types (Integer, Double, String)
//     */
//    public static Map<String, Metric> parseMetrics(Map<String, Object> additionalMetrics) {
//        Map<String, Metric> metrics = new HashMap<>();
//
//        if (additionalMetrics == null || additionalMetrics.isEmpty()) {
//            return metrics;
//        }
//
//        // Heart Rate
//        if (additionalMetrics.containsKey("heartRate")) {
//            Object value = additionalMetrics.get("heartRate");
//            String formatted = formatNumber(value) + " bpm";
//            metrics.put("heartRate", new Metric("Heart Rate", formatted, "❤️"));
//        }
//
//        // Steps
//        if (additionalMetrics.containsKey("steps")) {
//            Object value = additionalMetrics.get("steps");
//            String formatted = formatNumber(value);
//            metrics.put("steps", new Metric("Steps", formatted, "👟"));
//        }
//
//        // Distance
//        if (additionalMetrics.containsKey("distanceKm") || additionalMetrics.containsKey("distance")) {
//            Object value = additionalMetrics.containsKey("distanceKm")
//                ? additionalMetrics.get("distanceKm")
//                : additionalMetrics.get("distance");
//            String formatted = formatDecimal(value) + " km";
//            metrics.put("distance", new Metric("Distance", formatted, "📏"));
//        }
//
//        // Average Speed
//        if (additionalMetrics.containsKey("avgSpeed") || additionalMetrics.containsKey("averageSpeed")) {
//            Object value = additionalMetrics.containsKey("avgSpeed")
//                ? additionalMetrics.get("avgSpeed")
//                : additionalMetrics.get("averageSpeed");
//            String formatted = formatDecimal(value) + " km/h";
//            metrics.put("speed", new Metric("Avg Speed", formatted, "⚡"));
//        }
//
//        // Elevation Gain (if available)
//        if (additionalMetrics.containsKey("elevationGain")) {
//            Object value = additionalMetrics.get("elevationGain");
//            String formatted = formatNumber(value) + " m";
//            metrics.put("elevation", new Metric("Elevation", formatted, "⛰️"));
//        }
//
//        // Max Speed (if available)
//        if (additionalMetrics.containsKey("maxSpeed")) {
//            Object value = additionalMetrics.get("maxSpeed");
//            String formatted = formatDecimal(value) + " km/h";
//            metrics.put("maxSpeed", new Metric("Max Speed", formatted, "🚀"));
//        }
//
//        return metrics;
//    }
//
//    /**
//     * Format number (handles Integer, Double, String)
//     */
//    private static String formatNumber(Object value) {
//        if (value == null) return "0";
//
//        if (value instanceof Integer) {
//            return String.valueOf(value);
//        } else if (value instanceof Double) {
//            return String.format("%.0f", (Double) value);
//        } else {
//            return value.toString();
//        }
//    }
//
//    /**
//     * Format decimal number with 1 decimal place
//     */
//    private static String formatDecimal(Object value) {
//        if (value == null) return "0.0";
//
//        if (value instanceof Double) {
//            return String.format("%.1f", (Double) value);
//        } else if (value instanceof Integer) {
//            return String.format("%.1f", ((Integer) value).doubleValue());
//        } else {
//            try {
//                double parsed = Double.parseDouble(value.toString());
//                return String.format("%.1f", parsed);
//            } catch (NumberFormatException e) {
//                return value.toString();
//            }
//        }
//    }
//}

package com.saif.fitnessapp.ui.activity;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for parsing and formatting additional metrics
 * SAFE: Will never crash even if backend sends unexpected data
 */
public class MetricsHelper {

    public static class Metric {
        public String label;
        public String value;
        public String emoji;

        public Metric(String label, String value, String emoji) {
            this.label = label;
            this.value = value;
            this.emoji = emoji;
        }
    }

    public static Map<String, Metric> parseMetrics(Map<String, Object> additionalMetrics) {
        Map<String, Metric> metrics = new HashMap<>();



        if (additionalMetrics == null || additionalMetrics.isEmpty()) {
            Log.d("MetricsHelper", "parseMetrics: " + additionalMetrics);
            return metrics;
        }
        Log.d("MetricsHelper", "parseMetrics: " + additionalMetrics.toString());

        // -------- COMMON METRICS --------

        addIfExists(metrics, additionalMetrics, "avgHeartRate",
                "Avg Heart Rate", " bpm", "❤️");

        addIfExists(metrics, additionalMetrics, "intensityScore",
                "Intensity Score", "/10", "🔥");

        addIfExists(metrics, additionalMetrics, "confidenceScore",
                "Session Quality", "%", "⭐");

        // -------- RUNNING / WALKING --------

        addIfExists(metrics, additionalMetrics, "distanceKm",
                "Distance", " km", "📏");

        addIfExists(metrics, additionalMetrics, "estimatedSteps",
                "Steps", "", "👟");

        addIfExists(metrics, additionalMetrics, "avgSpeedKmh",
                "Avg Speed", " km/h", "⚡");

        addIfExists(metrics, additionalMetrics, "cadenceSpm",
                "Cadence", " spm", "🎵");

        // -------- CYCLING --------

        addIfExists(metrics, additionalMetrics, "estimatedPowerWatts",
                "Power", " W", "⚡");

        // -------- SWIMMING --------

        addIfExists(metrics, additionalMetrics, "laps",
                "Laps", "", "🏊");

        addIfExists(metrics, additionalMetrics, "distanceMeters",
                "Distance", " m", "📏");

        addIfExists(metrics, additionalMetrics, "avgStrokeRate",
                "Stroke Rate", " spm", "🌊");

        // -------- WEIGHT LIFTING --------

        addIfExists(metrics, additionalMetrics, "sets",
                "Sets", "", "🏋️");

        addIfExists(metrics, additionalMetrics, "repsPerSet",
                "Reps/Set", "", "🔁");

        addIfExists(metrics, additionalMetrics, "estimatedLoadKg",
                "Load", " kg", "🏋️");

        // -------- BOXING --------

        addIfExists(metrics, additionalMetrics, "punchesThrown",
                "Punches", "", "🥊");

        addIfExists(metrics, additionalMetrics, "rounds",
                "Rounds", "", "🥊");

        addIfExists(metrics, additionalMetrics, "avgIntensity",
                "Intensity", "/10", "🔥");

        // -------- YOGA / STRETCHING --------

        addIfExists(metrics, additionalMetrics, "flexibilityScore",
                "Flexibility", "%", "🧘");

        addIfExists(metrics, additionalMetrics, "breathingScore",
                "Breathing", "%", "🌬️");

        addIfExists(metrics, additionalMetrics, "mindfulnessScore",
                "Mindfulness", "%", "🧠");

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
            String emoji
    ) {
        if (!data.containsKey(key) || data.get(key) == null) return;

        Object value = data.get(key);
        String formatted = formatNumber(value) + unit;
        metrics.put(key, new Metric(label, formatted, emoji));
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
