plugins {
    `java-library`
}

dependencies {
    api(project(":core"))
    
    // Protobuf runtime (transitive from :protocol, but needed at compile time)
    implementation("com.google.protobuf:protobuf-java:3.25.5")
    
    // WebSocket client
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
    
    // JSON for token parsing
    implementation("com.google.code.gson:gson:2.11.0")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
