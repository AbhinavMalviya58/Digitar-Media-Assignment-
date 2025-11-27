package com.jetpack.assignmentapplication;

import java.io.IOException;

import retrofit2.Response;

public class AppRepository {

    private final ApiService apiService;

    public AppRepository() {
        this.apiService = RetrofitClient.getApiService();
    }

    public Response<ApiResponseModel> sendAppList(PayloadModel payloadModel) throws IOException {
        return apiService.sendAppList(payloadModel).execute();
    }
}
