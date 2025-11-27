package com.jetpack.assignmentapplication;

import com.google.gson.annotations.SerializedName;

public class AppModel {

    @SerializedName("app_name")
    private String appName;

    @SerializedName("package_name")
    private String packageName;

    @SerializedName("installed_at")
    private String installedAt;

    public AppModel(String appName, String packageName, String installedAt) {
        this.appName = appName;
        this.packageName = packageName;
        this.installedAt = installedAt;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getInstalledAt() {
        return installedAt;
    }
}
