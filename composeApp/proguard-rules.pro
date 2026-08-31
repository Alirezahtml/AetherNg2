# Essential rules for Compose Desktop
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep your model classes for serialization using annotations
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# Prevent shrinking of resources and JNI
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.ReadOnlyComposable *;
}

# Ignore warnings from common libraries if necessary
-dontwarn okio.**
-dontwarn okhttp3.**
-dontwarn kotlinx.serialization.**
-dontwarn javax.annotation.**
-dontwarn com.squareup.moshi.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn dalvik.system.**
-dontwarn com.jetbrains.**
-dontwarn io.github.immaghzbad.aetherst.platform.PlatformContext
-dontwarn io.github.immaghzbad.aetherst.shared.ui.AetherViewModel
-dontwarn io.github.immaghzbad.aetherst.shared.ui.OnboardingViewModel
-dontwarn io.github.immaghzbad.aetherst.platform.SystemUtils
-dontwarn kotlinx.datetime.Month
-dontwarn kotlinx.datetime.OverloadMarker
-dontwarn org.jetbrains.compose.resources.DrawableResource
-dontwarn org.jetbrains.compose.resources.FontResource
-dontwarn org.jetbrains.compose.resources.StringResource
-dontwarn org.jetbrains.compose.resources.PluralStringResource
-dontwarn org.jetbrains.compose.resources.StringArrayResource
