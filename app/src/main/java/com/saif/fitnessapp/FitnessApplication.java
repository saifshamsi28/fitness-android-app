package com.saif.fitnessapp;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class FitnessApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }
}
