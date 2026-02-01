package com.saif.fitnessapp.ui.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.saif.fitnessapp.MainActivity;
import com.saif.fitnessapp.R;
import com.saif.fitnessapp.activity.ActivityViewModel;
import com.saif.fitnessapp.auth.TokenManager;
import com.saif.fitnessapp.ui.TitleController;

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

        adapter.addLoadStateListener(loadStates -> {
            boolean isRefreshing =
                    loadStates.getRefresh() instanceof androidx.paging.LoadState.Loading;

            swipeRefreshLayout.setRefreshing(isRefreshing);

            return null;
        });


        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> {
            adapter.refresh();
        });


        String userId = tokenManager.getUserId();
        if (userId != null) {
            activityViewModel.getActivitiesLiveData(userId)
                    .observe(getViewLifecycleOwner(), pagingData ->
                            adapter.submitData(getViewLifecycleOwner().getLifecycle(), pagingData));
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        if (requireActivity() instanceof TitleController) {
            ((TitleController) requireActivity()).setTitle("Activities");
        }
    }



}