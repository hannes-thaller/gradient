import com.google.protobuf.gradle.*
import java.net.URI

plugins {
    idea
    kotlin("jvm")
    id("com.google.protobuf")
    `maven-publish`
    application
}

repositories {
    mavenCentral()
    maven {
        url = URI.create("https://sourceflow-429689067702.d.codeartifact.eu-central-1.amazonaws.com/maven/maven/")
        val authToken: String? = System.getenv("CODEARTIFACT_AUTH_TOKEN")
        assert(authToken != null) { "Expected that the environment variable CODEARTIFACT_AUTH_TOKEN is defined" }
        credentials {
            username = "aws"
            password = authToken
        }
    }
}

dependencies {
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.5.21")
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.3.5")
    implementation("io.grpc:grpc-all:1.28.0")
    implementation("io.grpc:grpc-kotlin-stub:0.1.1")
    implementation("com.google.protobuf:protobuf-java:3.11.1")
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.github.microutils:kotlin-logging:1.7.9")

    implementation("org.apache.pulsar:pulsar-client:2.5.0")

    testImplementation("io.kotest:kotest-runner-junit5:4.6.1")
    testImplementation("io.kotest:kotest-assertions-core:4.6.1")
    testImplementation("io.kotest:kotest-property:4.6.1")
    testImplementation("io.kotest:kotest-extensions-junitxml:4.6.1")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.OptIn"
    }
    withType<Test> {
        useJUnitPlatform()
        systemProperty("gradle.build.dir", "project.buildDir")
    }
    named<Test>("test") {
        filter {
            excludeTestsMatching("*IntegrationTest*")
        }
    }

    create<Test>("integrationTest") {
        filter {
            includeTestsMatching("*IntegrationTest*")
            systemProperty("gradle.build.dir", project.buildDir)
        }
    }
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}



configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "org.sourceflow"
            artifactId = project.name
            version = "0.1.0"
            pom {
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
        maven {
            url = URI.create("https://sourceflow-429689067702.d.codeartifact.eu-central-1.amazonaws.com/maven/maven/")
            val authToken: String? = System.getenv("CODEARTIFACT_AUTH_TOKEN")
            assert(authToken != null) { "Expected that the environment variable CODEARTIFACT_AUTH_TOKEN is defined" }
            credentials {
                username = "aws"
                password = authToken
            }
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.11.4"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.26.0"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:0.1.1"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc") {}
                id("grpckt") {}
            }
        }
    }
}

