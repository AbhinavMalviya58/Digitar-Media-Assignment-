package com.jetpack.assignmentapplication;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility methods related to time formatting.
 */
public final class TimeUtils {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private TimeUtils() {
        // Utility class
    }

    public static String formatTimestamp(long millis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_PATTERN, Locale.US);
        return dateFormat.format(new Date(millis));
    }
}
