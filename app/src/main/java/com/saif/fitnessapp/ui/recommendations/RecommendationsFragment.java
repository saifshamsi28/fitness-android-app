package com.saif.fitnessapp.ui.recommendations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.saif.fitnessapp.R;
import com.saif.fitnessapp.auth.TokenManager;
import com.saif.fitnessapp.recommendation.RecommendationViewModel;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

@AndroidEntryPoint
public class RecommendationsFragment extends Fragment {

    @Inject
    TokenManager tokenManager;

    private RecommendationViewModel viewModel;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private LinearLayout emptyStateContainer;
    private RecommendationAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
                            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recommendations, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recommendations_recycler);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        emptyStateContainer = view.findViewById(R.id.empty_state_container);

        viewModel = new ViewModelProvider(this).get(RecommendationViewModel.class);

        // Setup RecyclerView
        adapter = new RecommendationAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        loadRecommendations();
    }

    private void loadRecommendations() {
        String userId = tokenManager.getUserId();
        if (userId != null) {
            progressBar.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);

            viewModel.getUserRecommendations(userId).observe(getViewLifecycleOwner(), recommendations -> {
                progressBar.setVisibility(View.GONE);

                if (recommendations != null && !recommendations.isEmpty()) {
                    adapter.submitList(recommendations);
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyStateContainer.setVisibility(View.GONE);
                } else {
                    emptyStateText.setText("No recommendations available yet.\nComplete some activities to get personalized recommendations.");
                    emptyStateContainer.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
            });
        }
    }
}
