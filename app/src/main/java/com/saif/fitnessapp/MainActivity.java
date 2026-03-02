package com.saif.fitnessapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.saif.fitnessapp.ui.TitleController;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements TitleController {

    private BottomNavigationView bottomNavigationView;
    private NavController navController;
    // Guard flag to prevent listener loop when programmatically updating the bottom nav
    private boolean isUpdatingBottomNav = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);

        navController = navHostFragment.getNavController();

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        // Sync bottom nav selection when destination changes (e.g. back button, programmatic nav)
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destId = destination.getId();
            int tabId;
            if (destId == R.id.navigation_activity || destId == R.id.activityDetailFragment) {
                tabId = R.id.navigation_activity;
            } else if (destId == R.id.navigation_recommendations) {
                tabId = R.id.navigation_recommendations;
            } else if (destId == R.id.navigation_profile) {
                tabId = R.id.navigation_profile;
            } else {
                // home tab covers home + addActivity
                tabId = R.id.navigation_home;
            }
            // Use flag so our listener doesn't trigger a navigation call
            isUpdatingBottomNav = true;
            bottomNavigationView.setSelectedItemId(tabId);
            isUpdatingBottomNav = false;
        });

        // Handle bottom nav item clicks with proper back-stack management
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (isUpdatingBottomNav) {
                // Programmatic update — don't navigate again
                return true;
            }
            int itemId = item.getItemId();
            // Pop up to (but not including) the tab's root destination so that any
            // sub-fragments (e.g. activityDetailFragment) are removed before navigating.
            NavOptions navOptions = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(itemId, false)
                    .build();
            try {
                navController.navigate(itemId, null, navOptions);
            } catch (Exception ignored) {
                // Destination not found — ignore
            }
            return true;
        });
    }

    @Override
    public void setTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getSupportActionBar() == null) {
            return;
        }
        getSupportActionBar().setTitle("Fitness Tracker");
    }
}
