plugins {
    java
    kotlin("jvm") version "1.9.20"
    application
}

repositories {
    mavenCentral()
    maven("https://releases.usethesource.io/maven/")
}

application{
    mainClass = "java8.GeneratorKt"
}

dependencies {
    //benchmarks tool
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.10")
    //compared projects
    // 1. for ucfs
    implementation(project(":solver"))
    implementation(project(":generator"))
    implementation(project(":examples"))
    // 2. for antlr
    implementation("org.antlr:antlr4:4.13.1")
}


kotlin { jvmToolchain(11) }