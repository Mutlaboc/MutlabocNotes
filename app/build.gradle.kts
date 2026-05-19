import java.io.File
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
    val environmentProperty = providers.environmentVariable(name).orNull
    return gradleProperty?.takeIf { it.isNotBlank() }
        ?: localProperty?.takeIf { it.isNotBlank() }
        ?: environmentProperty?.takeIf { it.isNotBlank() }
}

fun String.asBuildConfigString(): String {
    return "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

val broadValidationTasks = setOf("assemble", "build", "check", "test", "lint")
val requestedTaskNames = gradle.startParameter.taskNames.map { it.substringAfterLast(":") }

fun String.asFlavorTaskPart(): String =
    replaceFirstChar { firstChar -> firstChar.uppercase() }

fun taskRequestsFlavor(flavorName: String): Boolean {
    val flavorTaskPart = flavorName.asFlavorTaskPart()
    return requestedTaskNames.any { taskName ->
        taskName.contains(flavorTaskPart)
    }
}

fun taskRequestsAllFlavors(): Boolean {
    return requestedTaskNames.any { taskName ->
        taskName in broadValidationTasks
    }
}

fun shouldRequireFlavorConfig(flavorName: String): Boolean {
    if (requestedTaskNames.isEmpty()) {
        return false
    }
    return taskRequestsFlavor(flavorName) || taskRequestsAllFlavors()
}

fun requiredConfigProperty(flavorName: String, name: String): String {
    configProperty(name)?.let { return it }
    if (shouldRequireFlavorConfig(flavorName)) {
        throw GradleException(
            "Missing $name for $flavorName flavor. Set it in local.properties, " +
                "a Gradle property (-P$name=...), or an environment variable."
        )
    }
    return ""
}

data class EnvironmentConfig(
    val backendBaseUrl: String,
    val googleWebClientId: String,
    val yandexClientId: String
)

data class ReleaseSigningConfig(
    val storeFile: File,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String
)

fun releaseSigningConfig(): ReleaseSigningConfig? {
    val storeFilePath = configProperty("ANDROID_KEYSTORE_FILE")
    val storePassword = configProperty("ANDROID_KEYSTORE_PASSWORD")
    val keyAlias = configProperty("ANDROID_KEY_ALIAS")
    val keyPassword = configProperty("ANDROID_KEY_PASSWORD")

    if (
        storeFilePath.isNullOrBlank() ||
        storePassword.isNullOrBlank() ||
        keyAlias.isNullOrBlank() ||
        keyPassword.isNullOrBlank()
    ) {
        return null
    }

    val signingStoreFile = File(storeFilePath).let { file ->
        if (file.isAbsolute) file else rootProject.file(storeFilePath)
    }

    return if (signingStoreFile.isFile) {
        ReleaseSigningConfig(
            storeFile = signingStoreFile,
            storePassword = storePassword,
            keyAlias = keyAlias,
            keyPassword = keyPassword
        )
    } else {
        null
    }
}

fun environmentConfig(
    flavorName: String,
    prefix: String,
    backendFallback: String? = null
): EnvironmentConfig {
    return EnvironmentConfig(
        backendBaseUrl = configProperty("${prefix}_BACKEND_URL")
            ?: backendFallback
            ?: requiredConfigProperty(flavorName, "${prefix}_BACKEND_URL"),
        googleWebClientId = requiredConfigProperty(flavorName, "${prefix}_GOOGLE_WEB_CLIENT_ID"),
        yandexClientId = requiredConfigProperty(flavorName, "${prefix}_YANDEX_CLIENT_ID")
    )
}

val environmentConfigs = mapOf(
    "dev" to environmentConfig(
        flavorName = "dev",
        prefix = "DEV",
        backendFallback = "http://10.0.2.2:8080/"
    ),
    "stage" to environmentConfig(
        flavorName = "stage",
        prefix = "STAGE"
    ),
    "prod" to environmentConfig(
        flavorName = "prod",
        prefix = "PROD"
    )
)
val releaseSigning = releaseSigningConfig()

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
                manifestPlaceholders["USES_CLEARTEXT_TRAFFIC"] = (flavorName == "dev").toString()
            }
        }
    }

    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = releaseSigning.storeFile
                storePassword = releaseSigning.storePassword
                keyAlias = releaseSigning.keyAlias
                keyPassword = releaseSigning.keyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (releaseSigning != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
