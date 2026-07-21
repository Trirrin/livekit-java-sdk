plugins {
    java
    `maven-publish`
    signing
    id("com.google.protobuf") version "0.9.4" apply false
    id("com.diffplug.spotless") version "6.25.0"
}

// Gradle always defines a `version` property (defaulting to "unspecified"), so a plain
// elvis fallback would never fire. Treat the default as absent instead.
val releaseVersion =
    findProperty("version")
        ?.toString()
        ?.takeUnless { it.isBlank() || it == Project.DEFAULT_VERSION }
        ?.removePrefix("v")

allprojects {
    group = "com.github.Trirrin.livekit-java-sdk"
    version = releaseVersion ?: "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "com.diffplug.spotless")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).apply {
            encoding = "UTF-8"
            addStringOption("Xdoclint:none", "-quiet")
        }
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            target("src/main/java/**/*.java", "src/test/java/**/*.java")
            googleJavaFormat("1.22.0")
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    // Skip publishing for examples module
    if (project.name != "examples") {
        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])

                    pom {
                        name.set("LiveKit Java SDK - ${project.name}")
                        description.set(
                            "Unofficial Java client SDK for the LiveKit real-time communication platform",
                        )
                        url.set("https://github.com/Trirrin/livekit-java-sdk")

                        licenses {
                            license {
                                name.set("Apache License, Version 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0")
                            }
                        }

                        developers {
                            developer {
                                id.set("Trirrin")
                                name.set("Trirrin")
                                url.set("https://github.com/Trirrin")
                            }
                        }

                        scm {
                            connection.set("scm:git:git://github.com/Trirrin/livekit-java-sdk.git")
                            developerConnection.set("scm:git:ssh://github.com/Trirrin/livekit-java-sdk.git")
                            url.set("https://github.com/Trirrin/livekit-java-sdk")
                        }
                    }
                }
            }

            repositories {
                maven {
                    name = "local"
                    url = uri(layout.buildDirectory.dir("repo"))
                }
            }
        }

        configure<SigningExtension> {
            // Only sign if credentials are available (skip on JitPack)
            setRequired({ gradle.taskGraph.hasTask("publish") && !System.getenv("JITPACK").toBoolean() })
            val publishing = extensions.getByType<PublishingExtension>()
            sign(publishing.publications["maven"])
        }
    }
}

// Root project spotless for build files
spotless {
    kotlinGradle {
        target("*.gradle.kts", "**/build.gradle.kts")
        ktlint("1.2.1")
    }
}
