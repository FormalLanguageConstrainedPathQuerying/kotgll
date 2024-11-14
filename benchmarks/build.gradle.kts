plugins {
    java
    kotlin("jvm") version "1.9.20"
    application
}

application{
    mainClass = "org.ucfs.GeneratorKt"
}

repositories {
    mavenCentral()
    maven("https://releases.usethesource.io/maven/")
}

dependencies {
    // Other dependencies.
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    //compared projects
    // 1. for ucfs
    implementation(project(":solver"))
    implementation(project(":generator"))
    implementation(project(":examples"))
    // 2. for antlr
    implementation("org.antlr:antlr4:4.13.1")
    implementation(kotlin("stdlib-jdk8"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}