package org.srcgll.generators

class GeneratorException(msg: String = ""): Exception("Generator exception$msg") {
    companion object{
        val grammarExpectedMsg = "Only subclass of Grammar class can be used for parser generation"
    }
}