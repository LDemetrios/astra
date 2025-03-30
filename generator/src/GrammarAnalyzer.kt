package org.ldemetrios.astra

import org.antlr.v4.runtime.tree.TerminalNode
import org.ldemetrios.astra.GrammarElement.Reference.NonTerminal
import org.ldemetrios.astra.GrammarElement.Reference.Terminal
import org.ldemetrios.astra.GrammarParser.CodeInsertContext
import org.ldemetrios.astra.GrammarParser.DirectiveContext
import org.ldemetrios.astra.GrammarParser.FileContext
import org.ldemetrios.astra.GrammarParser.LexRuleContext
import org.ldemetrios.astra.GrammarParser.ParseRuleContext
import org.ldemetrios.astra.GrammarParser.RegularContext
import org.ldemetrios.astra.GrammarParser.SurrogateLiteralContext
import org.ldemetrios.astra.GrammarParser.SurrogateRegexContext
import org.ldemetrios.astra.Variant
import org.ldemetrios.astra.api.Token

fun DirectiveContext.arguments(vararg arity: Int): List<TerminalNode> {
    val res = this.directiveArgs()?.Ident() ?: listOf()
    if (arity.isNotEmpty() && res.size !in arity) throw IllegalArgumentException(
        "Wrong arity ${res.size} for directive `${command}`, ${arity.joinToString(", ", "[", "]")} expected"
    )
    return res
}

fun DirectiveContext.bodyAsCode() = this.directiveBody().Code().text
    ?: throw IllegalArgumentException("Expected code as a body for `${command}` directive")

fun DirectiveContext.bodyAsIdents(): List<TerminalNode> = this.directiveBody().Ident()
    .ifEmpty { null }
    ?: throw IllegalArgumentException("Expected list of identifiers as body of `${command}` directive")


