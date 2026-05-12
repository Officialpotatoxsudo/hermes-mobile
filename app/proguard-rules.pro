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

# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# Retrofit / OkHttp / SSE
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.sse.** { *; }

# Tink / Android Keystore
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Kotlinx serialization model serializers
-keep class com.hermes.mobile.core.model.**$$serializer { *; }
-keep class com.hermes.mobile.core.model.** { *; }
