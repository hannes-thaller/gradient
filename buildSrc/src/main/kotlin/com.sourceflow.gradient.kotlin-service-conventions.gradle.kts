import com.google.protobuf.gradle.*
import java.io.FileInputStream
import java.net.URI
import java.nio.file.Paths
import java.util.*

plugins {
    kotlin("jvm")
    id("com.google.protobuf")
    `maven-publish`
    application
}

repositories {
    mavenCentral()
    maven {
        url =
            URI.create("https://sourceflow-gradient-429689067702.d.codeartifact.eu-central-1.amazonaws.com/maven/maven/")
        val authToken = loadAuthToken()
        assert(authToken.isBlank()) { "Expected non empty auth token" }
        credentials {
            username = "aws"
            password = authToken
        }
    }
}

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
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.github.microutils:kotlin-logging:1.7.9")

    implementation("org.apache.pulsar:pulsar-client:2.5.0")

    testImplementation("io.kotest:kotest-framework-engine-jvm:4.6.3")
    testImplementation("io.kotest:kotest-runner-junit5:4.6.3")
    testImplementation("io.kotest:kotest-assertions-core:4.6.3")
    testImplementation("io.kotest:kotest-property:4.6.3")
    testImplementation("io.kotest:kotest-extensions-junitxml:4.6.3")
    testImplementation("io.mockk:mockk:1.9.3")
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
    afterEvaluate {
        val gitHash = loadGitHash()
        val version = loadVersion()

        if (gitHash != version.gitHash) {
            logger.info("Incrementing the build version for new git hash $gitHash")
            val newVersion = Version(version.major, version.minor, version.patch, version.build + 1, gitHash)
            storeVersion(newVersion)
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

            url = URI.create(
                "https://sourceflow-gradient-429689067702.d.codeartifact.eu-central-1.amazonaws.com" +
                        "/maven/sourceflow-gradient-jvm/"
            )
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

fun storeVersion(version: Version) {
    val fileProjectProps = Paths.get(rootDir.path, "project.properties").toFile()

    val props = Properties()
    if (fileProjectProps.canRead()) {
        fileProjectProps.inputStream().use { props.load(it) }
    }

    props["version"] = "${version.major}.${version.minor}.${version.patch}-${version.build}"
    props["gitHash"] = version.gitHash
    fileProjectProps.outputStream().use { props.store(it, "update version") }
}

fun loadGitHash(): String {
    val cmd = arrayListOf(
        "git",
        "rev-parse",
        "HEAD"
    )

    val process = ProcessBuilder(cmd)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .start()
    process.waitFor(60, TimeUnit.SECONDS)
    return process.inputStream.bufferedReader().readText().trim()
}