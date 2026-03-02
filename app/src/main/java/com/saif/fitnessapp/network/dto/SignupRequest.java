package com.saif.fitnessapp.network.dto;

import com.google.gson.annotations.SerializedName;

public class SignupRequest {
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("password")
    private String password;
    
    @SerializedName("firstName")
    private String firstName;
    
    @SerializedName("lastName")
    private String lastName;

    @SerializedName("username")
    private String username;

    public SignupRequest(String email, String password, String firstName, String lastName, String username) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
    }

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}