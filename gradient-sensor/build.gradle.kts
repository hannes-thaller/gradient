plugins {
    id("com.sourceflow.gradient.kotlin-service-conventions")
}

dependencies {
    implementation(project(":gradient-service-domain"))
    implementation(project(":gradient-annotations"))
    implementation("org.yaml:snakeyaml:1.29")

    implementation("io.github.classgraph:classgraph:4.8.123")
    implementation("org.ow2.asm:asm:9.2")
    implementation("org.ow2.asm:asm-commons:9.2")

    testImplementation("org.ow2.asm:asm-util:9.2")
}
repositories {
    mavenCentral()
}


configure<JavaApplication> {
    mainClass.set("org.sourceflow.gradient.code.BootstrapKt")
}

