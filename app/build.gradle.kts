plugins {
    kotlin("jvm") version "1.8.0"   // Kotlin plugin for JVM support
    application                    // Application plugin to define the main entry point
}

group = "org.example"               // Set the group name for the project
version = "1.0-SNAPSHOT"            // Version of your project

repositories {
    mavenCentral()                  // Use Maven Central repository to fetch dependencies
}

dependencies {
    implementation(kotlin("stdlib"))  // Kotlin standard library for Kotlin support

    // JUnit 5 for testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")

    // Other dependencies for the application (e.g., Guava, logging)
    // implementation("com.google.guava:guava:31.1-jre")
}

application {
    // Define the main class for the application
    // Set it to the class that launches the RMI server or the main app
    mainClass.set("hagimule.App")  // Update with your actual main class
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))  // Set the Java version (if needed)
    }
}

tasks.test {
    useJUnitPlatform()  // Use JUnit 5 for running tests
}