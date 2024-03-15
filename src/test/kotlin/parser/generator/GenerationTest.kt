package parser.generator

import org.junit.jupiter.api.Test
import org.srcgll.parser.generator.ParserGenerator
import parser.generator.handwrite.ManyAbX
import parser.generator.handwrite.SomeAbX
import java.nio.file.Path

class GenerationTest {
    @Test
    fun generateSomeAbX() {
        ParserGenerator(SomeAbX::class.java).generate(
            Path.of("src", "test", "kotlin"), "parser.generator.generated"
        )
    }

    @Test
    fun generateManyAbX() {
        ParserGenerator(ManyAbX::class.java).generate(
            Path.of("src", "test", "kotlin"), "parser.generator.generated"
        )
    }
}