package com.saif.fitnessapp.ui.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.saif.fitnessapp.R;
import com.saif.fitnessapp.activity.ActivityViewModel;
import com.saif.fitnessapp.auth.TokenManager;
import com.saif.fitnessapp.network.dto.ActivityResponse;
import com.saif.fitnessapp.ui.TitleController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    // Search components
    private SearchView searchView;
    private RecyclerView searchResultsRecycler;
    private LinearLayout searchEmptyState;
    private SearchResultAdapter searchResultAdapter;

    // Full list loaded for search filtering
    private List<ActivityResponse> allActivities = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_activity, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView        = view.findViewById(R.id.activities_recycler);
        swipeRefreshLayout  = view.findViewById(R.id.swipe_refresh);
        searchView          = view.findViewById(R.id.search_view);
        searchResultsRecycler = view.findViewById(R.id.search_results_recycler);
        searchEmptyState    = view.findViewById(R.id.search_empty_state);

        activityViewModel = new ViewModelProvider(this).get(ActivityViewModel.class);

        setupBrowseRecyclerView();
        setupSearchRecyclerView();
        setupSearchView();

        String userId = tokenManager.getUserId();
        if (userId != null) {
            // Load paging stream for browse mode
            activityViewModel.getActivitiesLiveData(userId)
                    .observe(getViewLifecycleOwner(), pagingData ->
                            adapter.submitData(getViewLifecycleOwner().getLifecycle(), pagingData));

            // Pre-fetch full list for instant search
            activityViewModel.getAllActivitiesForSearch(userId)
                    .observe(getViewLifecycleOwner(), activities -> {
                        if (activities != null) {
                            allActivities = activities;
                        }
                    });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Browse mode (paging)
    // ─────────────────────────────────────────────────────────────────────────

    private void setupBrowseRecyclerView() {
        adapter = new ActivityAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnActivityClickListener(this::navigateToActivityDetail);

        adapter.addLoadStateListener(loadStates -> {
            boolean isRefreshing =
                    loadStates.getRefresh() instanceof androidx.paging.LoadState.Loading;
            swipeRefreshLayout.setRefreshing(isRefreshing);
            return null;
        });

        swipeRefreshLayout.setOnRefreshListener(() -> adapter.refresh());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Search mode (list)
    // ─────────────────────────────────────────────────────────────────────────

    private void setupSearchRecyclerView() {
        searchResultAdapter = new SearchResultAdapter();
        searchResultsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        searchResultsRecycler.setAdapter(searchResultAdapter);
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    showBrowseMode();
                } else {
                    performSearch(newText);
                }
                return true;
            }
        });
    }

    private void performSearch(String rawQuery) {
        String q = rawQuery.toLowerCase(Locale.ROOT).trim();
        if (TextUtils.isEmpty(q)) {
            showBrowseMode();
            return;
        }

        List<ActivityResponse> results = new ArrayList<>();
        for (ActivityResponse a : allActivities) {
            if (matchesQuery(a, q)) {
                results.add(a);
            }
        }

        showSearchMode();
        if (results.isEmpty()) {
            searchResultsRecycler.setVisibility(View.GONE);
            searchEmptyState.setVisibility(View.VISIBLE);
        } else {
            searchEmptyState.setVisibility(View.GONE);
            searchResultsRecycler.setVisibility(View.VISIBLE);
            searchResultAdapter.setItems(results);
        }
    }

    /**
     * Multi-field matching: type, duration, calories, date, distance, notes.
     */
    private boolean matchesQuery(ActivityResponse a, String q) {
        if (a.getActivityType() != null &&
                a.getActivityType().toLowerCase(Locale.ROOT).replace("_", " ").contains(q)) return true;
        if (a.getDuration() != null && a.getDuration().toString().contains(q)) return true;
        if (a.getCaloriesBurned() != null && a.getCaloriesBurned().toString().contains(q)) return true;
        if (a.getStartTime() != null && a.getStartTime().toLowerCase(Locale.ROOT).contains(q)) return true;
        if (a.getId() != null && a.getId().toLowerCase(Locale.ROOT).contains(q)) return true;
        // Additional metrics map — search values
        if (a.getAdditionalMetrics() != null) {
            for (Object val : a.getAdditionalMetrics().values()) {
                if (val != null && val.toString().toLowerCase(Locale.ROOT).contains(q)) return true;
            }
        }
        return false;
    }

    private void showBrowseMode() {
        swipeRefreshLayout.setVisibility(View.VISIBLE);
        searchResultsRecycler.setVisibility(View.GONE);
        searchEmptyState.setVisibility(View.GONE);
    }

    private void showSearchMode() {
        swipeRefreshLayout.setVisibility(View.GONE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation helper
    // ─────────────────────────────────────────────────────────────────────────

    private void navigateToActivityDetail(ActivityResponse activity) {
        Bundle bundle = new Bundle();
        bundle.putString("activity_id",   activity.getId());
        bundle.putString("activity_type", activity.getActivityType());
        bundle.putInt("duration",  activity.getDuration()       != null ? activity.getDuration()       : 0);
        bundle.putInt("calories",  activity.getCaloriesBurned() != null ? activity.getCaloriesBurned() : 0);
        bundle.putString("start_time", activity.getStartTime());

        Navigation.findNavController(requireView())
                .navigate(R.id.action_activityFragment_to_activityDetailFragment, bundle);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Search result adapter (plain ListAdapter, reuses item_activity layout)
    // ─────────────────────────────────────────────────────────────────────────

    private class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.VH> {

        private List<ActivityResponse> items = new ArrayList<>();

        void setItems(List<ActivityResponse> newItems) {
            items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_activity, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ActivityResponse a = items.get(position);

            holder.emoji.setText(ActivityAdapter.getActivityEmoji(a.getActivityType()));
            holder.type .setText(ActivityAdapter.formatActivityName(a.getActivityType()));

            // Duration
            if (a.getDuration() != null) {
                holder.duration.setText(a.getDuration() + " min");
            } else {
                holder.duration.setText("-- min");
            }

            // Calories
            if (a.getCaloriesBurned() != null) {
                holder.calories.setText(a.getCaloriesBurned() + " kcal");
            } else {
                holder.calories.setText("-- kcal");
            }

            // Date
            if (a.getStartTime() != null && a.getStartTime().length() >= 10) {
                holder.date.setText(a.getStartTime().substring(0, 10));
            } else {
                holder.date.setText("");
            }

            holder.itemView.setOnClickListener(v -> navigateToActivityDetail(a));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView emoji, type, duration, calories, date;
            VH(@NonNull View itemView) {
                super(itemView);
                emoji    = itemView.findViewById(R.id.activity_emoji);
                type     = itemView.findViewById(R.id.activity_type);
                duration = itemView.findViewById(R.id.duration);
                calories = itemView.findViewById(R.id.calories);
                date     = itemView.findViewById(R.id.start_time);
            }
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
