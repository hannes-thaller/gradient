import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("idea")
    id("maven-publish")
    id("java")
    kotlin("jvm") version "1.3.72"
}

group = "org.sourceflow"
version = project.properties["version_${project.name}"] as String

val mavenSourceflow = Action<MavenArtifactRepository> {
    name = "sourceflow-org"
    url = uri("https://pkgs.dev.azure.com/sourceflow-org/_packaging/sourceflow-org/maven/v1")
    credentials {
        username = "sourceflow-org"
        password = System.getenv("GRADIENT_AZURE_KEY")
    }
    content {
        includeGroup("org.sourceflow")
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven(mavenSourceflow)
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.3.5")

    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.github.microutils:kotlin-logging:1.7.7")

    implementation("org.sourceflow:gradient-annotations-jvm:0.2.4")
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_11.majorVersion
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.OptIn"
    }

    withType<Test> {
        useJUnitPlatform()
    }

    named<Test>("test") {
        filter {
            excludeTestsMatching("*IntegrationTest*")
            excludeTestsMatching("*SystemTest*")
        }
    }

    create<Test>("integrationTest") {
        filter {
            includeTestsMatching("*IntegrationTest*")
        }
    }

    create<Test>("systemTest") {
        filter {
            includeTestsMatching("*SystemTest*")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                setDescription("Gradient jvm testing.")
                licenses {
                    license {
                        name.set("Copyright 2020 - Sourceflow")
                    }
                }
                developers {
                    developer {
                        id.set("hthaller")
                        name.set("Hannes Thaller")
                        email.set("hannes.thaller.at@gmail.com")
                    }
                }
            }
        }
    }
    repositories {
        maven(mavenSourceflow)
    }
}