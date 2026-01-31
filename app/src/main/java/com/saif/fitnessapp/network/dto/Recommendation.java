package com.saif.fitnessapp.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Recommendation {
    @SerializedName("id")
    private String id;

    @SerializedName("userId")
    private String userId;

    @SerializedName("activityId")
    private String activityId;

    @SerializedName("activityType")
    private String activityType;

    @SerializedName("recommendation")
    private String recommendation;

    @SerializedName("improvements")
    private List<String> improvements;

    @SerializedName("suggestions")
    private List<String> suggestions;

    @SerializedName("safety")
    private List<String> safety;

    @SerializedName("createdAt")
    private String createdAt;

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getActivityId() { return activityId; }
    public String getActivityType() { return activityType; }
    public String getRecommendation() { return recommendation; }
    public List<String> getImprovements() { return improvements; }
    public List<String> getSuggestions() { return suggestions; }
    public List<String> getSafety() { return safety; }
    public String getCreatedAt() { return createdAt; }
}
