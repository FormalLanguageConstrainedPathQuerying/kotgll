plugins {
    kotlin("jvm") version "1.9.20"
}


version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":solver"))
    implementation("com.squareup:kotlinpoet:1.16.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}