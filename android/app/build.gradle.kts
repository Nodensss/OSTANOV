import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Номер сборки берётся из GitHub Actions, локально — 1.
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
// Единый источник данных с веб-версией: <корень репозитория>/data/stop-algorithm.json.
val algorithmDataDir = rootProject.file("../data")
// Подпись release-сборки — только если в окружении есть keystore (секреты CI).
val releaseKeystore = System.getenv("UPE_KEYSTORE_PATH")?.let(::file)?.takeIf { it.exists() }

android {
    namespace = "ru.upestop.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.upestop.app"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "0.1.$buildNumber"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("UPE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("UPE_KEY_ALIAS")
                keyPassword = System.getenv("UPE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    sourceSets {
        getByName("main") {
            assets.srcDir(algorithmDataDir)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        // Robolectric-тесты интерфейса читают ресурсы и assets (stop-algorithm.json) приложения.
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        abortOnError = true
        // Версии зафиксированы осознанно (проверенная связка, см. ТЗ 5.1),
        // а приложение распространяется APK-файлом, не через Google Play.
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "ExpiredTargetSdkVersion")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.withType<Test>().configureEach {
    systemProperty("algorithm.json", File(algorithmDataDir, "stop-algorithm.json").absolutePath)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
