package com.jetpack.assignmentapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public final class NotificationUtils {

    public static final String CHANNEL_BACKGROUND = "background_service";
    public static final String CHANNEL_IMPORTANT = "important_events";

    private NotificationUtils() {
    }

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel backgroundChannel = new NotificationChannel(
                    CHANNEL_BACKGROUND,
                    "Background service",
                    NotificationManager.IMPORTANCE_MIN
            );
            NotificationChannel importantChannel = new NotificationChannel(
                    CHANNEL_IMPORTANT,
                    "Important events",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(backgroundChannel);
                manager.createNotificationChannel(importantChannel);
            }
        }
    }

    public static Notification buildForegroundNotification(Context context, String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_BACKGROUND)
                .setContentTitle("Background service")
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN);
        return builder.build();
    }

    public static void showImportantNotification(Context context, int id, String title, String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_IMPORTANT)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat.from(context).notify(id, builder.build());
    }
}
