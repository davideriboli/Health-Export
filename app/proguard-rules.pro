# ── Google API client — keep model classes used by Gson ───────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.api.** { *; }
-keep class com.google.apis.** { *; }
-keep class com.google.api.client.** { *; }

# ── Gson — serialises Google API client models ────────────────────────────────
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── Suppress expected warnings from Google / Apache / Sun internals ───────────
-dontwarn com.google.api.**
-dontwarn com.google.apis.**
-dontwarn com.google.api.client.**
-dontwarn sun.misc.**
-dontwarn org.apache.**
-dontwarn com.fasterxml.**

# ── Health Connect ────────────────────────────────────────────────────────────
-keep class androidx.health.connect.** { *; }

# ── WorkManager — keep worker constructors for Hilt factory reflection ────────
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── Hilt (the plugin handles most of this, explicit fallback) ─────────────────
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# ── Kotlin coroutines ─────────────────────────────────────────────────────────
-dontwarn kotlinx.coroutines.**

# ── Google Play Services / Credential Manager ─────────────────────────────────
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**
