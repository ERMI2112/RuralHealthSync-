# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep data classes used with Gson / Retrofit
-keepclassmembers class com.example.ruralhealthsync.data.** { *; }
-keep class com.example.ruralhealthsync.data.remote.SyncResponse { *; }
