# Fitness Tracker Android App

A production-ready Android application for fitness tracking built with Spring Boot microservices backend integration. This app features Keycloak OAuth2 authentication, activity tracking, AI recommendations, and user profile management.

## Architecture Overview

- **Authentication**: Keycloak OAuth2 with PKCE Authorization Code Flow
- **Networking**: Retrofit + OkHttp with automatic token refresh
- **Dependency Injection**: Hilt
- **Architecture Pattern**: MVVM with LiveData and ViewModel
- **Pagination**: Paging 3 for infinite scrolling
- **Storage**: EncryptedSharedPreferences for secure token storage
- **UI**: Material Design 3 with XML layouts

## Repositories

- Android App: https://github.com/saifshamsi28/fitness-android-app
- Backend (Spring Boot Microservices): https://github.com/saifshamsi28/fitness-microservices

## Prerequisites

1. Android Studio (latest version)
2. Java 17 or higher
3. Keycloak server running with OAuth2 configured
4. Spring Boot microservices backend (Gateway, User Service, Activity Service, AI Service)

## Setup Instructions

### 1. Clone the Repository
```bash
git clone https://github.com/saifshamsi28/fitness-android-app.git
cd android-fitness-app
```

### 2. Configure Keycloak

Update AuthConfig.java with environment-specific values (do not commit secrets).
this one -> `src/main/java/com/saif/fitnessapp/auth/AuthConfig.java`

```java
public static final String KEYCLOAK_SERVER_URL = "https://your-keycloak-server.com";
public static final String KEYCLOAK_REALM = "your-realm";
public static final String CLIENT_ID = "your-client-id";
public static final String API_BASE_URL = "http://your-gateway-ip:8080/";
```

### 3. Register OAuth2 Client in Keycloak

1. Navigate to Keycloak Admin Console
2. Go to Clients > Create New Client
3. Set Client ID: `fitness-mobile-app`
4. Enable "Standard Flow Enabled" and "Direct Access Grants Enabled"
5. Add Redirect URIs: `com.saif.fitnessapp://oauth-callback`
6. Generate a client secret (copy it for later use)

### 4. Update Build Configuration

In `build.gradle.kts`, update the API base URL with your backend server IP:
```kotlin
// Use your backend server IP or domain
public static final String API_BASE_URL = "http://192.168.x.x:8080/";
```

### 5. Handle OAuth Callback

In `LoginActivity.java`, update the client secret exchange:
```java
authManager.exchangeCodeForToken(authorizationCode, "YOUR_CLIENT_SECRET", callback);
```

### 6. Add Icons (Optional)

Create icon resources in `src/main/res/drawable/`:
- `ic_home.xml`
- `ic_activity.xml`
- `ic_recommendations.xml`
- `ic_profile.xml`
- `ic_add.xml`

Or use Material Icons from Android Studio's built-in resources.

### 7. Build and Run

```bash
# Debug
./gradlew assembleDebug

# Or use Android Studio: Build > Build App Bundle / Generate Signed Bundle
```

## Project Structure

```
com.saif.fitnessapp/
├── auth/
│   ├── AuthConfig.java           # Configuration constants
│   ├── AuthManager.java          # OAuth2 flow manager
│   └── TokenManager.java         # Secure token storage
├── network/
│   ├── ApiService.java           # Retrofit interface
│   ├── AuthInterceptor.java      # Token injection
│   ├── NetworkModule.java        # Hilt network configuration
│   └── dto/
│       ├── UserResponse.java
│       ├── ActivityRequest.java
│       ├── ActivityResponse.java
│       └── Recommendation.java
├── activity/
│   ├── ActivityViewModel.java
│   ├── ActivityRepository.java
│   ├── ActivityPagingSource.java
├── recommendation/
│   ├── RecommendationViewModel.java
│   └── RecommendationRepository.java
├── user/
│   ├── UserViewModel.java
│   └── UserRepository.java
└── ui/
    ├── splash/SplashActivity.java
    ├── auth/LoginActivity.java
    ├── home/
    │   ├── HomeFragment.java
    │   └── AddActivityFragment.java
    ├── activity/
    │   ├── ActivityFragment.java
    │   └── ActivityAdapter.java
    ├── recommendations/
    │   ├── RecommendationsFragment.java
    │   └── RecommendationAdapter.java
    └── profile/ProfileFragment.java
```

## Key Features

### 1. Authentication Flow
- Splash screen with auto-login check
- Chrome Custom Tabs for Keycloak login
- Automatic token refresh before expiration
- Secure token storage using EncryptedSharedPreferences

