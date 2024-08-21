package java8
import org.ucfs.parser.ParserGenerator
import org.ucfs.parser.RecoveryParserGenerator
import java.nio.file.Path
class Generator
fun main(args: Array<String>){
//    if(args.size != 1){
//        throw IllegalArgumentException("Set first argument as path to generation")
//    }
//    val path = Path.of(args[0])
    val path = Path.of("benchmarks/src/main/kotlin")
    println("Generate ${Java8::class} UCFS parsers at ${path.toAbsolutePath()}")
    generateJavaParser(path)
    generateJavaRecoveryParser(path)
}

fun generateJavaParser(path: Path) {
    ParserGenerator(
        Java8::class.java,
        JavaToken::class.java
    ).generate(path, "org.ucfs")
}
fun generateJavaRecoveryParser(path: Path) {
    RecoveryParserGenerator(
        Java8::class.java,
        JavaToken::class.java
    ).generate(path, "org.ucfs")
}

