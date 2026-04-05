// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Version declarations live here so modules share a single plugin baseline.
plugins {
    id("com.android.application") version "8.11.1" apply false
    kotlin("android")            version "2.1.20" apply false
    alias(libs.plugins.compose.compiler) apply false

}
