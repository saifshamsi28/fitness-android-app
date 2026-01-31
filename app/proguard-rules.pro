####################################
# Retrofit
####################################
-keep class com.saif.fitnessapp.network.** { *; }
-keep interface com.saif.fitnessapp.network.** { *; }

####################################
# Gson
####################################
-keep class com.google.gson.** { *; }

####################################
# DTOs
####################################
-keep class com.saif.fitnessapp.network.dto.** { *; }

####################################
# AppAuth (OAuth)
####################################
-keep class net.openid.appauth.** { *; }

####################################
# Hilt
####################################
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }

####################################
# EncryptedSharedPreferences
####################################
-keep class androidx.security.crypto.** { *; }

####################################
# Paging
####################################
-keep class androidx.paging.** { *; }

####################################
# Retrofit annotations
####################################
-keepattributes Signature
-keepattributes *Annotation*

####################################
# Gson SerializedName
####################################
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

####################################
# Enums
####################################
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

####################################
# Remove logging in release
####################################
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
