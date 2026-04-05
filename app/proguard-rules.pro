# Google API client — keep model classes used by Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.api.** { *; }
-keep class com.google.apis.** { *; }

# Health Connect
-keep class androidx.health.connect.** { *; }

# Hilt (handled by hilt-android plugin, but explicit fallback)
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
