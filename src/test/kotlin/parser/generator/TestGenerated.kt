package parser.generator

import org.junit.jupiter.api.Test
import org.srcgll.parser.generator.ParserGenerator
import parser.generator.handwrite.ManyAbX
import parser.generator.handwrite.SomeAbX
import java.nio.file.Path

class TestGenerated {
    @Test
    fun generateSomeAbX(){
        ParserGenerator.generate(SomeAbX::class.java,
            Path.of("src", "test", "kotlin"), "parser.generator.generated")
    }

    @Test
    fun generateManyAbX(){
        ParserGenerator.generate(
            ManyAbX::class.java,
            Path.of("src", "test", "kotlin"), "parser.generator.generated")
    }
}