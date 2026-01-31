package com.saif.fitnessapp.activity;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.PagingData;

import com.saif.fitnessapp.network.dto.ActivityRequest;
import com.saif.fitnessapp.network.dto.ActivityResponse;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

import kotlinx.coroutines.flow.Flow;

@HiltViewModel
public class ActivityViewModel extends ViewModel {
    private final ActivityRepository activityRepository;

    @Inject
    public ActivityViewModel(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    public LiveData<ActivityResponse> trackActivity(ActivityRequest request) {
        return activityRepository.trackActivity(request);
    }

    public Flow<PagingData<ActivityResponse>> getActivities(String userId) {
        return activityRepository.getActivitiesFlow(userId);
    }

    // Add this method to convert Flow to LiveData
    public LiveData<PagingData<ActivityResponse>> getActivitiesLiveData(String userId) {
        return androidx.lifecycle.FlowLiveDataConversions.asLiveData(
                activityRepository.getActivitiesFlow(userId)
        );
    }
}