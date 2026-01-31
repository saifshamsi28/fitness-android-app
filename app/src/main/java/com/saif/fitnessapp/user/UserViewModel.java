package com.saif.fitnessapp.user;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

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
}
