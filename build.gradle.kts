plugins {
    id("java")
    checkstyle
    id("com.diffplug.spotless") version "6.25.0"
    id("maven-publish")
}

group = "io.github.jeschkies"
version = "0.0.1-SNAPSHOT"

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



publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "loki-client"
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
                developers {
                    developer {
                        id = "jeschkies"
                    }
                }
                scm {
                    connection = "scm:git:git@github.com:jeschkies/loki-client-java.git"
                    developerConnection= "scm:git:git@github.com:jeschkies/loki-client-java.git"
                    url = "https://github.com/jeschkies/loki-client-java"
                }
            }
        }
    }
}

