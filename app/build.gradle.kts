import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun configProperty(name: String): String? {
    val gradleProperty = providers.gradleProperty(name).orNull
    val localProperty = localProperties.getProperty(name)
    return gradleProperty?.takeIf { it.isNotBlank() }
        ?: localProperty?.takeIf { it.isNotBlank() }
}

fun String.asBuildConfigString(): String {
    return "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

data class EnvironmentConfig(
    val backendBaseUrl: String,
    val googleWebClientId: String,
    val yandexClientId: String
)

val productionBackendBaseUrl =
    configProperty("PROD_BACKEND_URL")
        ?: configProperty("BACKEND_URL")
        ?: "https://homenoteapp.ru/"
val productionGoogleWebClientId =
    configProperty("PROD_GOOGLE_WEB_CLIENT_ID")
        ?: configProperty("GOOGLE_WEB_CLIENT_ID")
        ?: "822837772778-f7lc8b9nnbpn1u65njf7agkj392dub8c.apps.googleusercontent.com"
val productionYandexClientId =
    configProperty("PROD_YANDEX_CLIENT_ID")
        ?: configProperty("YANDEX_CLIENT_ID")
        ?: "776676c1ec6c4097ba260b05824f3a39"

fun environmentConfig(
    prefix: String,
    backendFallback: String
): EnvironmentConfig {
    return EnvironmentConfig(
        backendBaseUrl = configProperty("${prefix}_BACKEND_URL") ?: backendFallback,
        googleWebClientId = configProperty("${prefix}_GOOGLE_WEB_CLIENT_ID")
            ?: productionGoogleWebClientId,
        yandexClientId = configProperty("${prefix}_YANDEX_CLIENT_ID")
            ?: productionYandexClientId
    )
}

val environmentConfigs = mapOf(
    "dev" to environmentConfig(
        prefix = "DEV",
        backendFallback = "http://10.0.2.2:8080/"
    ),
    "stage" to environmentConfig(
        prefix = "STAGE",
        backendFallback = productionBackendBaseUrl
    ),
    "prod" to environmentConfig(
        prefix = "PROD",
        backendFallback = productionBackendBaseUrl
    )
)

android {
    namespace = "com.example.mutlabocsnotes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mutlabocsnotes"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "environment"
    productFlavors {
        environmentConfigs.forEach { (flavorName, config) ->
            create(flavorName) {
                dimension = "environment"
                buildConfigField(
                    "String",
                    "BACKEND_BASE_URL",
                    config.backendBaseUrl.asBuildConfigString()
                )
                buildConfigField(
                    "String",
                    "GOOGLE_WEB_CLIENT_ID",
                    config.googleWebClientId.asBuildConfigString()
                )
                buildConfigField(
                    "String",
                    "YANDEX_CLIENT_ID",
                    config.yandexClientId.asBuildConfigString()
                )
                resValue("string", "google_web_client_id", config.googleWebClientId)
                manifestPlaceholders["YANDEX_CLIENT_ID"] = config.yandexClientId
            }
        }
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

    androidResources {
        generateLocaleConfig = true
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.material)
    implementation(libs.play.services.auth)
    implementation(libs.yandex.authsdk)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
