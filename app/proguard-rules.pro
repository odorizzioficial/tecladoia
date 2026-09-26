# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.odorizzioficial.tecladoia.** {
    *** Companion;
}
-keepclasseswithmembers class com.odorizzioficial.tecladoia.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# AccessibilityService entry point
-keep class com.odorizzioficial.tecladoia.service.** { *; }
