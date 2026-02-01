package com.saif.fitnessapp.network.dto;

import com.google.gson.annotations.SerializedName;

public class UserResponse {
    @SerializedName("id")
    private String id;

    @SerializedName("keycloakId")
    private String keycloakId;

    @SerializedName("email")
    private String email;

    @SerializedName("firstName")
    private String firstName;

    @SerializedName("lastName")
    private String lastName;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    // Getters
    public String getId() { return id; }
    public String getKeycloakId() { return keycloakId; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
