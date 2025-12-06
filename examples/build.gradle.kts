plugins {
    application
}

dependencies {
    implementation(project(":rtc"))
}

application {
    mainClass.set("io.livekit.examples.BasicRoomExample")
}

// Allow running different examples
tasks.register<JavaExec>("runPublish") {
    mainClass.set("io.livekit.examples.PublishExample")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("runSubscribe") {
    mainClass.set("io.livekit.examples.SubscribeExample")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("runDataChannel") {
    mainClass.set("io.livekit.examples.DataChannelExample")
    classpath = sourceSets["main"].runtimeClasspath
}
