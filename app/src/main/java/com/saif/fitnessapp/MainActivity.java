package com.saif.fitnessapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.saif.fitnessapp.ui.TitleController;
import com.saif.fitnessapp.ui.home.HomeFragment;
import com.saif.fitnessapp.ui.activity.ActivityFragment;
import com.saif.fitnessapp.ui.recommendations.RecommendationsFragment;
import com.saif.fitnessapp.ui.profile.ProfileFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements TitleController {

    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();

        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        // Handle bottom navigation clicks
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment = null;

            if (item.getItemId() == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_activity) {
                fragment = new ActivityFragment();
            } else if (item.getItemId() == R.id.nav_recommendations) {
                fragment = new RecommendationsFragment();
            } else if (item.getItemId() == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            return loadFragment(fragment);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    @Override
    public void setTitle(String title) {
        if (getSupportActionBar() == null) {
            return;
        }
        getSupportActionBar().setTitle(title);
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
