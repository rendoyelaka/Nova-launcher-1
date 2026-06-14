# Nova Launcher ProGuard Rules
-keep class com.nova.launcher.** { *; }
-keep class androidx.** { *; }
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-dontwarn kotlin.**
-dontwarn androidx.**
