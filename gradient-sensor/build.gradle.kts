import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.google.protobuf.gradle.*
import java.io.FileInputStream
import java.net.URI
import java.nio.file.Paths
import java.util.*

plugins {
    kotlin("jvm")
    id("com.google.protobuf")
    id("com.github.johnrengelman.shadow") version "7.1.0"
    `maven-publish`
    application
}

val sourceflowMaven = "https://sourceflow-gradient-429689067702.d.codeartifact.eu-central-1.amazonaws.com" +
        "/maven/sourceflow-gradient-jvm/"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.31")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common:1.5.31")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.5.2")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.5.31")
    implementation("io.grpc:grpc-all:1.28.0")
    implementation("io.grpc:grpc-kotlin-stub:0.1.1")
    implementation("com.google.protobuf:protobuf-java:3.11.1")

    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
    implementation("org.apache.pulsar:pulsar-client:2.5.0")

    implementation(project(":gradient-service-domain"))
    implementation(project(":gradient-annotations"))
    implementation("org.yaml:snakeyaml:1.29")

    implementation("io.github.classgraph:classgraph:4.8.126")
    implementation("org.ow2.asm:asm:9.2")
    implementation("org.ow2.asm:asm-commons:9.2")

    testImplementation("org.ow2.asm:asm-util:9.2")
    testImplementation("io.kotest:kotest-framework-engine-jvm:4.6.3")
    testImplementation("io.kotest:kotest-runner-junit5:4.6.3")
    testImplementation("io.kotest:kotest-assertions-core:4.6.3")
    testImplementation("io.kotest:kotest-property:4.6.3")
    testImplementation("io.kotest:kotest-extensions-junitxml:4.6.3")
    testImplementation("io.mockk:mockk:1.9.3")
}


fun loadAuthToken(): String {
    val cmd = arrayListOf(
        "aws",
        "codeartifact",
        "get-authorization-token",
        "--domain",
        "sourceflow-gradient",
        "--domain-owner",
        "429689067702",
        "--query",
        "authorizationToken",
        "--output",
        "text"
    )

    val process = ProcessBuilder(cmd)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .start()
    process.waitFor(60, TimeUnit.SECONDS)
    return process.inputStream.bufferedReader().readText()
}

data class Version(val major: Int, val minor: Int, val patch: Int, val build: Int, val gitHash: String = "") {
    companion object {
        fun fromProperties(properties: Map<String, String>): Version {
            require("version" in properties)
            require("gitHash" in properties)

            val parts = properties["version"]!!.split("-")
            val versions = parts[0].split(".")
            return Version(
                versions[0].toInt(),
                versions[1].toInt(),
                versions[2].toInt(),
                parts[1].toInt(),
                properties["gitHash"]!!
            )
        }
    }

    override fun toString(): String {
        return "${major}.${minor}.${patch}-${build}"
    }
}


fun loadVersion(): Version {
    val fileProjectProps = Paths.get(rootDir.path, "project.properties").toFile()

    var version = Version(0, 1, 0, 0)
    if (fileProjectProps.canRead()) {
        val props = Properties()
        FileInputStream(fileProjectProps).use { props.load(it) }
        val properties = props.stringPropertyNames()
            .associateWith { props.getProperty(it) }

        version = Version.fromProperties(properties)
    }

    return version
}

repositories {
    mavenCentral()
    maven {
        url = URI.create(sourceflowMaven)
        val authToken = loadAuthToken()
        assert(authToken.isBlank()) { "Expected non empty auth token" }
        credentials {
            username = "aws"
            password = authToken
        }
    }
}

tasks {
    withType<Jar> {
        manifest {
            attributes["Premain-Class"] = "org.sourceflow.gradient.sensor.Bootstrap"
            attributes["Can-Redefine-Classes"] = "true"
            attributes["Can-Retransform-Classes"] = "true"
            attributes["Can-Set-Native-Method-Prefix"] = "false"
            attributes["Implementation-Title"] = "Gradient Sensor - JVM"
            attributes["Implementation-Version"] = project.version
        }
    }

    named<ShadowJar>("shadowJar") {
        mergeServiceFiles()
        manifest {
            attributes["Premain-Class"] = "org.sourceflow.gradient.sensor.Bootstrap"
            attributes["Can-Redefine-Classes"] = "true"
            attributes["Can-Retransform-Classes"] = "true"
            attributes["Can-Set-Native-Method-Prefix"] = "false"
            attributes["Implementation-Title"] = "Gradient Sensor - JVM"
            attributes["Implementation-Version"] = project.version
            archiveClassifier.set("")
        }
        dependencies {
            exclude {
                it.name == "ch.qos.logback:logback-classic:1.2.3"
            }
        }
        minimize()
    }
}

configure<JavaApplication> {
    mainClass.set("org.sourceflow.gradient.code.BootstrapKt")
}

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks["shadowJar"])
            from(components["java"])
            groupId = "org.sourceflow"
            artifactId = project.name
            version = loadVersion().toString()
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
            url = URI.create(sourceflowMaven)
            val authToken = loadAuthToken()
            assert(authToken.isBlank()) { "Expected non empty auth token" }
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
