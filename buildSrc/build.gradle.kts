plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation("gradle.plugin.com.google.protobuf:protobuf-gradle-plugin:0.8.17")
    implementation("ru.vyarus:gradle-use-python-plugin:2.3.0")
}
