package com.saif.fitnessapp.recommendation;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.saif.fitnessapp.network.ApiService;
import com.saif.fitnessapp.network.dto.Recommendation;

import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecommendationRepository {
    private final ApiService apiService;

    @Inject
    public RecommendationRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public LiveData<List<Recommendation>> getUserRecommendations(String userId) {
        MutableLiveData<List<Recommendation>> liveData = new MutableLiveData<>();

        apiService.getUserRecommendations(userId).enqueue(new Callback<List<Recommendation>>() {
            @Override
            public void onResponse(Call<List<Recommendation>> call, Response<List<Recommendation>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    liveData.postValue(response.body());
                } else {
                    liveData.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<Recommendation>> call, Throwable t) {
                t.printStackTrace();
                liveData.postValue(null);
            }
        });

        return liveData;
    }

    public LiveData<Recommendation> getActivityRecommendation(String activityId) {
        MutableLiveData<Recommendation> liveData = new MutableLiveData<>();

        apiService.getActivityRecommendation(activityId).enqueue(new Callback<Recommendation>() {
            @Override
            public void onResponse(Call<Recommendation> call, Response<Recommendation> response) {
                if (response.isSuccessful() && response.body() != null) {
                    liveData.postValue(response.body());
                } else {
                    liveData.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<Recommendation> call, Throwable t) {
                t.printStackTrace();
                liveData.postValue(null);
            }
        });

        return liveData;
    }
}
