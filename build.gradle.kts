import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

plugins {
    id("java-library")
    checkstyle
    id("com.diffplug.spotless") version "6.25.0"

    id("maven-publish")
    signing
    id("tech.yanand.maven-central-publish") version "1.2.0"
}

group = "io.github.jeschkies"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.google.guava:guava:33.3.0-jre")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.testcontainers:testcontainers:1.20.1")
}

checkstyle {
    toolVersion = "10.18.1"
}

spotless {
    java {
        googleJavaFormat()
    }
}

tasks.test {
    useJUnitPlatform()
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            version = "0.0.1"

            from(components["java"])

            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name = "Loki Client"
                description = "Loki Java client that sends and retrieves logs to and from a running Loki server"
                url = "https://www.github.com/jeschkies/loki-client-java"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                signing {
                    sign(publishing.publications["mavenJava"])
                    sign(configurations["archives"])
                }
                developers {
                    developer {
                        id = "jeschkies"
                        name = "Karsten Jeschkies"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com:jeschkies/loki-client-java.git"
                    developerConnection= "scm:git:ssh://github.com:jeschkies/loki-client-java.git"
                    url = "https://github.com/jeschkies/loki-client-java"
                }
            }
        }
    }

    repositories {
        maven {
            name = "Local"
            url = uri(layout.buildDirectory.dir("repos/bundles"))
        }
    }
}

signing {
    val keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
    val secretKey = System.getenv("SIGNING_KEY")
    useInMemoryPgpKeys(secretKey, keyPassword)
    sign(publishing.publications["mavenJava"])
}

mavenCentral {
    repoDir = layout.buildDirectory.dir("repos/bundles")
    // TODO: DO NOT CHECK IN
    @OptIn(ExperimentalEncodingApi::class)
    authToken = Base64.Default.encode("".encodeToByteArray())
    publishingType = "AUTOMATIC"
}
