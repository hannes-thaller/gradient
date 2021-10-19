import java.net.URI

plugins {
    id("org.sourceflow.gradient.kotlin-service-conventions")
}

val sourceflowMaven = "https://sourceflow-gradient-429689067702.d.codeartifact.eu-central-1.amazonaws.com" +
        "/maven/sourceflow-gradient-jvm/"

val gradient: Configuration by configurations.creating
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


dependencies {
    implementation("org.sourceflow:gradient-annotations:0.1.0-6")
    gradient(project(":gradient-sensor"))
}

tasks {
    application {
        val gradientConfig = "src/main/resources/gradient.yaml"
        mainClass.set("org.sourceflow.gradient.testing.nutrition.Main")
        applicationDefaultJvmArgs = listOf("-javaagent:${gradient.first().absoluteFile}=${gradientConfig}")
    }
}