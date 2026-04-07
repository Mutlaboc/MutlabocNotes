// Корневой файл сборки, где задаются общие параметры для всех модулей.
// Объявления версий находятся здесь, чтобы модули использовали единый набор плагинов.
plugins {
    id("com.android.application") version "8.11.1" apply false
    kotlin("android")            version "2.1.20" apply false
    alias(libs.plugins.compose.compiler) apply false

}
