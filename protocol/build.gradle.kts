plugins {
    id("com.google.protobuf")
}

dependencies {
    implementation("com.google.protobuf:protobuf-java:3.25.5")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.5"
    }
}

sourceSets {
    main {
        proto {
            srcDir("src/main/proto")
            // Exclude server-side RPC definitions (require psrpc, not needed for client SDK)
            exclude("rpc/**")
            exclude("infra/**")
            exclude("agent/**")
        }
    }
}

tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<ProcessResources>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
