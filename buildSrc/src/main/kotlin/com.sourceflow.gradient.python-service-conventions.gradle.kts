import org.jetbrains.kotlin.konan.file.File

plugins {
    base
    idea
}


tasks.register("createCondaEnvironment") {
    inputs.file("environment.sh")
        .withPropertyName("environment file")

    val outputText: String = java.io.ByteArrayOutputStream().use { stream ->
        project.exec {
            commandLine("./environment.sh")
            standardOutput = stream
        }
        stream.toString()
    }
    print(outputText)
}
