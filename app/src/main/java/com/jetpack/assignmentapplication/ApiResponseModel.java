package com.jetpack.assignmentapplication;

import com.google.gson.annotations.SerializedName;

public class ApiResponseModel {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
