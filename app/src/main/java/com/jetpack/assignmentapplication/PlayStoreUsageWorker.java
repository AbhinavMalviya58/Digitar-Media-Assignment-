package com.jetpack.assignmentapplication;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

public class PlayStoreUsageWorker extends Worker {

    private static final String PLAY_STORE_PACKAGE = "com.android.vending";

    public PlayStoreUsageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        Log.d("PlayStoreUsageWorker", "doWork started");
        UsageStatsManager usageStatsManager =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usageStatsManager == null) {
            Log.d("PlayStoreUsageWorker", "UsageStatsManager is null");
            return Result.success();
        }

        long endTime = System.currentTimeMillis();
        long startTime = endTime - 15 * 60 * 1000;

        List<UsageStats> stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
        );
        if (stats == null || stats.isEmpty()) {
            Log.d("PlayStoreUsageWorker", "No usage stats returned. Usage access likely not granted.");
            return Result.success();
        }

        UsageStats mostRecent = null;
        for (UsageStats usageStats : stats) {
            if (mostRecent == null || usageStats.getLastTimeUsed() > mostRecent.getLastTimeUsed()) {
                mostRecent = usageStats;
            }
        }

        if (mostRecent == null) {
            Log.d("PlayStoreUsageWorker", "No recent app found in usage stats");
            return Result.success();
        }

        if (!PLAY_STORE_PACKAGE.equals(mostRecent.getPackageName())) {
            Log.d("PlayStoreUsageWorker", "Most recent app is not Play Store: " + mostRecent.getPackageName());
            return Result.success();
        }

        Log.d("PlayStoreUsageWorker", "Detected Play Store open event");

        String deviceId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        List<AppModel> apps = new ArrayList<>();
        AppModel appModel = new AppModel("Play Store", PLAY_STORE_PACKAGE,
                TimeUtils.formatTimestamp(System.currentTimeMillis()));
        apps.add(appModel);

        PayloadModel payload = new PayloadModel(deviceId, "play_store_open", apps);
        AppRepository repository = new AppRepository();

        try {
            Response<ApiResponseModel> response = repository.sendAppList(payload);
            if (response.isSuccessful()) {
                Log.d("PlayStoreUsageWorker", "Successfully notified backend about Play Store open");
                return Result.success();
            } else {
                Log.d("PlayStoreUsageWorker", "HTTP error when notifying Play Store open, code=" + response.code());
                return Result.retry();
            }
        } catch (IOException e) {
            Log.e("PlayStoreUsageWorker", "Network error when notifying Play Store open", e);
            return Result.retry();
        }
    }
}
