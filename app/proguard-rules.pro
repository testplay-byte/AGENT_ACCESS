# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Moshi
-keepclassmembers,allowshrinking,allowobfuscation class * {
    @com.squareup.moshi.JsonClass <fields>;
}
-keep @com.squareup.moshi.JsonQualifier interface *

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose
-keep,allowobfuscation,allowshrinking class androidx.compose.** {
    *;
}

# kotlinx.serialization - CRITICAL for WebSocket messages
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**
-keep @kotlinx.serialization.Serializable class *
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
}
-keepclasseswithmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
}

# Room
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# DataStore
-keepclassmembers class * {
    ** Companion;
}
-keepclasseswithmembers class * {
    ** Companion;
}
-keepclasseswithmembernames class * {
    native <methods>;
}

# AniTrack specific
-keep class com.anitrack.app.data.api.models.** { *; }
-keep class com.anitrack.app.remotecontrol.models.** { *; }

# Keep essential annotation attributes for serialization
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
