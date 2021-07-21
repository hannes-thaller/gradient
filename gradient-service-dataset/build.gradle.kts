plugins {
    id("com.sourceflow.gradient.kotlin-service-conventions")
}

dependencies {
    implementation("org.mongodb:mongodb-driver-sync:4.0.2")

//    implementation("org.sourceflow:gradient-dataset-api:0.1.0")
//    implementation("org.sourceflow:gradient-code-api:0.1.0")
//    implementation("org.sourceflow:gradient-monitoring-api:0.1.0")
//    implementation("org.sourceflow:gradient-introspect-api:0.1.0")


//    testImplementation("org.sourceflow:gradient-code-api:0.1.0")
}


configure<JavaApplication> {
    mainClass.set("org.sourceflow.gradient.dataset.BootstrapKt")
}

