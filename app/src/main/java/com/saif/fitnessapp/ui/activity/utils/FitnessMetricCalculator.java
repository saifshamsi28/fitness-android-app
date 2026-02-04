package com.saif.fitnessapp.ui.activity.utils;

import java.util.Random;

public class FitnessMetricCalculator {
    
    private static final Random random = new Random();

    public static double randomInRange(double min, double max) {
        return min + (random.nextDouble() * (max - min));
    }

    public static int randomIntInRange(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    public static double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}
