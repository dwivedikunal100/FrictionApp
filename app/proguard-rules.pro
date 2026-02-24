# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Room database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.paging.**

# DataStore
-keep class androidx.datastore.** { *; }

# Google Play Billing
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# Accessibility Service — must not be stripped
-keep class com.friction.app.accessibility.** { *; }

# Our data models (Room entities)
-keep class com.friction.app.data.model.** { *; }

# Compose — keep all composable functions
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
