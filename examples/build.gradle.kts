plugins {

    java
    kotlin("jvm") version "1.9.20"
    application
}

group = "org.example"
version = "unspecified"

application{
    mainClass = "java7.GeneratorKt"
}
repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":solver"))
    implementation(project(":generator"))
}

