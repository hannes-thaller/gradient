plugins {
    id("com.sourceflow.gradient.kotlin-service-conventions")
}

dependencies {
    implementation(project(":gradient-service-api"))

    implementation("org.mongodb:mongodb-driver-sync:4.0.2")
}

configure<JavaApplication> {
    mainClass.set("org.sourceflow.gradient.project.BootstrapKt")
}
