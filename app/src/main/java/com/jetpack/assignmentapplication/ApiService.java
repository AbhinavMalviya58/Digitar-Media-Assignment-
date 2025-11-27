package com.jetpack.assignmentapplication;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Retrofit API definition for sending app list payloads.
 */
public interface ApiService {

    @POST("test/applist/")
    Call<ApiResponseModel> sendAppList(@Body PayloadModel payloadModel);
}
