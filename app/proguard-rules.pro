# Project specific ProGuard rules
# ---------------------------------------------------------------------------------

# 1. Keep Model Classes (Firestore, GSON, and Room need original names)
-keep class com.example.pace.model.** { *; }

# 2. Keep Attributes for Reflection (Serialization/Deserialization)
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses

# 3. Firebase & Firestore
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# 4. GSON
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 5. OSMDroid (Map)
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# 6. MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# 7. Google Gemini AI & Guava
-keep class com.google.ai.client.generativeai.** { *; }
-keep class com.google.common.util.concurrent.** { *; }
-dontwarn com.google.ai.client.generativeai.**
-dontwarn com.google.common.**

# 8. Room Database
-keep class androidx.room.** { *; }
-dontwarn androidx.sqlite.db.**

# 9. General Debugging
-keepattributes SourceFile, LineNumberTable
