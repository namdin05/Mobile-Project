import java.util.Properties
import java.io.FileInputStream

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


        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(FileInputStream(localPropertiesFile))
        }

        // 2. Tạo biến toàn cục (Lưu ý: Bắt buộc phải có dấu \" ở quanh biến)
        buildConfigField("String", "BASE_URL", "\"${properties.getProperty("SUPABASE_BASE_URL")}\"")
        buildConfigField("String", "API_KEY", "\"${properties.getProperty("SUPABASE_API_KEY")}\"")


    }

    buildFeatures {
        buildConfig = true
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

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // THƯ VIỆN MỚI CHO MVVM (ViewModel & LiveData)
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")
}