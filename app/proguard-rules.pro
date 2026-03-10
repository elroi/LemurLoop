# LemurLoop project-specific ProGuard/R8 rules.
# Firebase, Hilt, Room, and many AndroidX libs include consumer rules via AARs.

# BuildConfig (used by AboutScreen for version and build date)
-keep class com.elroi.lemurloop.BuildConfig { *; }

# Native methods (ML Kit Face Detection, MediaPipe GenAI, etc.)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Kotlin metadata and annotations (reflection used by libs)
-keepattributes *Annotation*, InnerClasses
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
