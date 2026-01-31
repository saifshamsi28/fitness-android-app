package com.saif.fitnessapp.recommendation;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.saif.fitnessapp.network.dto.Recommendation;

import dagger.hilt.android.lifecycle.HiltViewModel;

import java.util.List;

import javax.inject.Inject;

@HiltViewModel
public class RecommendationViewModel extends ViewModel {
    private final RecommendationRepository recommendationRepository;

    @Inject
    public RecommendationViewModel(RecommendationRepository recommendationRepository) {
        this.recommendationRepository = recommendationRepository;
    }

    public LiveData<List<Recommendation>> getUserRecommendations(String userId) {
        return recommendationRepository.getUserRecommendations(userId);
    }

    public LiveData<Recommendation> getActivityRecommendation(String activityId) {
        return recommendationRepository.getActivityRecommendation(activityId);
    }
}
