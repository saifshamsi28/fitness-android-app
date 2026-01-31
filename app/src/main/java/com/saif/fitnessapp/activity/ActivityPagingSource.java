package com.saif.fitnessapp.activity;

import androidx.annotation.NonNull;
import androidx.paging.PagingState;
import androidx.paging.rxjava3.RxPagingSource;

import com.saif.fitnessapp.network.ApiService;
import com.saif.fitnessapp.network.dto.ActivityResponse;

import java.util.List;

import io.reactivex.rxjava3.core.Single;
import retrofit2.Response;

public class ActivityPagingSource
        extends RxPagingSource<Integer, ActivityResponse> {

    private static final int STARTING_PAGE_INDEX = 0;
    private static final int PAGE_SIZE = 10;

    private final ApiService apiService;
    private final String userId;

    public ActivityPagingSource(ApiService apiService, String userId) {
        this.apiService = apiService;
        this.userId = userId;
    }

    @NonNull
    @Override
    public Single<LoadResult<Integer, ActivityResponse>> loadSingle(
            @NonNull LoadParams<Integer> params
    ) {
        int pageIndex = params.getKey() != null
                ? params.getKey()
                : STARTING_PAGE_INDEX;

        return Single.fromCallable(() -> {
            Response<List<ActivityResponse>> response =
                    apiService.getActivities(pageIndex, PAGE_SIZE, userId)
                            .execute();

            if (response.isSuccessful() && response.body() != null) {
                List<ActivityResponse> data = response.body();

                Integer prevKey = pageIndex == STARTING_PAGE_INDEX
                        ? null
                        : pageIndex - 1;

                Integer nextKey = data.isEmpty()
                        ? null
                        : pageIndex + 1;

                return new LoadResult.Page<>(data, prevKey, nextKey);
            } else {
                return new LoadResult.Error<>(
                        new Exception("API error")
                );
            }
        });
    }

    @Override
    public Integer getRefreshKey(
            @NonNull PagingState<Integer, ActivityResponse> state
    ) {
        Integer anchor = state.getAnchorPosition();
        if (anchor == null) return null;

        LoadResult.Page<Integer, ActivityResponse> page =
                state.closestPageToPosition(anchor);

        if (page == null) return null;
        if (page.getPrevKey() != null) return page.getPrevKey() + 1;
        if (page.getNextKey() != null) return page.getNextKey() - 1;
        return null;
    }
}
