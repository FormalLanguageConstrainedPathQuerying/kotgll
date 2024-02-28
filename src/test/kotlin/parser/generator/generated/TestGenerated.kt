package parser.generator.generated

import org.junit.jupiter.api.Test
import org.srcgll.parser.generator.ParserGenerator
import parser.generator.handwrite.SomeAbHandWriteParser

class TestGenerated {
    @Test
    fun generateSomeAbX(){
        ParserGenerator.generate(SomeAbHandWriteParser::class.java, "src/test/kotlin/parser/generator/generated")
    }
}