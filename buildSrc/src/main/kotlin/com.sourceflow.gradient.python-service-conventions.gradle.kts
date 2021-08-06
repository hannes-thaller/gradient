import com.pswidersk.gradle.python.VenvTask
import groovy.util.Node

plugins {
    base
    idea
    id("com.pswidersk.python-plugin")
}

idea {
    module {
        iml {
            withXml {
                val root = asNode().children().first() as Node
                val entry = Node(
                    root, "orderEntry", mapOf(
                        "jdkName" to "Python 3.7 (gradient-service-model)",
                        "jdkType" to "Python SDK"
                    )
                )

            }
        }
    }
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