package com.saif.fitnessapp.repository;

import androidx.lifecycle.MutableLiveData;

import com.saif.fitnessapp.network.ApiService;
import com.saif.fitnessapp.network.dto.ActivityResponse;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class ActivityRepository {

    private final ApiService apiService;

    @Inject
    public ActivityRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    /**
     * Fetch full activity details by ID
     * Returns complete ActivityResponse including additionalMetrics
     */
    public void getActivityById(String activityId, ActivityCallback callback) {
        apiService.getActivityById(activityId).enqueue(new Callback<ActivityResponse>() {
            @Override
            public void onResponse(Call<ActivityResponse> call, Response<ActivityResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load activity details");
                }
            }

            @Override
            public void onFailure(Call<ActivityResponse> call, Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    /**
     * Callback interface for activity fetching
     */
    public interface ActivityCallback {
        void onSuccess(ActivityResponse activity);
        void onError(String error);
    }
}