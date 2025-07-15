// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {

    id("com.google.gms.google-services") version "4.4.3" apply false
    id("com.android.application") version "8.11.1" apply false
    kotlin("android")            version "2.1.20" apply false
    alias(libs.plugins.compose.compiler) apply false

}