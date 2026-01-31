package com.saif.fitnessapp.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class ActivityRequest {
    @SerializedName("userId")
    private String userId;

    @SerializedName("activityType")
    private String activityType;

    @SerializedName("duration")
    private Integer duration;

    @SerializedName("caloriesBurned")
    private Integer caloriesBurned;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("additionalMetrics")
    private Map<String, Object> additionalMetrics;

    public ActivityRequest(String userId, String activityType, Integer duration, 
                          Integer caloriesBurned, String startTime, Map<String, Object> additionalMetrics) {
        this.userId = userId;
        this.activityType = activityType;
        this.duration = duration;
        this.caloriesBurned = caloriesBurned;
        this.startTime = startTime;
        this.additionalMetrics = additionalMetrics;
    }

    // Getters
    public String getUserId() { return userId; }
    public String getActivityType() { return activityType; }
    public Integer getDuration() { return duration; }
    public Integer getCaloriesBurned() { return caloriesBurned; }
    public String getStartTime() { return startTime; }
    public Map<String, Object> getAdditionalMetrics() { return additionalMetrics; }
}
