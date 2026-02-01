package com.saif.fitnessapp.activity;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;

import com.saif.fitnessapp.network.ApiService;
import com.saif.fitnessapp.network.dto.ActivityRequest;
import com.saif.fitnessapp.network.dto.ActivityResponse;

import javax.inject.Inject;

import kotlinx.coroutines.flow.Flow;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityRepository {
    private final ApiService apiService;

    @Inject
    public ActivityRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public LiveData<ActivityResponse> trackActivity(ActivityRequest request) {
        MutableLiveData<ActivityResponse> liveData = new MutableLiveData<>();

        apiService.trackActivity(request).enqueue(new Callback<ActivityResponse>() {
            @Override
            public void onResponse(Call<ActivityResponse> call,
                                   Response<ActivityResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    liveData.postValue(response.body());
                } else {
                    liveData.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<ActivityResponse> call, Throwable t) {
                t.printStackTrace();
                liveData.postValue(null);
            }
        });

        return liveData;
    }


    public Flow<PagingData<ActivityResponse>> getActivitiesFlow(String userId) {

        return new Pager<>(
                new PagingConfig(
                        10
                ),
                () -> new ActivityPagingSource(apiService, userId)
        ).getFlow();
    }


}
