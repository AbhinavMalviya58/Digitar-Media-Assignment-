package com.jetpack.assignmentapplication;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;

/**
 * Worker responsible for sending the full list of installed apps to the backend.
 */
public class SendFullListWorker extends Worker {

    public static final String SYNC_TYPE_FULL_LIST = "full_list";
    public static final String OUTPUT_KEY_SUCCESS = "output_success";
    public static final String OUTPUT_KEY_MESSAGE = "output_message";

    public SendFullListWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppScanner scanner = new AppScanner(context);
        List<AppModel> apps = scanner.getInstalledApps();

        String deviceId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        PayloadModel payload = new PayloadModel(deviceId, SYNC_TYPE_FULL_LIST, apps);

        AppRepository repository = new AppRepository();

        try {
            Response<ApiResponseModel> response = repository.sendAppList(payload);

            if (response.isSuccessful()) {
                ApiResponseModel body = response.body();
                boolean success = body != null && body.isSuccess();
                String message = body != null ? body.getMessage() : null;

                Data output = new Data.Builder()
                        .putBoolean(OUTPUT_KEY_SUCCESS, success)
                        .putString(OUTPUT_KEY_MESSAGE, message)
                        .build();

                return Result.success(output);
            } else {
                String message = null;
                try {
                    if (response.errorBody() != null) {
                        String errorJson = response.errorBody().string();
                        ApiResponseModel errorBody = new Gson().fromJson(errorJson, ApiResponseModel.class);
                        if (errorBody != null) {
                            message = errorBody.getMessage();
                        }
                    }
                } catch (Exception ignored) {
                }

                if (message == null || message.isEmpty()) {
                    message = "HTTP " + response.code() + " error";
                }

                Data output = new Data.Builder()
                        .putBoolean(OUTPUT_KEY_SUCCESS, false)
                        .putString(OUTPUT_KEY_MESSAGE, message)
                        .build();

                return Result.success(output);
            }
        } catch (IOException e) {
            return Result.retry();
        }
    }
}
