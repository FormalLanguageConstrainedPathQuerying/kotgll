plugins {
  java
  application
  kotlin("jvm") version "1.9.20"
  id("me.champeau.jmh") version "0.7.2"
//  id("org.jetbrains.kotlinx.benchmark") version "0.4.10"
  kotlin("plugin.allopen") version "1.9.20"
}

repositories {
  mavenCentral()
  maven("https://releases.usethesource.io/maven/")
}

dependencies {
  testImplementation(kotlin("test"))
  testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
  implementation("java_cup:java_cup:0.9e")
  implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
  implementation("org.antlr:antlr4:4.13.1")
//  implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.10")
  implementation("io.usethesource:capsule:0.6.3")
  implementation("com.fasterxml.jackson.core:jackson-core:2.14.0")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0")
  implementation("com.google.guava:guava-testlib:23.0")
  implementation("info.picocli:picocli:4.7.0")
  implementation(kotlin("reflect"))
  jmhImplementation("org.openjdk.jmh:jmh-core:1.36")
  jmhImplementation("org.openjdk.jmh:jmh-generator-annprocess:1.36")
  jmhImplementation("org.openjdk.jmh:jmh-generator-bytecode:1.36")
}

tasks.test { useJUnitPlatform() }

kotlin { jvmToolchain(11) }
application { mainClass.set("org.srcgll.MainKt") }

configure<SourceSetContainer> {
  named("main") {
    java.srcDir("src/main/kotlin")
  }
  named("jmh") {
    kotlin.srcDir("src/jmh/kotlin")
    resources.srcDir("src/jmh/resources")
  }
}

//allOpen {
//  annotation("org.openjdk.jmh.annotations.State")
//}
//
//benchmark {
//  targets {
//    register("jvm")
//  }
//}

//benchmark {
//  configurations {
//    named("main") {
//      warmups = 5
//      iterations = 15
//      outputTimeUnit = "ns"
//      advanced("nativeGCAfterIteration", true)
//    }
//  }
//}


jmh {
  duplicateClassesStrategy = DuplicatesStrategy.EXCLUDE
  zip64 = true
//  profilers.addAll("gc", "stack")
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

tasks.withType<Jar> {
  dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources"))
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  manifest { attributes(mapOf("Main-Class" to application.mainClass)) }
  val sourcesMain = sourceSets.main.get()
  val contents =
      configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) } +
          sourcesMain.output
  from(contents)
}
