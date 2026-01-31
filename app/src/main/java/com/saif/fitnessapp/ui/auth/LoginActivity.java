//package com.saif.fitnessapp.ui.auth;

//import android.content.Intent;
//import android.os.Bundle;
//import android.widget.Button;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.saif.fitnessapp.MainActivity;
//import com.saif.fitnessapp.R;
//import com.saif.fitnessapp.auth.AuthManager;
//import com.saif.fitnessapp.auth.TokenManager;
//
//import dagger.hilt.android.AndroidEntryPoint;
//
//import javax.inject.Inject;

//@AndroidEntryPoint
//public class LoginActivity extends AppCompatActivity {
//
//    @Inject
//    AuthManager authManager;
//
//    @Inject
//    TokenManager tokenManager;
//
//    private Button loginButton;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_login);
//
//        loginButton = findViewById(R.id.login_button);
//
//        loginButton.setOnClickListener(v -> {
//            Intent loginIntent = authManager.getLoginIntent();
//            startActivity(loginIntent);
//        });
//    }
//
//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        handleAuthCallback(intent);
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        // Check if user has logged in via the browser
//        if (tokenManager.isLoggedIn()) {
//            navigateToMain();
//        }
//    }
//
//    private void handleAuthCallback(Intent intent) {
//        if (intent != null && intent.getData() != null) {
//            String authorizationCode = intent.getData().getQueryParameter("code");
//
//            if (authorizationCode != null) {
//                // Exchange code for token (you'll need to provide your Keycloak client secret)
//                authManager.exchangeCodeForToken(authorizationCode, "YOUR_CLIENT_SECRET",
//                    new AuthManager.AuthTokenCallback() {
//                        @Override
//                        public void onSuccess(String userId) {
//                            navigateToMain();
//                        }
//
//                        @Override
//                        public void onError(String error) {
//                            Toast.makeText(LoginActivity.this, "Login failed: " + error,
//                                    Toast.LENGTH_SHORT).show();
//                        }
//                    });
//            }
//        }
//    }
//
//    private void navigateToMain() {
//        startActivity(new Intent(this, MainActivity.class));
//        finish();
//    }
//}

//package com.saif.fitnessapp.ui.auth;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.saif.fitnessapp.MainActivity;
//import com.saif.fitnessapp.R;
//import com.saif.fitnessapp.auth.AuthManager;
//
//import javax.inject.Inject;
//
//import dagger.hilt.android.AndroidEntryPoint;
//
//@AndroidEntryPoint
//public class LoginActivity extends AppCompatActivity {
//
//    @Inject
//    AuthManager authManager;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_login);
//
//        findViewById(R.id.login_button).setOnClickListener(v ->
//                authManager.startLogin(this)
//        );
//
//        handleIntent(getIntent());
//    }
//
//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        handleIntent(intent);
//    }
//
//    private void handleIntent(Intent intent) {
//
//        if (intent == null || intent.getData() == null) {
//            Log.d("LoginActivity", "No intent data,intent is null");
//            return; // NORMAL launch, ignore
//        }
//
//        authManager.handleAuthResponse(intent, new AuthManager.AuthCallback() {
//            @Override
//            public void onSuccess() {
//                Log.d("LoginActivity", "Login successful");
//                startActivity(new Intent(LoginActivity.this, MainActivity.class));
//                finish();
//            }
//
//            @Override
//            public void onError(String error) {
//                Log.e("LoginActivity", "Login failed: " + error);
//                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
//            }
//        });
//    }
//
//}

package com.saif.fitnessapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.saif.fitnessapp.MainActivity;
import com.saif.fitnessapp.R;
import com.saif.fitnessapp.auth.AuthManager;
import com.saif.fitnessapp.auth.TokenManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_AUTH = 100;

    @Inject
    AuthManager authManager;

    @Inject
    TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate called");

        // Check if already logged in
        if (tokenManager.isLoggedIn()) {
            Log.d(TAG, "User already logged in, navigating to MainActivity");
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        findViewById(R.id.login_button).setOnClickListener(v -> {
            Log.d(TAG, "Login button clicked");
            authManager.startLogin(this);
        });

        // Handle the intent if this activity was launched with intent data
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent called");
        setIntent(intent); // IMPORTANT: Update the activity's intent
        handleIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult - requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (requestCode == RC_AUTH) {
            if (data != null) {
                handleAuthResponse(data);
            } else {
                Log.e(TAG, "onActivityResult - data is null");
                Toast.makeText(this, "Login cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            Log.d(TAG, "handleIntent - intent is null");
            return;
        }

        if (intent.getData() == null) {
            Log.d(TAG, "No intent data, intent is null");
            return; // Normal launch, not a redirect
        }

        Log.d(TAG, "handleIntent - has data: " + intent.getData());
        handleAuthResponse(intent);
    }

    private void handleAuthResponse(Intent intent) {
        Log.d(TAG, "handleAuthResponse called");

        authManager.handleAuthResponse(intent, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Login successful");
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Login failed: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void navigateToMain() {
        Log.d(TAG, "Navigating to MainActivity");
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");

        // Check again in case user logged in elsewhere
        if (tokenManager.isLoggedIn() && !isFinishing()) {
            Log.d(TAG, "User logged in during resume, navigating to MainActivity");
            navigateToMain();
        }
    }
}