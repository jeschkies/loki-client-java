plugins {
    id("java-library")
    checkstyle
    id("com.diffplug.spotless") version "6.25.0"

    //id("maven-publish")
    //signing
    //id("tech.yanand.maven-central-publish") version "1.2.0"
}

// TODO: share between projects
group = "io.github.jeschkies"
version = "0.0.1"

repositories {
    mavenCentral()
}
dependencies {
    implementation("com.google.guava:guava:33.3.0-jre")
    implementation("org.testcontainers:testcontainers:1.20.1")
}

checkstyle {
    toolVersion = "10.18.1"
}

spotless {
    java {
        googleJavaFormat()
    }
}
