import org.ucfs.Java8
import org.ucfs.parser.Gll

fun main(){
    // get RSM start state
    val startState = Java8().rsm
    // get linear graph from source code
    val text = "package a; class X {}"
    val tokens = getTokenStream(text)
    // parse (intersect graph and rsm)
    val gll = Gll.gll(startState, tokens)
    // make sure that sppf isn't empty
    assert(gll.parse().first != null) { "can't build sppf" }
}