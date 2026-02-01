package com.saif.fitnessapp.network;

import com.saif.fitnessapp.BuildConfig;
import com.saif.fitnessapp.auth.AuthConfig;
import com.saif.fitnessapp.auth.AuthManager;
import com.saif.fitnessapp.auth.TokenManager;

import java.util.concurrent.TimeUnit;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    /**
     * Injects AuthManager for automatic token refresh
     */
    @Provides
    @Singleton
    public AuthInterceptor provideAuthInterceptor(
            TokenManager tokenManager,
            AuthManager authManager
    ) {
        return new AuthInterceptor(tokenManager, authManager);
    }

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(AuthInterceptor authInterceptor) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        if (BuildConfig.DEBUG) {
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            logging.redactHeader("Authorization");
            logging.redactHeader("Cookie");
        } else {
            logging.setLevel(HttpLoggingInterceptor.Level.NONE);
        }


        return new OkHttpClient.Builder()
                .addInterceptor(authInterceptor) // Adds auth token and handles refresh
                .addInterceptor(logging)
                .connectTimeout(AuthConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(AuthConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(AuthConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(AuthConfig.API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    public ApiService provideApiService(Retrofit retrofit) {
        return retrofit.create(ApiService.class);
    }
}