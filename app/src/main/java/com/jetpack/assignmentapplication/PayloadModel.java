package com.jetpack.assignmentapplication;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PayloadModel {

    @SerializedName("device_id")
    private String deviceId;

    @SerializedName("sync_type")
    private String syncType;

    @SerializedName("apps")
    private List<AppModel> apps;

    public PayloadModel(String deviceId, String syncType, List<AppModel> apps) {
        this.deviceId = deviceId;
        this.syncType = syncType;
        this.apps = apps;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getSyncType() {
        return syncType;
    }

    public List<AppModel> getApps() {
        return apps;
    }
}
