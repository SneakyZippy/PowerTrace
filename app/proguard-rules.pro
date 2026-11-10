# Proguard rules for Battery Diagnostics
-keepclassmembers class com.android.internal.os.PowerProfile {
    public <init>(...);
    public double getAveragePower(...);
}
-keep class com.sneakyzippy.powerlab.data.db.** { *; }
