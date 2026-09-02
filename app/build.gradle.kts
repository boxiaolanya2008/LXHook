plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "github.boxiaolanya2008.lingxihook"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "github.boxiaolanya2008.lingxihook"
        minSdk = 33
        targetSdk = 37
        versionCode = 4
        versionName = "1.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // compileOnly：Hook 实现类由框架在宿主进程内注入，打进 APK 反而会和框架冲突
    compileOnly(libs.libxposed.api)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.haze)
    implementation(libs.miuix.blur)
    implementation(libs.materialkolor) {
        // materialkolor 传递依赖的 Compose Multiplatform material3/foundation 与 androidx 冲突，
        // 其内部 MaterialExpressiveTheme 为 internal；排除后统一使用 BOM 提供的 androidx material3
        exclude(group = "org.jetbrains.compose.material3")
        exclude(group = "org.jetbrains.compose.foundation")
        exclude(group = "org.jetbrains.compose.runtime")
        exclude(group = "org.jetbrains.compose.ui")
        exclude(group = "com.github.ajalt.colormath")
    }
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}