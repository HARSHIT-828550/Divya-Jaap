# Production R8 / ProGuard Rules for Divya Jaap

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Kotlin Metadata & Coroutines
-keep class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.**

# Moshi & Domain / Database Models (used for JSON Backup/Restore and Cloud TTS API)
-keep class com.example.domain.BackupPayload { *; }
-keep class com.example.database.** { *; }
-keep class com.example.data.CloudTtsRequest { *; }
-keep class com.example.data.CloudTtsResponse { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Retrofit & OkHttp
-keepattributes Signature,Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# WorkManager Workers
-keep public class * extends androidx.work.ListenableWorker {
    public <init>(...);
}

# Media3 & Playback Service
-keep class com.example.audio.MantraPlaybackService { *; }
-dontwarn androidx.media3.**
