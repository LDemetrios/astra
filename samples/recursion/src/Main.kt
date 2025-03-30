package org.ldemetrios.astra.samples.recursion

import java.io.File
import org.ldemetrios.astra.*

fun main(vararg args: String) {
//    val args = listOf(
//            "/home/ldemetrios/Workspace/ITMO/MT/astra/samples/functions/src/main/astra/functions.g",
//            "/home/ldemetrios/Workspace/ITMO/MT/astra/samples/functions/build/generated-src/astra/main/"
//    )
    val input = args[0]
    val output = args[1]
    val grammarName = input.split("/").last().removeSuffix(".g")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    val data = GrammarData(
        GrammarParser(GrammarLexer(File(input).readText())).file()
    )

    for ((gen, postfix) in listOf(
        GrammarData::generateLexer to "Lexer",
        GrammarData::generateParser to "Parser"
    )) {
        File("$output/$grammarName$postfix.kt").run {
            parentFile.mkdirs()
            writeText(gen(data, grammarName))
        }
    }
}
