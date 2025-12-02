package com.jetpack.assignmentapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

/**
 * BroadcastReceiver that listens for new application installs and triggers a worker.
 */
public class PackageAddedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        if (!Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
            return;
        }

        Uri data = intent.getData();
        if (data == null) {
            return;
        }

        String packageName = data.getSchemeSpecificPart();
        if (packageName == null || packageName.isEmpty()) {
            return;
        }

        Log.d("PackageAddedReceiver", "New package installed: " + packageName);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SendNewInstallWorker.class)
                .setConstraints(constraints)
                .setInputData(SendNewInstallWorker.createInputData(packageName))
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueue(workRequest);
    }
}
