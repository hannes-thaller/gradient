plugins {
    id("com.sourceflow.gradient.kotlin-service-conventions")
}

dependencies {
    implementation(project(":gradient-service-domain"))
    implementation(project(":gradient-annotations"))
    implementation("org.yaml:snakeyaml:1.26")

    implementation("io.github.classgraph:classgraph:4.8.77")
    implementation("org.ow2.asm:asm:7.2")
    implementation("org.ow2.asm:asm-commons:7.2")

    testImplementation("org.ow2.asm:asm-util:7.2")
}


configure<JavaApplication> {
    mainClass.set("org.sourceflow.gradient.code.BootstrapKt")
}

