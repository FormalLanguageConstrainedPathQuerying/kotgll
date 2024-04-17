plugins {
    java
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.allopen") version "1.9.20"
}

repositories {
    mavenCentral()
    maven("https://releases.usethesource.io/maven/")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
    implementation("io.usethesource:capsule:0.6.3")
    implementation("com.fasterxml.jackson.core:jackson-core:2.14.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0")
    implementation("com.google.guava:guava-testlib:23.0")
    implementation("info.picocli:picocli:4.7.0")
    implementation(kotlin("reflect"))
    implementation("com.squareup:kotlinpoet:1.16.0")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0")
}


configure<SourceSetContainer> {
    named("main") {
        java.srcDir("solver/src/main/kotlin")
    }
}

tasks.test { useJUnitPlatform() }