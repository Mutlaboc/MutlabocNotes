// Набор плагинов для Android-приложения, Kotlin, обработки аннотаций и Compose.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id ("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.compose")

}

// Конфигурация Android-модуля: id пакета, версии SDK, типы сборок и Compose.
android {
    namespace = "com.example.mutlabocsnotes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mutlabocsnotes"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Берём идентификатор OAuth-клиента из локальных/CI-свойств, с резервным значением для разработки.
        val yandexClientId =
            (project.findProperty("YANDEX_CLIENT_ID") as String?) ?: "776676c1ec6c4097ba260b05824f3a39"

        manifestPlaceholders["YANDEX_CLIENT_ID"] = yandexClientId
        buildConfigField("String", "YANDEX_CLIENT_ID", "\"$yandexClientId\"")
    }

    buildTypes {
        getByName("release") {
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

}

// Зависимости функциональности, сгруппированные по платформе, сети, хранению и тестам.
dependencies {

    implementation(libs.androidx.material3.android)
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("com.google.android.gms:play-services-auth:21.3.0")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material")

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("androidx.navigation:navigation-compose:2.5.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation ("androidx.room:room-runtime:2.6.1")
    kapt ("androidx.room:room-compiler:2.6.1")
    implementation ("androidx.room:room-ktx:2.6.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("com.yandex.android:authsdk:3.1.3")
}
