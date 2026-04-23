package com.saif.fitnessapp.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.saif.fitnessapp.R;
import com.saif.fitnessapp.activity.ActivityViewModel;
import com.saif.fitnessapp.auth.TokenManager;
import com.saif.fitnessapp.network.dto.ActivityResponse;
import com.saif.fitnessapp.ui.TitleController;
import com.saif.fitnessapp.user.UserViewModel;
import com.saif.fitnessapp.utils.ThemeManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    @Inject
    TokenManager tokenManager;

    private UserViewModel userViewModel;
    private ActivityViewModel activityViewModel;

    private TextView greetingText;
    private TextView welcomeText;
    private TextView userEmailText;
    private TextView dateText;
    private TextView avatarInitials;
    private TextView motivationText;
    private TextView statWorkoutsCount;
    private TextView statCaloriesCount;
    private TextView statDurationCount;
    private View addActivityButton;
    private TextView viewAllActivities;
    private LinearLayout recentActivitiesContainer;
    private MaterialCardView emptyActivitiesCard;

    // Shimmer + content containers
    private ShimmerFrameLayout shimmerHome;
    private LinearLayout statsRow;
    private LinearLayout homeContentSection;
    // Reset per-view so stopShimmer() fires correctly every time the view is (re)created
    private boolean activitiesLoaded = false;

    // Fallback: stop shimmer after 20 s even if API never responds
    private final Handler shimmerTimeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable shimmerTimeoutRunnable;

    private static final String[] MOTIVATIONAL_QUOTES = {
            "Every workout is progress. Keep pushing!",
            "The only bad workout is the one that didn't happen.",
            "Your body can stand almost anything. It's your mind you have to convince.",
            "Fitness is not about being better than someone else. It's about being better than you used to be.",
            "The pain you feel today will be the strength you feel tomorrow.",
            "Don't stop when you're tired. Stop when you're done.",
            "Success starts with self-discipline.",
            "A one-hour workout is only 4% of your day. No excuses.",
            "Strive for progress, not perfection.",
            "Take care of your body. It's the only place you have to live."
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        greetingText = view.findViewById(R.id.greeting_text);
        welcomeText = view.findViewById(R.id.welcome_text);
        userEmailText = view.findViewById(R.id.user_email_text);
        dateText = view.findViewById(R.id.date_text);
        avatarInitials = view.findViewById(R.id.avatar_initials);
        motivationText = view.findViewById(R.id.motivation_text);
        statWorkoutsCount = view.findViewById(R.id.stat_workouts_count);
        statCaloriesCount = view.findViewById(R.id.stat_calories_count);
        statDurationCount = view.findViewById(R.id.stat_duration_count);
        addActivityButton = view.findViewById(R.id.add_activity_button);
        viewAllActivities = view.findViewById(R.id.view_all_activities);
        recentActivitiesContainer = view.findViewById(R.id.recent_activities_container);
        emptyActivitiesCard = view.findViewById(R.id.empty_activities_card);

        // ── Theme toggle button (moon / sun icon) ──
        ImageButton btnThemeToggle = view.findViewById(R.id.btn_theme_toggle);
        if (btnThemeToggle != null) {
            updateThemeIcon(btnThemeToggle);
            btnThemeToggle.setOnClickListener(v -> {
                ThemeManager.toggle(requireContext());
                // Activity will recreate automatically after setDefaultNightMode;
                // update icon immediately for a smooth feel before recreate.
                updateThemeIcon(btnThemeToggle);
            });
        }

        // Shimmer + content containers
        shimmerHome = view.findViewById(R.id.shimmer_home);
        statsRow = view.findViewById(R.id.stats_row);
        homeContentSection = view.findViewById(R.id.home_content_section);

        // Reset flag so shimmer always stops when new data arrives on this view
        activitiesLoaded = false;

        // Start shimmer animation while data loads
        shimmerHome.startShimmer();

        // Safety net: if backend never responds in 20 s, stop shimmer and show empty state
        shimmerTimeoutRunnable = () -> {
            if (!activitiesLoaded && shimmerHome != null) {
                activitiesLoaded = true;
                stopShimmer();
                if (emptyActivitiesCard != null) emptyActivitiesCard.setVisibility(View.VISIBLE);
                if (recentActivitiesContainer != null) recentActivitiesContainer.setVisibility(View.GONE);
            }
        };
        shimmerTimeoutHandler.postDelayed(shimmerTimeoutRunnable, 20_000);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        activityViewModel = new ViewModelProvider(this).get(ActivityViewModel.class);

        // Set time-based greeting
        setTimeBasedGreeting();

        // Set today's date
        setTodayDate();

        // Set motivational quote
        setMotivationalQuote();

        // Load data
        String userId = tokenManager.getUserId();
        if (userId != null) {
            loadUserProfile(userId);
            loadRecentActivities(userId);
        }

        // Button actions
        addActivityButton.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_addActivity)
        );

        viewAllActivities.setOnClickListener(v -> {
            BottomNavigationView nav = requireActivity().findViewById(R.id.bottom_navigation);
            if (nav != null) {
                nav.setSelectedItemId(R.id.navigation_activity);
            }
        });
    }

    private void setTimeBasedGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) {
            greeting = "Good Morning \u2600\uFE0F";
        } else if (hour < 17) {
            greeting = "Good Afternoon \uD83C\uDF24\uFE0F";
        } else {
            greeting = "Good Evening \uD83C\uDF19";
        }
        greetingText.setText(greeting);
    }

    private void setTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM", Locale.getDefault());
        dateText.setText(sdf.format(new Date()));
    }

    private void setMotivationalQuote() {
        int index = (int) (Math.random() * MOTIVATIONAL_QUOTES.length);
        motivationText.setText(MOTIVATIONAL_QUOTES[index]);
    }

    /**
     * Sets the toggle button icon to reflect the NEXT action:
     *  - Moon icon  → currently light mode  → "click to go dark"
     *  - Sun icon   → currently dark mode   → "click to go light"
     */
    private void updateThemeIcon(ImageButton btn) {
        if (ThemeManager.isDark(requireContext())) {
            btn.setImageResource(R.drawable.ic_light_mode);  // show SUN  (switch to light)
        } else {
            btn.setImageResource(R.drawable.ic_dark_mode);   // show MOON (switch to dark)
        }
    }

    private void loadUserProfile(String userId) {
        userViewModel.getUserProfile(userId).observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                welcomeText.setText(user.getFirstName() + "!");
                userEmailText.setText(user.getEmail());

                // Set avatar initials
                String initials = "";
                if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
                    initials += user.getFirstName().charAt(0);
                }
                if (user.getLastName() != null && !user.getLastName().isEmpty()) {
                    initials += user.getLastName().charAt(0);
                }
                avatarInitials.setText(initials.toUpperCase());
            }
        });
    }

    private void loadRecentActivities(String userId) {
        activityViewModel.getRecentActivities(userId).observe(getViewLifecycleOwner(), activities -> {
            // Cancel the fallback timeout — real data (or error) arrived
            shimmerTimeoutHandler.removeCallbacks(shimmerTimeoutRunnable);
            // Always stop shimmer when data arrives (flag guards double-stop on same view)
            if (!activitiesLoaded) {
                activitiesLoaded = true;
                stopShimmer();
            }
            if (activities != null && !activities.isEmpty()) {
                displayStats(activities);
                displayRecentActivities(activities);
                emptyActivitiesCard.setVisibility(View.GONE);
                recentActivitiesContainer.setVisibility(View.VISIBLE);
            } else {
                emptyActivitiesCard.setVisibility(View.VISIBLE);
                recentActivitiesContainer.setVisibility(View.GONE);
            }
        });
    }

    private void stopShimmer() {
        shimmerHome.stopShimmer();
        shimmerHome.setVisibility(View.GONE);
        statsRow.setVisibility(View.VISIBLE);
        homeContentSection.setVisibility(View.VISIBLE);
    }

    private void displayStats(List<ActivityResponse> activities) {
        int totalWorkouts = activities.size();
        int totalCalories = 0;
        int totalDuration = 0;

        for (ActivityResponse a : activities) {
            if (a.getCaloriesBurned() != null) totalCalories += a.getCaloriesBurned();
            if (a.getDuration() != null) totalDuration += a.getDuration();
        }

        statWorkoutsCount.setText(String.valueOf(totalWorkouts));
        statCaloriesCount.setText(formatNumber(totalCalories));
        statDurationCount.setText(String.valueOf(totalDuration));
    }

    private String formatNumber(int number) {
        if (number >= 1000) {
            return String.format(Locale.US, "%.1fk", number / 1000.0);
        }
        return String.valueOf(number);
    }

    private void displayRecentActivities(List<ActivityResponse> activities) {
        recentActivitiesContainer.removeAllViews();

        // Sort newest first by startTime
        List<ActivityResponse> sorted = new ArrayList<>(activities);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        Collections.sort(sorted, (a, b) -> {
            try {
                Date da = sdf.parse(a.getStartTime() != null ? a.getStartTime() : "");
                Date db = sdf.parse(b.getStartTime() != null ? b.getStartTime() : "");
                if (da != null && db != null) return db.compareTo(da);
            } catch (ParseException ignored) {}
            return 0;
        });

        int max = Math.min(sorted.size(), 4);
        for (int i = 0; i < max; i++) {
            View itemView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_recent_activity, recentActivitiesContainer, false);

            ActivityResponse activity = sorted.get(i);

            TextView emoji = itemView.findViewById(R.id.recent_activity_emoji);
            TextView name = itemView.findViewById(R.id.recent_activity_name);
            TextView time = itemView.findViewById(R.id.recent_activity_time);
            TextView duration = itemView.findViewById(R.id.recent_activity_duration);
            TextView calories = itemView.findViewById(R.id.recent_activity_calories);

            emoji.setText(getActivityEmoji(activity.getActivityType()));
            name.setText(formatActivityType(activity.getActivityType()));
            time.setText(formatTimeAgo(activity.getStartTime()));
            duration.setText((activity.getDuration() != null ? activity.getDuration() : 0) + " min");
            calories.setText((activity.getCaloriesBurned() != null ? activity.getCaloriesBurned() : 0) + " kcal");

            // Clickable — navigate to ActivityDetailFragment
            final ActivityResponse act = activity;
            itemView.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("activity_id", act.getId());
                bundle.putString("activity_type", act.getActivityType());
                bundle.putInt("duration", act.getDuration() != null ? act.getDuration() : 0);
                bundle.putInt("calories", act.getCaloriesBurned() != null ? act.getCaloriesBurned() : 0);
                bundle.putString("start_time", act.getStartTime());
                Navigation.findNavController(v)
                        .navigate(R.id.action_home_to_activityDetail, bundle);
            });

            recentActivitiesContainer.addView(itemView);
        }
    }

    private String getActivityEmoji(String type) {
        if (type == null) return "\uD83C\uDFC3";
        return switch (type.toUpperCase()) {
            case "RUNNING" -> "\uD83C\uDFC3";
            case "SWIMMING" -> "\uD83C\uDFCA";
            case "WALKING" -> "\uD83D\uDEB6";
            case "CYCLING" -> "\uD83D\uDEB4";
            case "YOGA" -> "\uD83E\uDDD8";
            case "WEIGHT_LIFTING" -> "\uD83C\uDFCB\uFE0F";
            case "BOXING" -> "\uD83E\uDD4A";
            case "CARDIO" -> "\u2764\uFE0F";
            case "STRETCHING" -> "\uD83E\uDD38";
            default -> "\uD83C\uDFC3";
        };
    }

    private String formatActivityType(String type) {
        if (type == null) return "Activity";
        String formatted = type.replace("_", " ");
        return formatted.substring(0, 1).toUpperCase() + formatted.substring(1).toLowerCase();
    }

    private String formatTimeAgo(String isoTime) {
        if (isoTime == null) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date date = sdf.parse(isoTime);
            if (date == null) return "";

            long diff = System.currentTimeMillis() - date.getTime();
            long minutes = diff / (60 * 1000);
            long hours = diff / (60 * 60 * 1000);
            long days = diff / (24 * 60 * 60 * 1000);

            if (minutes < 1) return "Just now";
            if (minutes < 60) return minutes + "m ago";
            if (hours < 24) return hours + "h ago";
            if (days < 7) return days + "d ago";

            SimpleDateFormat output = new SimpleDateFormat("dd MMM", Locale.US);
            return output.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void onDestroyView() {
        // Cancel any pending shimmer timeout BEFORE super.onDestroyView() so the
        // runnable cannot fire after the view lifecycle ends and touch null views.
        if (shimmerTimeoutRunnable != null) {
            shimmerTimeoutHandler.removeCallbacks(shimmerTimeoutRunnable);
        }
        // Null view refs before super so any late callbacks see null and bail
        shimmerHome = null;
        statsRow = null;
        homeContentSection = null;
        recentActivitiesContainer = null;
        emptyActivitiesCard = null;
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requireActivity() instanceof TitleController) {
            ((TitleController) requireActivity()).setTitle("Fitness Tracker");
        }
        // Re-load activities silently so newly added activities appear after returning
        String userId = tokenManager.getUserId();
        if (userId != null && activitiesLoaded) {
            activityViewModel.getRecentActivities(userId).observe(getViewLifecycleOwner(), activities -> {
                if (emptyActivitiesCard == null || recentActivitiesContainer == null) return;
                if (activities != null && !activities.isEmpty()) {
                    displayStats(activities);
                    displayRecentActivities(activities);
                    emptyActivitiesCard.setVisibility(View.GONE);
                    recentActivitiesContainer.setVisibility(View.VISIBLE);
                } else {
                    emptyActivitiesCard.setVisibility(View.VISIBLE);
                    recentActivitiesContainer.setVisibility(View.GONE);
                }
            });
        }
    }
}
