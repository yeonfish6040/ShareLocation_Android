plugins {
    id("com.android.application")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.yeonfish.sharelocation"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yeonfish.sharelocation"
        minSdk = 29
        targetSdk = 34
        versionCode = 64
        versionName = "1.1.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] = "AIzaSyA8M2s4M5qsUzrCkyn8692p67uh8HJRD4w"
        manifestPlaceholders["KAKAO_NATIVE_KEY"] = "7c91ec8d182523adfa1e6f0df190eff3"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    dataBinding {
        enable = true
    }

    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
    }

}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.credentials:credentials:1.3.0-alpha03")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.google.gms:google-services:4.4.1")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
    implementation("com.google.api-client:google-api-client:1.33.0")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.kakao.sdk:v2-share:2.19.0")
    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

}