plugins {
    id("org.sourceflow.gradient.kotlin-service-conventions")
}

dependencies {
    implementation(project(":gradient-service-domain"))
    
    implementation("com.fasterxml.uuid:java-uuid-generator:4.0.1")
    implementation("org.mongodb:mongodb-driver-sync:4.0.2")
}


configure<JavaApplication> {
    mainClass.set("org.sourceflow.gradient.dataset.BootstrapKt")
}

