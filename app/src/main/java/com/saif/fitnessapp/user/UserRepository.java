package com.saif.fitnessapp.user;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.saif.fitnessapp.network.ApiService;
import com.saif.fitnessapp.network.dto.ApiResponse;
import com.saif.fitnessapp.network.dto.ChangePasswordRequest;
import com.saif.fitnessapp.network.dto.UpdateProfileRequest;
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

    // ─── Callback interfaces ───────────────────────────────────────
    public interface UpdateCallback {
        void onSuccess(UserResponse user);
        void onError(String message);
    }
    public interface ActionCallback {
        void onSuccess();
        void onError(String message);
    }

    // ─── Fetch ─────────────────────────────────────────────────────
    public LiveData<UserResponse> fetchUser(String userId) {
        MutableLiveData<UserResponse> liveData = new MutableLiveData<>();
        apiService.getUser(userId).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                liveData.postValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
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
                liveData.postValue(response.isSuccessful() && Boolean.TRUE.equals(response.body()));
            }
            @Override
            public void onFailure(Call<Boolean> call, Throwable t) {
                liveData.postValue(false);
            }
        });
        return liveData;
    }

    // ─── Update profile ────────────────────────────────────────────
    public void updateUser(String userId, UpdateProfileRequest request, UpdateCallback callback) {
        apiService.updateUser(userId, request).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    String msg = response.code() == 401 ? "Unauthorized" :
                                 response.code() == 404 ? "User not found" :
                                 "Failed to update profile (" + response.code() + ")";
                    callback.onError(msg);
                }
            }
            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                callback.onError("Network error. Check your connection.");
            }
        });
    }

    // ─── Change password ───────────────────────────────────────────
    public void changePassword(String userId, ChangePasswordRequest request, ActionCallback callback) {
        apiService.changePassword(userId, request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess();
                } else {
                    String msg = response.code() == 401 ? "Current password is incorrect" :
                                 (response.body() != null && response.body().getMessage() != null)
                                 ? response.body().getMessage()
                                 : "Failed to change password (" + response.code() + ")";
                    callback.onError(msg);
                }
            }
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError("Network error. Check your connection.");
            }
        });
    }
}
