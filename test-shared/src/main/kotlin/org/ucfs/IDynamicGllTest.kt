package org.ucfs

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory
import java.io.File


interface IDynamicGllTest {
    val mainFileName: String

    companion object {
        const val GRAMMARS_FOLDER = "grammars"
        const val ONE_LINE_INPUTS = "oneLineInputs.txt"
        const val ONE_LINE_ERRORS_INPUTS = "oneLineErrorInputs.txt"
        const val GRAMMAR_FOLDER = "src/test/resources/grammars"
        const val INPUTS = "correctInputs"
        const val INCORRECT_INPUTS = "incorrectInputs"
        fun getTestName(input: String): String {
            return when (input.length) {
                0 -> "empty"
                in 1..10 -> input
                else -> "${input.take(10)}..."
            }
        }

        fun getFile(name: String, grammarFile: File): File? {
            return grammarFile.listFiles()?.firstOrNull { it.name == name }
        }

        fun getLines(fileName: String, folder: File): List<String> {
            val file = getFile(fileName, folder) ?: return listOf()
            return file.readLines()
        }

        fun getFiles(fileName: String, folder: File): Array<out File>? {
            val file = getFile(fileName, folder) ?: return arrayOf()
            return file.listFiles()
        }

        fun readFile(file: File): String {
            return file.inputStream().reader().readText()
        }
    }

    @TestFactory
    fun testAll(): Collection<DynamicContainer> {
        val folders =
            File(GRAMMAR_FOLDER).listFiles() ?: throw Exception("Resource folder $GRAMMAR_FOLDER not found")
        return folders
            .filter {
                it.isDirectory && it.listFiles()?.any { file -> file.name == mainFileName } == true
            }
            .map { concreteGrammarFolder -> handleFolder(concreteGrammarFolder) }
    }


    fun handleFolder(concreteGrammarFolder: File): DynamicContainer {
        val grammarName = concreteGrammarFolder.name
        return DynamicContainer.dynamicContainer(
            grammarName, getTestCases(concreteGrammarFolder)
        )

    }

    fun getTestCases(concreteGrammarFolder: File): Iterable<DynamicNode>

}
