plugins {
    `java-library`
}

dependencies {
    api(project(":protocol"))
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
