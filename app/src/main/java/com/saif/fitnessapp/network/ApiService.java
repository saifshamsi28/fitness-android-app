package com.saif.fitnessapp.network;

import com.saif.fitnessapp.network.dto.ActivityRequest;
import com.saif.fitnessapp.network.dto.ActivityResponse;
import com.saif.fitnessapp.network.dto.Recommendation;
import com.saif.fitnessapp.network.dto.UserResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    
    // User Service APIs
    @GET("user-service/api/users/{userId}")
    Call<UserResponse> getUser(@Path("userId") String userId);

    @GET("user-service/api/users/{userId}/validate")
    Call<Boolean> validateUser(@Path("userId") String userId);

    // Activity Service APIs
    @POST("activity-service/api/activities/track")
    Call<ActivityResponse> trackActivity(@Body ActivityRequest request);

    @GET("activity-service/api/activities")
    Call<List<ActivityResponse>> getActivities(
            @Query("page") int page,
            @Query("size") int size,
            @Query("userId") String userId
    );

    // AI Service APIs
    @GET("ai-service/api/recommendations/user/{userId}")
    Call<List<Recommendation>> getUserRecommendations(@Path("userId") String userId);

    @GET("ai-service/api/recommendations/activity/{activityId}")
    Call<Recommendation> getActivityRecommendation(@Path("activityId") String activityId);
}
