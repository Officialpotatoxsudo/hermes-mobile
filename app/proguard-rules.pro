# ProGuard / R8 rules for Hermes Mobile
-keepattributes Signature
-keepattributes *Annotation*

-keep class kotlinx.serialization.** { *; }
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    public static ** serializer(...);
}

-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.** class * { *; }
-keep class androidx.room.** { *; }
-keep class okhttp3.sse.** { *; }
