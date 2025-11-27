package com.jetpack.assignmentapplication;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper responsible for scanning installed applications on the device.
 */
public class AppScanner {

    private final Context appContext;

    public AppScanner(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * Returns a list of all installed applications on the device.
     */
    public List<AppModel> getInstalledApps() {
        PackageManager packageManager = appContext.getPackageManager();
        List<PackageInfo> packageInfoList = packageManager.getInstalledPackages(0);
        List<AppModel> result = new ArrayList<>();

        for (PackageInfo packageInfo : packageInfoList) {
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            CharSequence label = packageManager.getApplicationLabel(applicationInfo);
            String appName = label != null ? label.toString() : packageInfo.packageName;
            String installedAt = TimeUtils.formatTimestamp(packageInfo.firstInstallTime);
            result.add(new AppModel(appName, packageInfo.packageName, installedAt));
        }

        return result;
    }

    /**
     * Returns a single application model for a package name, or null if not found.
     */
    public AppModel getAppForPackage(String packageName) {
        try {
            PackageManager packageManager = appContext.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            CharSequence label = packageManager.getApplicationLabel(applicationInfo);
            String appName = label != null ? label.toString() : packageInfo.packageName;
            String installedAt = TimeUtils.formatTimestamp(packageInfo.firstInstallTime);
            return new AppModel(appName, packageInfo.packageName, installedAt);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
