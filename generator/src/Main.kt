package org.ldemetrios.astra

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.Trees
import org.ldemetrios.astra.GrammarLexer
import org.ldemetrios.astra.GrammarParser
import pretty.Doc
import pretty.encloseSep
import pretty.nest
import pretty.symbols.space
import pretty.text
import java.io.File

fun main(vararg args: String) {
//    val args = listOf(
//        "/home/ldemetrios/Workspace/ITMO/MT/astra/samples/functions/src/main/astra/functions.g",
//        "/home/ldemetrios/Workspace/ITMO/MT/astra/samples/functions/build/generated-src/astra/main/"
//    )

    val input = args[0]
    val output = args[1]
    val grammarName = input.split("/").last().removeSuffix(".g")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    val data = GrammarData(
        GrammarParser(CommonTokenStream(GrammarLexer(CharStreams.fromFileName(input)))).file()
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

private fun formatLispExpr(function: String, args: List<Doc<Nothing>>): Doc<Nothing> {
    val exprDoc = (
            listOf(function.text()) +
                    args.map { it.nest(4) }
            ).encloseSep("(".text(), ")".text(), space())
    return exprDoc
}

fun ParseTree.format(): Doc<Nothing> {
    if (this.childCount == 0) {
        return Trees
            .getNodeText(this, null as Parser?)!!
            .escape()
            .replace("\"", "\\\"")
            .let { "\"$it\"" }
            .text()
    }
    val name = this.javaClass.simpleName.removeSuffix("Context")
    return formatLispExpr(
        name,
        children.map { it.format() }.toList()
    )
}

val ParseTree.children get() = (0 until childCount).asSequence().map(this::getChild)
