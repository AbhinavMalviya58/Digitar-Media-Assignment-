package com.jetpack.assignmentapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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

        Button shareButton = findViewById(R.id.button_share_apps);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enqueueFullListWork();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        enqueueFullListWork();
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