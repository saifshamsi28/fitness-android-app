package com.saif.fitnessapp.user;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.saif.fitnessapp.network.ApiService;
import com.saif.fitnessapp.network.dto.UserResponse;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {
    private final ApiService apiService;

    @Inject
    public UserRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public LiveData<UserResponse> fetchUser(String userId) {
        MutableLiveData<UserResponse> liveData = new MutableLiveData<>();

        apiService.getUser(userId).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    liveData.postValue(response.body());
                } else {
                    liveData.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                t.printStackTrace();
                liveData.postValue(null);
            }
        });

        return liveData;
    }

    public LiveData<Boolean> validateUser(String userId) {
        MutableLiveData<Boolean> liveData = new MutableLiveData<>();

        apiService.validateUser(userId).enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                if (response.isSuccessful()) {
                    liveData.postValue(response.body() != null && response.body());
                } else {
                    liveData.postValue(false);
                }
            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable t) {
                t.printStackTrace();
                liveData.postValue(false);
            }
        });

        return liveData;
    }
}
