# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# --- Retrofit / OkHttp / Gson ---
-keepattributes Signature
-keepattributes *Annotation*

-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**

-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Keep Retrofit API interfaces methods with HTTP annotations
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}

# --- App models & classes ---
-keep class com.jetpack.assignmentapplication.ApiResponseModel { *; }
-keep class com.jetpack.assignmentapplication.AppModel { *; }
-keep class com.jetpack.assignmentapplication.PayloadModel { *; }

# Keep entire app package (models, activities, services, workers)
-keep class com.jetpack.assignmentapplication.** { *; }

# --- WorkManager workers and receivers ---
-keep class com.jetpack.assignmentapplication.SendFullListWorker { *; }
-keep class com.jetpack.assignmentapplication.SendNewInstallWorker { *; }
-keep class com.jetpack.assignmentapplication.PackageAddedReceiver { *; }
-keep class com.jetpack.assignmentapplication.BootCompletedReceiver { *; }
-keep class com.jetpack.assignmentapplication.AppMonitoringService { *; }
-keep class com.jetpack.assignmentapplication.PlayStoreUsageWorker { *; }
-keep class com.jetpack.assignmentapplication.UrlHitWorker { *; }

-keep class androidx.work.** { *; }

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile