package com.saif.fitnessapp.user;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.saif.fitnessapp.network.dto.ChangePasswordRequest;
import com.saif.fitnessapp.network.dto.UpdateProfileRequest;
import com.saif.fitnessapp.network.dto.UserResponse;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

@HiltViewModel
public class UserViewModel extends ViewModel {
    private final UserRepository userRepository;

    @Inject
    public UserViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LiveData<UserResponse> getUserProfile(String userId) {
        return userRepository.fetchUser(userId);
    }

    public LiveData<Boolean> validateUser(String userId) {
        return userRepository.validateUser(userId);
    }

    public void updateUser(String userId, UpdateProfileRequest request, UserRepository.UpdateCallback callback) {
        userRepository.updateUser(userId, request, callback);
    }

    public void changePassword(String userId, ChangePasswordRequest request, UserRepository.ActionCallback callback) {
        userRepository.changePassword(userId, request, callback);
    }
}
