# Project-specific ProGuard / R8 rules.
# These rules are written so that R8 minification can be enabled safely.
# To enable: set `isMinifyEnabled = true` for the release build type in
# app/build.gradle.kts, then run a full `:app:assembleProdRelease` and
# smoke-test auth, notes sync, home cards and deadline notifications on a
# real device before submitting to RuStore.

# Keep stack traces readable in RuStore crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*,RuntimeVisibleAnnotations,AnnotationDefault

# ---------------------------------------------------------------------------
# Gson / data models (serialized via reflection — must not be renamed)
# ---------------------------------------------------------------------------
# Keep all API request/response and domain model classes and their fields.
-keep class app.homenotes.android.network.** { *; }
-keep class app.homenotes.android.**Models { *; }
-keep class app.homenotes.android.**Models$* { *; }
-keepclassmembers class app.homenotes.android.** {
    @com.google.gson.annotations.SerializedName <fields>;
}
# Keep Kotlin data-class metadata that Gson relies on.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Gson internals
-keep class com.google.gson.** { *; }
-dontwarn sun.misc.**
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ---------------------------------------------------------------------------
# Retrofit / OkHttp (ship their own consumer rules, these are belt-and-braces)
# ---------------------------------------------------------------------------
-keepattributes Exceptions
-keep,allowobfuscation interface app.homenotes.android.network.**
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ---------------------------------------------------------------------------
# Room
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# Coroutines
# ---------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ---------------------------------------------------------------------------
# Yandex AuthSDK / Google Play services auth
# ---------------------------------------------------------------------------
-keep class com.yandex.** { *; }
-dontwarn com.yandex.**
-keep class com.google.android.gms.auth.** { *; }
-dontwarn com.google.android.gms.**

# ---------------------------------------------------------------------------
# Kotlin metadata / enums / Parcelize
# ---------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
