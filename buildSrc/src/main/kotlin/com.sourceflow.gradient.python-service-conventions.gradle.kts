import com.pswidersk.gradle.python.VenvTask
import groovy.util.Node

plugins {
    base
    id("com.pswidersk.python-plugin")
}

pythonPlugin {
    pythonVersion.set("3.7")
}

tasks {
    register<VenvTask>("listDependencies") {
        venvExec = "conda"
        args = listOf("list")
    }

    register<VenvTask>("installDependencies") {
        venvExec = "conda"
        args = listOf("env", "update", "--file", "requirements.yaml", "--prune")
    }
}