internal fun GrammarData(file: FileContext): GrammarData {
//    println(file.format().pretty(400))
    val tokensIndexed = file.rule_()
        .filterIsInstance<LexRuleContext>()
        .withIndex()

    val tokenList = tokensIndexed.map { it.value.Ident().text }

    val tokenTable = tokensIndexed.associate { it.value.Ident().text to it.index }.toMutableMap()

    val patterns = tokensIndexed.mapNotNull {
        it.value.Regex()?.run { regex(this.text) to it.index }
    }.toMap().toMutableMap()

    val literals = tokensIndexed.mapNotNull {
        it.value.Literal()?.run { literal(this.text) to it.index }
    }.toMap().toMutableMap()

    val tokensWithTypes = tokensIndexed.map {
        it.value.Regex()?.run {
            TokenTypeInfo(true, it.value.Ident().text, regex(this.text))
        } ?: it.value.Literal().run {
            TokenTypeInfo(false, it.value.Ident().text, literal(this.text))
        }
    }.toMutableList()

    val insertionsLexer: MutableList<String> = mutableListOf()
    val insertionsParser = InsertionsParser()

    val tokensToSkip = mutableSetOf<Token>()

    val attributes = mutableMapOf<String, MutableList<Attribute>>()

    var grammarName: String? = null

    for (directive in file.directive()) when (directive.command.text) {
        "insert" -> {
            val place = directive.arguments(1)[0].text
            val code = directive.bodyAsCode().stripCode()

            when(place) {
                "lexer.top" -> insertionsLexer.add(code)
                "parser.top" -> insertionsParser.top.add(code)
                "both.top", "top" -> {
                    insertionsLexer.add(code)
                    insertionsParser.top.add(code)
                }

                "parser.alongside" -> insertionsParser.alongside.add(code)
                "parser.member" -> insertionsParser.members.add(code)
                "parser.typearg" -> insertionsParser.typeargs.add(code)
                "parser.param" -> insertionsParser.params.add(code)
                else -> throw IllegalArgumentException("`insert` directive expects (lexer|parser)?.top, parser.(alongside|member|typearg|param) as its argument, not $place")
            }
        }
        "skip" -> {
            directive.arguments(0)
            tokensToSkip.addAll(
                directive.bodyAsIdents().map {
                    tokenTable[it.text] ?: throw IllegalArgumentException("${it.text} token is not present in grammar")
                }
            )
        }
        "setattr" -> {
            val attrs = directive.arguments().map(TerminalNode::getText).map(Attribute::of)
            for (applicant in directive.bodyAsIdents()) {
                attributes.computeIfAbsent(applicant.text) { mutableListOf() }.addAll(attrs)
            }
        }
        "grammar" -> {
            grammarName = directive.arguments(0, 1).getOrNull(0)?.text
                ?: directive.directiveBody()?.Ident()?.getOrNull(0)?.text
                        ?: directive.directiveBody()?.Code()?.text?.stripCode()
                        ?: throw IllegalArgumentException("Grammar name should be specified in `grammar` directive")
        }
    }

    var nextSurrogateToken = tokensIndexed.maxOf { it.index } + 1

    val rules: Map<String, Rule> = file.rule_()
        .filterIsInstance<ParseRuleContext>()
        .map {
            Rule(
                name = it.Ident().text,
                variants = it.variant().map { vari ->
                    Variant(
                        vari.element().map { el ->
                            when (el) {
                                is RegularContext -> if (el.Ident().text in tokenList) Terminal(
                                    el.Ident().text
                                ) else NonTerminal(el.Ident().text, el.args()?.arg()?.map {
                                    it.Ident()?.text ?: it.Code().text.stripCode()
                                })
                                is CodeInsertContext -> {
                                    GrammarElement.Code(el.Code().text.stripCode())
                                }
                                is SurrogateRegexContext -> {
                                    val regex = regex(el.Regex().text)
                                    val token = patterns[regex]
                                    if (token == null) {
                                        patterns[regex] = nextSurrogateToken
                                        tokenTable["__UNNAMED_TOKEN_$nextSurrogateToken"] = nextSurrogateToken
                                        tokensWithTypes.add(
                                            TokenTypeInfo(
                                                true, "__UNNAMED_TOKEN_$nextSurrogateToken", regex
                                            )
                                        )
                                        Terminal("__UNNAMED_TOKEN_${nextSurrogateToken++}")
                                    } else {
                                        Terminal(tokensWithTypes[token].name)
                                    }
                                }
                                is SurrogateLiteralContext -> {
                                    val literal = literal(el.Literal().text)
                                    val token = literals[literal]
                                    if (token == null) {
                                        literals[literal] = nextSurrogateToken
                                        tokenTable["__UNNAMED_TOKEN_$nextSurrogateToken"] = nextSurrogateToken
                                        tokensWithTypes.add(
                                            TokenTypeInfo(
                                                false, "__UNNAMED_TOKEN_$nextSurrogateToken", literal
                                            )
                                        )
                                        Terminal("__UNNAMED_TOKEN_${nextSurrogateToken++}")
                                    } else {
                                        Terminal(tokensWithTypes[token].name)
                                    }
                                }
                                else -> throw AssertionError("Unexpected element type ${el.javaClass}")
                            }
                        }
                    )
                },
                returns = it.outAttr?.text,
                args = it.inAttrs()?.param()?.map { it.Ident().text to it.type().text }
            )
        }.associateBy { it.name }

    val anyEpsilonVariants = rules
        .values
        .asSequence()
        .flatMap { it.variants }
        .any { it.isEpsilon() }

    tokenTable[EOF] = -1
    return GrammarData(
        tokenTable = tokenTable,
        tokens = tokensWithTypes,
        tokensToSkip = tokensToSkip,
        rules = rules,
        anyEpsilonVariants = anyEpsilonVariants,
        insertionsLexer = insertionsLexer,
        insertionsParser = insertionsParser,
        attributes = attributes,
        grammarName = grammarName,
    )
}

