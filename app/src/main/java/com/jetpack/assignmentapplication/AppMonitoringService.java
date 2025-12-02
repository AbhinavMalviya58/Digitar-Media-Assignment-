package com.jetpack.assignmentapplication;

import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AppMonitoringService extends Service {

    private static final int FOREGROUND_NOTIFICATION_ID = 1;
    private static final String WORK_NAME_FULL_LIST = "full_list_periodic";
    private static final long USAGE_POLL_INTERVAL_MS = 2000L;
    private static final long PLAY_STORE_TRIGGER_COOLDOWN_MS = 60_000L; // don't spam backend
    private static final String PLAY_STORE_PACKAGE = "com.android.vending";

    private Handler handler;
    private Runnable usageCheckRunnable;
    private UsageStatsManager usageStatsManager;
    private PackageAddedReceiver dynamicPackageReceiver;
    private long lastPlayStoreTriggerTime = 0L;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("AppMonitoringService", "onCreate: starting foreground service");
        startForeground(FOREGROUND_NOTIFICATION_ID,
                NotificationUtils.buildForegroundNotification(this, "Running in background"));
        schedulePeriodicWork();
    }

    private void schedulePeriodicWork() {
        Log.d("AppMonitoringService", "Scheduling periodic work for full list");
        Constraints networkConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest fullListRequest =
                new PeriodicWorkRequest.Builder(SendFullListWorker.class, 12, TimeUnit.HOURS)
                        .setConstraints(networkConstraints)
                        .build();

        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
        workManager.enqueueUniquePeriodicWork(
                WORK_NAME_FULL_LIST,
                ExistingPeriodicWorkPolicy.KEEP,
                fullListRequest
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Initialize UsageStats polling loop once
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
            usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            usageCheckRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        checkPlayStoreUsage();
                    } catch (Exception e) {
                        Log.e("AppMonitoringService", "Error in usageCheckRunnable", e);
                    }
                    if (handler != null) {
                        handler.postDelayed(this, USAGE_POLL_INTERVAL_MS);
                    }
                }
            };
            handler.post(usageCheckRunnable);
        }

        // Dynamically register PackageAddedReceiver so installs are caught
        if (dynamicPackageReceiver == null) {
            dynamicPackageReceiver = new PackageAddedReceiver();
            IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
            filter.addDataScheme("package");
            registerReceiver(dynamicPackageReceiver, filter);
            Log.d("AppMonitoringService", "Dynamically registered PackageAddedReceiver");
        }

        return START_STICKY;
    }

    private void checkPlayStoreUsage() {
        if (usageStatsManager == null) {
            Log.d("AppMonitoringService", "UsageStatsManager is null in checkPlayStoreUsage");
            return;
        }

        long endTime = System.currentTimeMillis();
        long startTime = endTime - 15 * 60 * 1000; // last 15 minutes, same as worker logic

        List<UsageStats> stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
        );
        if (stats == null || stats.isEmpty()) {
            // Likely no usage access granted / no data
            return;
        }

        UsageStats mostRecent = null;
        for (UsageStats usageStats : stats) {
            if (mostRecent == null || usageStats.getLastTimeUsed() > mostRecent.getLastTimeUsed()) {
                mostRecent = usageStats;
            }
        }

        if (mostRecent == null) {
            return;
        }

        if (!PLAY_STORE_PACKAGE.equals(mostRecent.getPackageName())) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastPlayStoreTriggerTime < PLAY_STORE_TRIGGER_COOLDOWN_MS) {
            return;
        }
        lastPlayStoreTriggerTime = now;

        Log.d("AppMonitoringService", "Detected Play Store in recent foreground usage - triggering full list sync");

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
        workManager.enqueue(new OneTimeWorkRequest.Builder(SendFullListWorker.class)
                .setConstraints(constraints)
                .build());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && usageCheckRunnable != null) {
            handler.removeCallbacks(usageCheckRunnable);
        }
        handler = null;
        usageCheckRunnable = null;

        if (dynamicPackageReceiver != null) {
            try {
                unregisterReceiver(dynamicPackageReceiver);
                Log.d("AppMonitoringService", "Unregistered dynamic PackageAddedReceiver");
            } catch (IllegalArgumentException ignored) {
            }
            dynamicPackageReceiver = null;
        }
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, AppMonitoringService.class);
        ContextCompat.startForegroundService(context, intent);
    }
}
