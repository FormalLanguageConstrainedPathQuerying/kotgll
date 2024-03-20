package dynamic.parser.generator.compilation

import com.tschuchort.compiletesting.SourceFile
import dynamic.parser.IDynamicGllTest
import dynamic.parser.generator.GllGeneratedTest
import java.io.File

class GrammarInfo private constructor(val folderFile: File, val name: String){

    val source: SourceFile
    lateinit var clazz: Class<*>
    val file = folderFile.toPath().resolve("${GllGeneratedTest.DSL_FILE_NAME}.kt").toFile()
    val fqn: String = "${IDynamicGllTest.GRAMMAR_FOLDER}.$name.${GllGeneratedTest.DSL_FILE_NAME}"

    init{
        assert(file.exists()) { "Grammar file $file does not exist" }
        source = RuntimeCompiler.getSource(file)

    }

    companion object{
        fun getScanerlessGrammarInfo(folderFile: File, name: String): GrammarInfo{
            val grammarInfo = GrammarInfo(folderFile, name)
            val grammarClassLoader = RuntimeCompiler.compileClasses(listOf(grammarInfo.source))
            grammarInfo.clazz = grammarClassLoader.loadClass(grammarInfo.fqn)
            return grammarInfo
        }

        fun getGrammarInfo(folderFile: File, name: String): GrammarInfo{
            val grammarInfo = GrammarInfo(folderFile, name)
            return grammarInfo
        }
    }
}