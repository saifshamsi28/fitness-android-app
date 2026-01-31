import java.util.Properties

val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")

if (localPropsFile.exists()) {
    localProps.load(localPropsFile.inputStream())
}

plugins {
    id("com.android.application")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.saif.fitnessapp"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.saif.fitnessapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        manifestPlaceholders["appAuthRedirectScheme"] = "com.saif.fitnessapp"

        // Emulator URLs
        buildConfigField(
            "String",
            "KEYCLOAK_EMULATOR_URL",
            "\"${localProps["KEYCLOAK_EMULATOR_URL"] ?: "http://10.0.2.2:8181"}\""
        )

        buildConfigField(
            "String",
            "API_EMULATOR_URL",
            "\"${localProps["API_EMULATOR_URL"] ?: "http://10.0.2.2:8080"}\""
        )

        // Physical Device URLs
        buildConfigField(
            "String",
            "KEYCLOAK_DEVICE_URL",
            "\"${localProps["KEYCLOAK_DEVICE_URL"] ?: "http://10.163.19.205:8181"}\""
        )

        buildConfigField(
            "String",
            "API_DEVICE_URL",
            "\"${localProps["API_DEVICE_URL"] ?: "http://10.163.19.205:8080"}\""
        )

        // Common config
        buildConfigField(
            "String",
            "KEYCLOAK_REALM",
            "\"${localProps["KEYCLOAK_REALM"] ?: "fitness-app"}\""
        )

        buildConfigField(
            "String",
            "KEYCLOAK_CLIENT_ID",
            "\"${localProps["KEYCLOAK_CLIENT_ID"] ?: "oauth2-pkce-client"}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Core Android
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.11.0")

    // Architecture Components
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.fragment:fragment:1.6.2")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.50")
    annotationProcessor("com.google.dagger:hilt-compiler:2.50")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.10.0")
    implementation("com.squareup.retrofit2:converter-gson:2.10.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Security & Storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Paging
    implementation("androidx.paging:paging-runtime:3.2.1")
    implementation("androidx.paging:paging-rxjava3:3.2.1")

    // RxJava
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")

    // OAuth2 with AppAuth
    implementation("net.openid:appauth:0.11.0")

    // Chrome Custom Tabs
    implementation("androidx.browser:browser:1.7.0")

    // androidx swiperefreshlayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0")

    // Shimmer Loading
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}