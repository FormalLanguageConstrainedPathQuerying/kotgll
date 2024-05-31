plugins {
    java
    kotlin("jvm") version "1.9.20"
    application
}

application{
    mainClass = "java8.GeneratorKt"
}
repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":solver"))
    implementation(project(":generator"))
}

kotlin { jvmToolchain(11) }
