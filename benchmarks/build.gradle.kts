plugins {
    java
    kotlin("jvm") version "1.9.20"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.10"
    kotlin("plugin.allopen") version "1.9.20"
}

repositories {
    mavenCentral()
    maven("https://releases.usethesource.io/maven/")
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
    // 2. for java_cup (?)
    implementation("java_cup:java_cup:0.9e")
    // 3. for antlr
    implementation("org.antlr:antlr4:4.13.1")
    // 4. for iguana
    implementation("io.usethesource:capsule:0.6.3")
    implementation("info.picocli:picocli:4.7.0")
    implementation("com.google.guava:guava-testlib:23.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.14.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0")
}

fun getArgs(strFolder: String): Array<String> {
    val resourcesDir = File(strFolder)
    val files = resourcesDir.listFiles()!!
    return files.map { it.toString() }.sorted().toTypedArray()
}

benchmark {
    configurations {
        named("main") {
            val dataset = "dataset"
            if (!hasProperty(dataset)) {
                println("BENCHMARKS FAILED! Set dataset folder by property '$dataset'")
            }
            else{
                param("fileName", *getArgs(property(dataset).toString()))
            }
            this.reportFormat = "csv"
            iterations = 15
            iterationTime = 1000
            iterationTimeUnit = "ms"
            warmups = 5
            outputTimeUnit = "ms"
            mode = "avgt"
            val tools = "toolName"
            if (hasProperty(tools)) {
                println("Run benchmarks for: .*${property(tools)}.*")
                include(".*${property(tools)}.*")
            }

        }
    }
    targets {
        register("main")
    }
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

kotlin { jvmToolchain(11) }