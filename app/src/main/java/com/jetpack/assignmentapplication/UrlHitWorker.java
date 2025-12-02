package com.jetpack.assignmentapplication;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Response;

public class UrlHitWorker extends Worker {

    public static final String KEY_URL = "url";

    public UrlHitWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        String url = getInputData().getString(KEY_URL);
        if (TextUtils.isEmpty(url)) {
            Log.d("UrlHitWorker", "No URL provided in input data");
            return Result.failure();
        }

        Log.d("UrlHitWorker", "Processing URL hit for: " + url);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            okhttp3.Response response = client.newCall(request).execute();
            boolean success = response.isSuccessful();
            response.close();

            String deviceId = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );

            List<AppModel> apps = new ArrayList<>();
            AppModel appModel = new AppModel("UrlHit", url,
                    TimeUtils.formatTimestamp(System.currentTimeMillis()));
            apps.add(appModel);

            PayloadModel payload = new PayloadModel(deviceId, "url_hit", apps);
            AppRepository repository = new AppRepository();
            Response<ApiResponseModel> apiResponse = repository.sendAppList(payload);
            if (apiResponse.isSuccessful() && success) {
                Log.d("UrlHitWorker", "Successfully hit URL and notified backend: " + url);
                return Result.success();
            } else {
                Log.d("UrlHitWorker", "HTTP error when notifying URL hit, success=" + success
                        + ", apiCode=" + apiResponse.code());
                return Result.retry();
            }
        } catch (IOException e) {
            Log.e("UrlHitWorker", "Network error when processing URL hit for: " + url, e);
            return Result.retry();
        }
    }

    public static void enqueue(Context context, String url) {
        Data input = new Data.Builder()
                .putString(KEY_URL, url)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(UrlHitWorker.class)
                .setInputData(input)
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueue(request);
    }
}
