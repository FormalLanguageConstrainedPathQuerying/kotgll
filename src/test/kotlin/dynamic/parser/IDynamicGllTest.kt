package dynamic.parser

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import java.io.File


interface IDynamicGllTest {
    val mainFileName: String

    companion object {
        const val GRAMMAR_FOLDER = "grammars"
    }

    val oneLineTestsFileName: String
        get() = "oneLineInputs.txt"
    val oneLineErrorsTestsFileName: String
        get() = "oneLineErrorInputs.txt"
    val grammarFolderName: String
        get() = "src/test/resources/grammars"

    @TestFactory
    fun testAll(): Collection<DynamicContainer> {
        val folders =
            File(grammarFolderName).listFiles() ?: throw Exception("Resource folder $grammarFolderName not found")
        return folders
            .filter {
                it.isDirectory && it.listFiles()?.any { file -> file.name == mainFileName } == true
            }
            .map { concreteGrammarFolder -> handleFolder(concreteGrammarFolder) }
    }

    fun getFile(name: String, grammarFile: File): File? {
        return grammarFile.listFiles()?.firstOrNull { it.name == name }
    }

    fun getTestName(input: String): String {
        return when (input.length) {
            0 -> "empty"
            in 1..10 -> input
            else -> "${input.take(10)}..."
        }
    }

    fun handleFolder(concreteGrammarFolder: File): DynamicContainer

    fun getLines(fileName: String, folder: File): List<String> {
        val file = getFile(fileName, folder) ?: return listOf()
        return file.readLines()
    }

}
