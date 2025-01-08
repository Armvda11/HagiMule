plugins {
    kotlin("jvm") version "1.8.0"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("com.google.guava:guava:33.3.1-jre")
    implementation("org.xerial:sqlite-jdbc:3.41.2.1")
    implementation("com.github.luben:zstd-jni:1.5.5-1")
    implementation ("org.tukaani:xz:1.9")
    implementation("org.lz4:lz4-java:1.7.1")
}

application {
    mainClass.set("hagimule.App")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "hagimule.App"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}