plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.21")
    implementation("gradle.plugin.com.google.protobuf:protobuf-gradle-plugin:0.8.17")
}
