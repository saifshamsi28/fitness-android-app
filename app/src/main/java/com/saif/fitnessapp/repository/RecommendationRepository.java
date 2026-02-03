package com.saif.fitnessapp.repository;

import android.util.Log;

import com.saif.fitnessapp.network.ApiService;
import com.saif.fitnessapp.network.dto.Recommendation;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class RecommendationRepository {

    private static final String TAG = "RecommendationRepository";
    private final ApiService apiService;

    @Inject
    public RecommendationRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    /**
     * Get AI recommendation for a specific activity
     */
    public void getActivityRecommendation(String activityId, RecommendationCallback callback) {
        Log.d(TAG, "Fetching recommendation for activity: " + activityId);

        apiService.getActivityRecommendation(activityId).enqueue(new Callback<Recommendation>() {
            @Override
            public void onResponse(Call<Recommendation> call, Response<Recommendation> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Recommendation fetched successfully");
                    callback.onSuccess(response.body());
                } else {
                    Log.e(TAG, "Failed to fetch recommendation. Code: " + response.code());
                    callback.onError("Failed to load AI insights. Please try again.");
                }
            }

            @Override
            public void onFailure(Call<Recommendation> call, Throwable t) {
                Log.e(TAG, "Network error fetching recommendation: " + t.getMessage(), t);
                callback.onError("Network error. Please check your connection.");
            }
        });
    }

    /**
     * Callback interface for recommendation fetching
     */
    public interface RecommendationCallback {
        void onSuccess(Recommendation recommendation);
        void onError(String error);
    }
}