package com.jetpack.assignmentapplication;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton holder for Retrofit and OkHttp instances.
 */
public final class RetrofitClient {

    private static final String BASE_URL = "https://api.digitarmedia.com/";

    private static volatile Retrofit retrofitInstance;

    private RetrofitClient() {
        // No instances
    }

    public static ApiService getApiService() {
        if (retrofitInstance == null) {
            synchronized (RetrofitClient.class) {
                if (retrofitInstance == null) {
                    retrofitInstance = buildRetrofit();
                }
            }
        }
        return retrofitInstance.create(ApiService.class);
    }

    private static Retrofit buildRetrofit() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
