plugins {
    `java-library`
}

dependencies {
    api(project(":protocol"))

    // Protobuf runtime (needed at compile time for proto message handling)
    implementation("com.google.protobuf:protobuf-java:3.25.5")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
