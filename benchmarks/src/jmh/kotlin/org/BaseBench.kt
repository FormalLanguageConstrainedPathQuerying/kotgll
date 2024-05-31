package org

import kotlinx.benchmark.*
import java.io.File
@State(Scope.Benchmark)
abstract class BaseBench {
    @Param("Throwables.java")
    var fileName: String = ""

    lateinit var fileContents: String

    @Setup
    open fun prepare() {
        fileContents = File(fileName).readText()
    }

}