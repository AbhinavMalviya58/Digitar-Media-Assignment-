package com.jetpack.assignmentapplication;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

/**
 * Worker responsible for sending a single newly installed app to the backend.
 */
public class SendNewInstallWorker extends Worker {

    public static final String KEY_PACKAGE_NAME = "package_name";
    public static final String SYNC_TYPE_NEW_INSTALL = "new_install";

    public SendNewInstallWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        Log.d("SendNewInstallWorker", "doWork started");

        String packageName = getInputData().getString(KEY_PACKAGE_NAME);
        if (packageName == null || packageName.isEmpty()) {
            Log.d("SendNewInstallWorker", "No package name in input data");
            return Result.failure();
        }

        if (PreferencesManager.isPackageAlreadySent(context, packageName)) {
            Log.d("SendNewInstallWorker", "Package already sent: " + packageName);
            return Result.success();
        }

        AppScanner scanner = new AppScanner(context);
        AppModel appModel = scanner.getAppForPackage(packageName);
        if (appModel == null) {
            Log.d("SendNewInstallWorker", "App not found for package: " + packageName);
            return Result.failure();
        }

        List<AppModel> apps = new ArrayList<>();
        apps.add(appModel);

        String deviceId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        PayloadModel payload = new PayloadModel(deviceId, SYNC_TYPE_NEW_INSTALL, apps);

        AppRepository repository = new AppRepository();

        try {
            Response<ApiResponseModel> response = repository.sendAppList(payload);
            if (response.isSuccessful()) {
                ApiResponseModel body = response.body();
                if (body != null && body.isSuccess()) {
                    PreferencesManager.markPackageSent(context, packageName);
                    Log.d("SendNewInstallWorker", "Successfully sent new install for: " + packageName);
                    return Result.success();
                } else {
                    Log.d("SendNewInstallWorker", "API responded but success=false for: " + packageName);
                    return Result.retry();
                }
            } else {
                Log.d("SendNewInstallWorker", "HTTP error when sending new install for: " + packageName
                        + ", code=" + response.code());
                return Result.retry();
            }
        } catch (IOException e) {
            Log.e("SendNewInstallWorker", "Network error when sending new install for: " + packageName, e);
            return Result.retry();
        }
    }

    public static Data createInputData(String packageName) {
        return new Data.Builder()
                .putString(KEY_PACKAGE_NAME, packageName)
                .build();
    }
}
