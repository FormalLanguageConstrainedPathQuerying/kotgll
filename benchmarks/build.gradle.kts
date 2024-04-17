plugins {
    java
    kotlin("jvm") version "1.9.20"
    id("me.champeau.jmh") version "0.7.2"
    kotlin("plugin.allopen") version "1.9.20"
}

repositories {
    mavenCentral()
    maven("https://releases.usethesource.io/maven/")
}

dependencies {
    implementation(project(":solver"))
    implementation("java_cup:java_cup:0.9e")
    implementation("org.antlr:antlr4:4.13.1")
    jmhImplementation("org.openjdk.jmh:jmh-core:1.36")
    jmhImplementation("org.openjdk.jmh:jmh-generator-annprocess:1.36")
    jmhImplementation("org.openjdk.jmh:jmh-generator-bytecode:1.36")
}
kotlin { jvmToolchain(11) }

configure<SourceSetContainer> {
    named("jmh") {
        kotlin.srcDir("benchmarks/src/jmh/kotlin")
        resources.srcDir("benchmarks/src/jmh/resources")
    }
}

jmh {
    duplicateClassesStrategy = DuplicatesStrategy.EXCLUDE
    zip64 = true
    warmupForks = 0
    warmupBatchSize = 1
    warmupIterations = 5
    warmup = "0s"
    timeOnIteration = "0s"
    fork = 1
    batchSize = 1
    iterations = 15
    verbosity = "EXTRA"
    jmhTimeout = "300s"
    benchmarkMode.addAll("ss")
    failOnError = false
    forceGC = true
    resultFormat = "CSV"
    jvmArgs.addAll("-Xmx4096m", "-Xss4m", "-XX:+UseG1GC")
}

tasks.processJmhResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}