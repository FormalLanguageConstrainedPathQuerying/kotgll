plugins {
    java
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.allopen") version "1.9.20"
    id("me.champeau.jmh") version "0.7.2"
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
    // 2. for antlr
    implementation("org.antlr:antlr4:4.13.1")
}

fun getArgs(strFolder: String): List<String> {
    val resourcesDir = File(strFolder)
    val files = resourcesDir.listFiles()!!
    return files.map { it.toString() }.sorted().toList()
}
jmh {
    duplicateClassesStrategy = DuplicatesStrategy.EXCLUDE
    warmupIterations = 5
    warmup = "1s"
    iterations = 10
    timeOnIteration = "2s"
    verbosity = "EXTRA"
    benchmarkMode.addAll("AverageTime")
    failOnError = false
    resultFormat = "CSV"
    jvmArgs.addAll("-Xmx128g")
    val buildDir = project.layout.buildDirectory.get().toString()
    humanOutputFile = project.file("${buildDir}/reports/jmh/human.txt") // human-readable output file
    resultsFile = project.file("${buildDir}/reports/jmh/results.txt") // results file
    profilers = listOf("gc")


    //configure files
    val dataset = "dataset"
    if (!hasProperty(dataset)) {
        println("BENCHMARKS FAILED! Set dataset folder by property '$dataset'")
    } else {
        val params = objects.listProperty<String>().value(getArgs(property(dataset).toString()))
        benchmarkParameters.put("fileName", params)
    }

    //filter on tools
    val tools = "toolName"
    if (hasProperty(tools)) {
        println("Run benchmarks for: .*${property(tools)}.*")
        includes = listOf(".*${property(tools)}.*")
    }

}


allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

kotlin { jvmToolchain(11) }