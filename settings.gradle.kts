plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "ucfs"
include("solver")
include("benchmarks")
include("generator")
include("test-shared")
include("examples")
