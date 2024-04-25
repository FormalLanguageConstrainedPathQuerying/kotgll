package org.ucfs

class GeneratorException(msg: String = "") : Exception("Generator exception$msg") {
    companion object {
        const val GRAMMAR_EXPECTED = "Only subclass of Grammar class can be used for parser generation"
    }
}