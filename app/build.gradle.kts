plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.melodix.app"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.melodix.app"
        minSdk = 29
        targetSdk = 36
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
    // 1. CÁC THƯ VIỆN HIỆN CÓ CỦA BẠN (Giữ nguyên, không đụng tới)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // 2. THÊM CÁC THƯ VIỆN GPT ĐỀ XUẤT (Đã chuẩn hóa cú pháp ngoặc tròn)
    implementation("androidx.core:core:1.18.0")

    // UI Components (Cho danh sách và giao diện cuộn mượt)
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.3.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Lifecycle (Bắt buộc cho kiến trúc MVVM)
    implementation("androidx.lifecycle:lifecycle-runtime:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.10.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.10.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.10.0")

    // Media3 (Dùng riêng cho task Điều khiển nhạc của bạn)
    implementation("androidx.media3:media3-exoplayer:1.9.2")
    implementation("androidx.media3:media3-ui:1.9.2")
    implementation("androidx.media3:media3-session:1.9.2")

    // Glide (Dùng để load ảnh bìa nghệ sĩ, album)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
}