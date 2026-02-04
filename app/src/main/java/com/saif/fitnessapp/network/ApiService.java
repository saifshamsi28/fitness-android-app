package com.saif.fitnessapp.network;

import com.saif.fitnessapp.network.dto.ActivityRequest;
import com.saif.fitnessapp.network.dto.ActivityResponse;
import com.saif.fitnessapp.network.dto.Recommendation;
import com.saif.fitnessapp.network.dto.SignupRequest;
import com.saif.fitnessapp.network.dto.SignupResponse;
import com.saif.fitnessapp.network.dto.UserResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // --- AUTH SERVICE APIs ---
    @POST("api/auth/signup")
    Call<SignupResponse> signup(@Body SignupRequest request);

    // --- USER SERVICE APIs ---

    @GET("api/users/{userId}")
    Call<UserResponse> getUser(@Path("userId") String userId);

    @GET("api/users/{userId}/validate")
    Call<Boolean> validateUser(@Path("userId") String userId);

    // --- ACTIVITY SERVICE APIs ---

    @POST("api/activities/track")
    Call<ActivityResponse> trackActivity(@Body ActivityRequest request);

    @GET("api/activities")
    Call<List<ActivityResponse>> getActivities(
            @Query("page") int page,
            @Query("size") int size,
            @Query("userId") String userId
    );

    // --- AI SERVICE APIs ---

    @GET("api/recommendations/user/{userId}")
    Call<List<Recommendation>> getUserRecommendations(@Path("userId") String userId);

    @GET("api/recommendations/activity/{activityId}")
    Call<Recommendation> getActivityRecommendation(@Path("activityId") String activityId);

    @GET("/api/activities/{activityId}")
    Call<ActivityResponse> getActivityById(@Path("activityId") String activityId);

}