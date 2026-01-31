package com.saif.fitnessapp.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class ActivityResponse {
    @SerializedName("id")
    private String id;

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

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getActivityType() { return activityType; }
    public Integer getDuration() { return duration; }
    public Integer getCaloriesBurned() { return caloriesBurned; }
    public String getStartTime() { return startTime; }
    public Map<String, Object> getAdditionalMetrics() { return additionalMetrics; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
