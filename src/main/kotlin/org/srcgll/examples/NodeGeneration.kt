import org.srcgll.generators.ast.AstClassesGenerator
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.extension.StringExtension.times
import org.srcgll.grammar.combinator.regexp.Nt
import org.srcgll.grammar.combinator.regexp.or
import org.srcgll.rsm.symbol.Term
import java.nio.file.Path

class AbStar: Grammar() {
    var S by Nt()
    var A by Nt()
    var B by Nt()

    init {
        setStart(S)
        S = "a" * B * "c" or A * "c"
        A = "a" * "b"
        B = Term("b")
    }
}
fun main(){
    val generator = AstClassesGenerator(AbStar::class.java)
    generator.generate(Path.of("gen"), "a.b.star")

}