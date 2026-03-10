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

# Suppress R8 warnings for compile-time-only javax.lang.model types
-dontwarn javax.lang.model.SourceVersion
-dontwarn javax.lang.model.element.Element
-dontwarn javax.lang.model.element.ElementKind
-dontwarn javax.lang.model.element.Modifier
-dontwarn javax.lang.model.type.TypeMirror
-dontwarn javax.lang.model.type.TypeVisitor
-dontwarn javax.lang.model.util.SimpleTypeVisitor8
