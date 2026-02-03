package com.saif.fitnessapp.ui.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.saif.fitnessapp.R;
import com.saif.fitnessapp.network.dto.ActivityResponse;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public class ActivityAdapter extends PagingDataAdapter<ActivityResponse, ActivityAdapter.ActivityViewHolder> {

    private OnActivityClickListener clickListener;

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
                return Objects.equals(oldItem.getId(), newItem.getId()) &&
                        Objects.equals(oldItem.getActivityType(), newItem.getActivityType()) &&
                        Objects.equals(oldItem.getDuration(), newItem.getDuration()) &&
                        Objects.equals(oldItem.getCaloriesBurned(), newItem.getCaloriesBurned()) &&
                        Objects.equals(oldItem.getStartTime(), newItem.getStartTime()) &&
                        Objects.equals(oldItem.getUserId(), newItem.getUserId()) &&
                        Objects.equals(oldItem.getAdditionalMetrics(), newItem.getAdditionalMetrics());
            }
        });
    }

    /**
     * Set click listener for activity items
     */
    public void setOnActivityClickListener(OnActivityClickListener listener) {
        this.clickListener = listener;
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
            holder.bind(activity, clickListener);
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

        public void bind(ActivityResponse activity, OnActivityClickListener listener) {
            activityType.setText(activity.getActivityType());
            duration.setText(activity.getDuration() + " min");
            calories.setText(activity.getCaloriesBurned() + " kcal");
            startTime.setText(formatDateTime(activity.getStartTime()));

            // Set click listener for the entire item
            itemView.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(v);

                Bundle bundle = new Bundle();
                bundle.putString("activity_id", activity.getId());
                bundle.putString("activity_type", activity.getActivityType());
                bundle.putInt("duration", activity.getDuration());
                bundle.putInt("calories", activity.getCaloriesBurned());
                bundle.putString("start_time", activity.getStartTime());

                navController.navigate(
                        R.id.action_activityFragment_to_activityDetailFragment,
                        bundle
                );
            });
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

    /**
     * Callback interface for activity item clicks
     */
    public interface OnActivityClickListener {
        void onActivityClick(ActivityResponse activity);
    }
}