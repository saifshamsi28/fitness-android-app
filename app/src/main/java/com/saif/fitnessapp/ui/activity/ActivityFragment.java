package com.saif.fitnessapp.ui.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagingData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.saif.fitnessapp.R;
import com.saif.fitnessapp.activity.ActivityViewModel;
import com.saif.fitnessapp.auth.TokenManager;
import com.saif.fitnessapp.network.dto.ActivityResponse;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

@AndroidEntryPoint
public class ActivityFragment extends Fragment {

    @Inject
    TokenManager tokenManager;

    private ActivityViewModel activityViewModel;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ActivityAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_activity, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.activities_recycler);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);

        activityViewModel = new ViewModelProvider(this).get(ActivityViewModel.class);

        // Setup RecyclerView
        adapter = new ActivityAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadActivities();
            swipeRefreshLayout.setRefreshing(false);
        });

        loadActivities();
    }

    private void loadActivities() {
        String userId = tokenManager.getUserId();
        if (userId != null) {
            // Use getActivitiesLiveData instead of getActivities
            activityViewModel.getActivitiesLiveData(userId).observe(getViewLifecycleOwner(),
                    pagingData -> adapter.submitData(getViewLifecycleOwner().getLifecycle(), pagingData));
        }
    }
}