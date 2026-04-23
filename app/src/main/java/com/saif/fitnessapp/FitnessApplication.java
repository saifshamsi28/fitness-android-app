package com.saif.fitnessapp;

import android.app.Application;

import com.saif.fitnessapp.utils.ThemeManager;

import java.io.InterruptedIOException;

import dagger.hilt.android.HiltAndroidApp;
import io.reactivex.rxjava3.exceptions.UndeliverableException;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

@HiltAndroidApp
public class FitnessApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Restore the user's saved theme preference before any Activity starts.
        // Must be called early so the first Activity gets the correct theme.
        ThemeManager.applyTheme(this);

        // When the user navigates away while a Paging load is in-flight, RxJava
        // disposes the subscription and OkHttp raises InterruptedIOException.
        // Because the subscriber is already disposed, RxJava can't deliver the
        // error normally — it wraps it in UndeliverableException and crashes.
        // This handler silences expected cancellation errors and re-throws the rest.
        RxJavaPlugins.setErrorHandler(throwable -> {
            Throwable cause = (throwable instanceof UndeliverableException)
                    ? throwable.getCause() : throwable;
            if (cause instanceof InterruptedException || cause instanceof InterruptedIOException) {
                return; // normal cancellation caused by fragment/paging teardown
            }
            Thread thread = Thread.currentThread();
            thread.getUncaughtExceptionHandler().uncaughtException(thread, cause);
        });
    }
}
