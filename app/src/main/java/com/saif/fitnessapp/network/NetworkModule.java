package com.saif.fitnessapp.network;


import com.saif.fitnessapp.auth.AuthConfig;
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

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    @Provides
    public OkHttpClient provideOkHttpClient(TokenManager tokenManager) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(tokenManager))
                .addInterceptor(logging)
                .connectTimeout((AuthConfig.API_TIMEOUT_SECONDS), TimeUnit.SECONDS)
                .readTimeout((AuthConfig.API_TIMEOUT_SECONDS), TimeUnit.SECONDS)
                .writeTimeout((AuthConfig.API_TIMEOUT_SECONDS), TimeUnit.SECONDS)
                .build();
    }

    @Provides
    public Retrofit provideRetrofit(OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(AuthConfig.API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides
    public ApiService provideApiService(Retrofit retrofit) {
        return retrofit.create(ApiService.class);
    }
}
