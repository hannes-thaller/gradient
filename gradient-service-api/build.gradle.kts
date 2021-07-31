
plugins {
    id("com.sourceflow.gradient.kotlin-service-conventions")
}


dependencies {
    api("com.google.protobuf:protobuf-java:3.11.1")
    implementation("io.grpc:grpc-protobuf:1.28.1")
    implementation("io.grpc:grpc-netty-shaded:1.28.1")
    implementation("io.grpc:grpc-stub:1.28.1")
}