### 2. Activity Tracking
- Add custom activities with type, duration, and calories
- Real-time submission to backend
- Activity history with pagination (Paging 3)
- Pull-to-refresh support

### 3. AI Recommendations
- Fetch personalized recommendations based on activities
- Improvements, suggestions, and safety tips
- Async processing via Kafka backend
- Empty state handling

### 4. User Profile
- Display user information from JWT
- Manage account settings
- Logout functionality

## API Endpoints

All requests go through the Gateway at `http://your-gateway-ip:8080/`:

### User Service
- `GET /user-service/api/users/{userId}` - Get user profile
- `GET /user-service/api/users/{userId}/validate` - Validate user

### Activity Service
- `POST /activity-service/api/activities/track` - Track activity
- `GET /activity-service/api/activities?page=0&size=10&userId={userId}` - Get activities

### AI Service
- `GET /ai-service/api/recommendations/user/{userId}` - Get user recommendations
- `GET /ai-service/api/recommendations/activity/{activityId}` - Get activity recommendation

All endpoints require `Authorization: Bearer <access_token>` header.

## Authentication Details

### OAuth2 Flow
1. User clicks login on LoginActivity
2. AuthManager opens Keycloak login in Chrome Custom Tabs
3. User authenticates and is redirected with authorization code
4. App exchanges code for access_token, refresh_token, and id_token
5. Tokens are stored encrypted in EncryptedSharedPreferences
6. Auth Interceptor automatically adds Bearer token to all API requests

### Token Refresh
- Checked on app launch
- AuthInterceptor can be extended to handle 401 responses and refresh tokens
- Automatic refresh before token expiration

## Dependencies

- **Retrofit 2.10.0**: HTTP client for API calls
- **Hilt 2.50**: Dependency injection
- **Paging 3.2.1**: Infinite scrolling
- **AppAuth 0.11.0**: OAuth2 support
- **EncryptedSharedPreferences 1.1.0-alpha06**: Secure storage
- **Material 3**: Modern UI components
- **Glide 4.16.0**: Image loading (optional)

## Configuration Files

### build.gradle.kts
Contains all dependencies and Android SDK configuration.

### AndroidManifest.xml
Declares activities, permissions, and app metadata.

### strings.xml
All UI text resources for localization.

### themes.xml & colors.xml
Material Design 3 styling and color palette.

## Security Considerations

1. **Token Storage**: Uses Android Keystore via EncryptedSharedPreferences
2. **HTTPS**: Use HTTPS for production Keycloak and backend servers
3. **PKCE**: Authorization Code Flow with PKCE for OAuth2
4. **API Interceptor**: Automatically includes Bearer token in headers
5. **Parameterized Queries**: Backend handles all SQL injection protection

## Error Handling

- Network errors are logged and shown in UI
- Empty states for activities and recommendations
- Loading indicators during async operations
- Toast messages for user feedback

## Testing

Run unit tests:
```bash
./gradlew test
```

Run instrumentation tests:
```bash
./gradlew connectedAndroidTest
```

## Deployment

### Production Build
```bash
./gradlew assembleRelease
# or create signed bundle
./gradlew bundleRelease
```

### Google Play Deployment
1. Prepare release build with signing key
2. Generate bundle via Android Studio
3. Upload to Google Play Console
4. Configure app listing and screenshots
5. Set up rollout strategy

## Troubleshooting

### Issue: "Token exchange failed"
- Verify Keycloak server is running
- Check CLIENT_ID and REDIRECT_URL match Keycloak config
- Ensure client secret is correct

### Issue: "401 Unauthorized" on API calls
- Verify token is being stored correctly
- Check Authorization header is being sent
- Ensure Keycloak token is still valid

### Issue: "Failed to load activities"
- Verify backend services are running
- Check Gateway is accessible at configured URL
- Verify user has valid authentication

### Issue: Icons not appearing
- Add custom icon SVGs to `src/main/res/drawable/`
- Or use Android Studio's Vector Asset Studio to import Material Icons
- Update menu resource file references

## Contributing

1. Follow Android development best practices
2. Use meaningful variable and function names
3. Add comments for complex logic
4. Test changes thoroughly
5. Submit pull requests with detailed descriptions

## License

Proprietary - All rights reserved

## Support

For issues or questions:
1. Check troubleshooting section above
2. Review backend service logs
3. Verify Keycloak configuration
4. Check network connectivity
# OR
- Open a GitHub issue
- Contact via GitHub profile