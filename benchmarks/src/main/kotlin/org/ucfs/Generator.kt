package org.ucfs
import org.ucfs.ast.NodeClassesGenerator
import org.ucfs.parser.ParserGenerator
import org.ucfs.parser.RecoveryParserGenerator
import java.nio.file.Path

val grammarClass = Java8::class.java
val tokenClass = JavaToken::class.java
val pkg = "org.ucfs"

fun main(args: Array<String>){
    val path: Path = if(args.isEmpty()){
        Path.of("benchmarks/src/main/kotlin")
    }
    else{
        Path.of(args[0])
    }
    println("Generate ${Java8::class} UCFS parsers at ${path.toAbsolutePath()}")
    generateJavaParser(path)
    generateJavaRecoveryParser(path)
    generateNodes(path)
}

fun generateJavaParser(path: Path) {
    ParserGenerator(grammarClass, tokenClass).generate(path, pkg)
}
fun generateJavaRecoveryParser(path: Path) {
    RecoveryParserGenerator(grammarClass, tokenClass).generate(path, pkg)
}

fun generateNodes(path: Path){
    var nodeGenerator = NodeClassesGenerator(grammarClass)
    nodeGenerator.generate(path, "$pkg.nodes")
}

