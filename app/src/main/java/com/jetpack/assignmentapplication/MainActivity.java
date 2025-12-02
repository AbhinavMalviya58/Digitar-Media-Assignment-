package com.jetpack.assignmentapplication;

import android.Manifest;
import android.app.AppOpsManager; // Import Added
import android.content.Context;   // Import Added
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;      // Import Added
import android.provider.Settings; // Import Added
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        requestNotificationPermissionIfNeeded();

        requestUsageStatsPermissionIfNeeded();

        Button shareButton = findViewById(R.id.button_share_apps);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enqueueFullListWork();
            }
        });

        Button openUrlButton = findViewById(R.id.button_open_url);
        openUrlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://abhinavmalviyaportfoilo.netlify.app/";

                UrlHitWorker.enqueue(getApplicationContext(), url);

                Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
                intent.putExtra(WebViewActivity.EXTRA_URL, url);
                startActivity(intent);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        enqueueFullListWork();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001);
            }
        }
    }

    private void requestUsageStatsPermissionIfNeeded() {
        boolean granted = false;
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), getPackageName());

        if (mode == AppOpsManager.MODE_DEFAULT) {
            granted = (checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            granted = (mode == AppOpsManager.MODE_ALLOWED);
        }

        if (!granted) {
            Toast.makeText(this, "Please grant Usage Access to detect Play Store", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        }
    }

    private void enqueueFullListWork() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SendFullListWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
        workManager.enqueue(workRequest);

        workManager.getWorkInfoByIdLiveData(workRequest.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo == null) {
                            return;
                        }

                        if (workInfo.getState().isFinished()) {
                            boolean success = workInfo.getOutputData()
                                    .getBoolean(SendFullListWorker.OUTPUT_KEY_SUCCESS, false);
                            String message = workInfo.getOutputData()
                                    .getString(SendFullListWorker.OUTPUT_KEY_MESSAGE);

                            if (success) {
                                Toast.makeText(MainActivity.this,
                                                "App list synced successfully",
                                                Toast.LENGTH_LONG)
                                        .show();
                            } else if (message != null && !message.isEmpty()) {
                                Toast.makeText(MainActivity.this,
                                                message,
                                                Toast.LENGTH_LONG)
                                        .show();
                            } else {
                                Toast.makeText(MainActivity.this,
                                                "Failed to sync app list",
                                                Toast.LENGTH_LONG)
                                        .show();
                            }
                        }
                    }
                });
    }
}