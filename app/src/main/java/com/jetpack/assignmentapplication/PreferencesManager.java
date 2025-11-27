package com.jetpack.assignmentapplication;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Helper around SharedPreferences to avoid sending duplicate new-install events.
 */
public final class PreferencesManager {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_SENT_PACKAGES = "sent_packages";

    private PreferencesManager() {
        // No instances
    }

    public static boolean isPackageAlreadySent(Context context, String packageName) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> sentPackages = sharedPreferences.getStringSet(KEY_SENT_PACKAGES, new HashSet<String>());
        if (sentPackages == null) {
            return false;
        }
        return sentPackages.contains(packageName);
    }

    public static void markPackageSent(Context context, String packageName) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> existing = sharedPreferences.getStringSet(KEY_SENT_PACKAGES, new HashSet<String>());
        Set<String> updated = new HashSet<>();
        if (existing != null) {
            updated.addAll(existing);
        }
        updated.add(packageName);
        sharedPreferences.edit().putStringSet(KEY_SENT_PACKAGES, updated).apply();
    }
}
