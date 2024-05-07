package org

import org.ucfs.Java8
import org.ucfs.JavaToken
import org.ucfs.parser.ParserGenerator
import java.nio.file.Path

fun main() {
    ParserGenerator(
        Java8::class.java,
        JavaToken::class.java
    ).generate(Path.of("benchmarks/src/main/kotlin/"), "org.ucfs")
}