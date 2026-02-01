package com.saif.fitnessapp.ui.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.saif.fitnessapp.R;
import com.saif.fitnessapp.network.dto.ActivityResponse;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public class ActivityAdapter extends PagingDataAdapter<ActivityResponse, ActivityAdapter.ActivityViewHolder> {

    public ActivityAdapter() {
        super(new DiffUtil.ItemCallback<ActivityResponse>() {
            @Override
            public boolean areItemsTheSame(
                    @NonNull ActivityResponse oldItem,
                    @NonNull ActivityResponse newItem
            ) {
                if (oldItem.getId() == null || newItem.getId() == null) {
                    return false;
                }
                return oldItem.getId().equals(newItem.getId());
            }


            @Override
            public boolean areContentsTheSame(@NonNull ActivityResponse oldItem, @NonNull ActivityResponse newItem) {
                // Compare all relevant fields to check if content has changed
                return Objects.equals(oldItem.getId(), newItem.getId()) &&
                        Objects.equals(oldItem.getActivityType(), newItem.getActivityType()) &&
                        Objects.equals(oldItem.getDuration(), newItem.getDuration()) &&
                        Objects.equals(oldItem.getCaloriesBurned(), newItem.getCaloriesBurned()) &&
                        Objects.equals(oldItem.getStartTime(), newItem.getStartTime()) &&
                        Objects.equals(oldItem.getUserId(), newItem.getUserId()) &&
                        Objects.equals(oldItem.getAdditionalMetrics(), newItem.getAdditionalMetrics())
                        ;

            }
        });
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        ActivityResponse activity = getItem(position);
        if (activity != null) {
            holder.bind(activity);
        }
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        private final TextView activityType;
        private final TextView duration;
        private final TextView calories;
        private final TextView startTime;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            activityType = itemView.findViewById(R.id.activity_type);
            duration = itemView.findViewById(R.id.duration);
            calories = itemView.findViewById(R.id.calories);
            startTime = itemView.findViewById(R.id.start_time);
        }

        public void bind(ActivityResponse activity) {
            activityType.setText(activity.getActivityType());
            duration.setText(activity.getDuration() + " min");
            calories.setText(activity.getCaloriesBurned() + " kcal");
            startTime.setText(formatDateTime(activity.getStartTime()));
        }
    }

    public static String formatDateTime(String isoTime) {
        try {
            SimpleDateFormat input = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS",
                    Locale.US
            );
            SimpleDateFormat output = new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.US
            );
            return output.format(input.parse(isoTime));
        } catch (Exception e) {
            return isoTime;
        }
    }

}
