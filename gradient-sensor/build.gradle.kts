plugins {
    id("com.sourceflow.gradient.kotlin-service-conventions")
}

dependencies {
    implementation(project(":gradient-service-domain"))
}


configure<JavaApplication> {
    mainClass.set("org.sourceflow.gradient.code.BootstrapKt")
}

