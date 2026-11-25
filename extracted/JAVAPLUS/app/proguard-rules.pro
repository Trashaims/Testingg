# Basic ProGuard configuration for Android
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Keep native methods and classes containing them
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep LSParanoid annotations and obfuscated classes
-keep @org.lsposed.lsparanoid.Obfuscate class *
-keepclassmembers class * {
    @org.lsposed.lsparanoid.Obfuscate *;
}

# Keep critical project classes
-keep class com.bgmi.MAct { *; }
-keep class com.bgmi.LogAct { *; }
-keep class com.bgmi.BoxApplication { *; }

# Keep all classes in floating package (ESP and UI Overlay)
# Obfuscation here often breaks WindowManager and custom View logic
-keep class com.bgmi.floating.** { *; }

# Keep classes used by reflection or JNI in utils
-keep class com.bgmi.utils.** { *; }

# Keep server related classes if they use JSON/Reflection
-keep class com.bgmi.server.** { *; }

# Keep SuperUser (libsu)
-keep class com.topjohnwu.superuser.** { *; }

# Keep BlackReflection and related
-keep class com.github.CodingGay.BlackReflection.** { *; }
-keep class com.zcore.** { *; }

# Keep standard Android classes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Keep R class members
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Handle generic ProGuard issues with OkHttp/Okio if used
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
