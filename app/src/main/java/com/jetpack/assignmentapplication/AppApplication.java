package com.jetpack.assignmentapplication;

import android.app.Application;

public class AppApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationUtils.createNotificationChannels(this);
        AppMonitoringService.start(this);
    }
}
