package com.saif.fitnessapp.ui.activity;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.saif.fitnessapp.network.dto.ActivityResponse;
import com.saif.fitnessapp.network.dto.Recommendation;
import com.saif.fitnessapp.repository.ActivityRepository;
import com.saif.fitnessapp.repository.RecommendationRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ActivityDetailViewModel extends ViewModel {

    private final RecommendationRepository recommendationRepository;
    private final ActivityRepository activityRepository;

    private final MutableLiveData<Recommendation> recommendationLiveData = new MutableLiveData<>();
    private final MutableLiveData<ActivityResponse> activityLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>();

    @Inject
    public ActivityDetailViewModel(
            RecommendationRepository recommendationRepository,
            ActivityRepository activityRepository
    ) {
        this.recommendationRepository = recommendationRepository;
        this.activityRepository = activityRepository;
    }

    /**
     * Fetch full activity details including additionalMetrics
     */
    public LiveData<ActivityResponse> getActivityDetails(String activityId) {
        activityRepository.getActivityById(activityId, new ActivityRepository.ActivityCallback() {
            @Override
            public void onSuccess(ActivityResponse activity) {
                activityLiveData.postValue(activity);
            }

            @Override
            public void onError(String error) {
                errorLiveData.postValue("Failed to load activity: " + error);
            }
        });

        return activityLiveData;
    }

    /**
     * Fetch AI recommendation for activity
     */
    public LiveData<Recommendation> getRecommendation(String activityId) {
        loadingLiveData.setValue(true);

        recommendationRepository.getActivityRecommendation(
                activityId,
                new RecommendationRepository.RecommendationCallback() {
                    @Override
                    public void onSuccess(Recommendation recommendation) {
                        loadingLiveData.postValue(false);
                        recommendationLiveData.postValue(recommendation);
                    }

                    @Override
                    public void onError(String error) {
                        loadingLiveData.postValue(false);
                        errorLiveData.postValue(error);
                    }
                }
        );

        return recommendationLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }
}