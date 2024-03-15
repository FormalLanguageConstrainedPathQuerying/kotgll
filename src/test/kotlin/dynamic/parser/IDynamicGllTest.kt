package dynamic.parser

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.TestFactory
import java.io.File


interface IDynamicGllTest {
    val oneLineTestsFileName: String
        get() = "oneLineCorrectInputs.txt"
    val oneLineErrorsTestsFileName: String
        get() = "oneLineErrorInputs.txt"
    val grammarFolderName: String
        get() = "src/test/resources/grammars"
    val unsupportedTests: List<String>
        get() = listOf("abStar")

    @TestFactory
    fun testAll(): Collection<DynamicContainer> {
        val folders =
            File(grammarFolderName).listFiles() ?: throw Exception("Resource folder $grammarFolderName not found")
        return folders
            .filter { !unsupportedTests.contains(it.name) }
            .map { concreteGrammarFolder -> handleFolder(concreteGrammarFolder) }
    }

    fun getFile(name: String, grammarFile: File): File {
        val file = grammarFile.listFiles()?.firstOrNull { it.name == name }
        if (file == null) {
            throw Exception("$name file missed in ${grammarFile.name}")
        }
        return file
    }

    fun getTestName(input: String): String {
        return when (input.length) {
            0 -> "empty"
            in 1..10 -> input
            else -> "${input.take(10)}..."
        }
    }

    fun handleFolder(concreteGrammarFolder: File): DynamicContainer
}
