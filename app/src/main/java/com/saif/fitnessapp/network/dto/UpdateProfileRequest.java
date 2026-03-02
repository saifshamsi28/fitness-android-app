package com.saif.fitnessapp.network.dto;

import com.google.gson.annotations.SerializedName;

public class UpdateProfileRequest {
    @SerializedName("firstName") private String firstName;
    @SerializedName("lastName")  private String lastName;
    @SerializedName("email")     private String email;

    public UpdateProfileRequest(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName  = lastName;
        this.email     = email;
    }
}
