plugins {

    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "kr.ac.uc.money_tracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "kr.ac.uc.money_tracker"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:32.7.2")) // ✅ BOM 사용 (최신)
    implementation("com.google.firebase:firebase-auth-ktx") // ✅ Firebase 인증용
    implementation("com.google.android.gms:play-services-auth:21.0.0") // ✅ 구글 로그인용(Optional)
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation ("com.google.firebase:firebase-storage:20.3.0")
}