package com.saif.fitnessapp.network.dto;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class SignupResponse {
    
    @SerializedName("keycloakId")
    private String keycloakId;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("firstName")
    private String firstName;
    
    @SerializedName("lastName")
    private String lastName;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("success")
    private boolean success;

    // Getters
    public String getKeycloakId() {
        return keycloakId;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return success;
    }

    // Setters
    public void setKeycloakId(String keycloakId) {
        this.keycloakId = keycloakId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @NonNull
    @Override
    public String toString() {
        return "SignupResponse{" +
                "keycloakId='" + keycloakId + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", message='" + message + '\'' +
                ", success=" + success +
                '}';
    }
}