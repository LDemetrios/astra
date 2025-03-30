package org.ldemetrios.astra.samples.recursion

import org.ldemetrios.astra.api.Token
import org.ldemetrios.astra.GrammarElement.Reference.NonTerminal
import org.ldemetrios.astra.GrammarElement.Reference.Terminal
import org.ldemetrios.astra.*

fun Directive.arguments(vararg arity: Int): List<String> {
    val res = this.directiveArgs
    if (arity.isNotEmpty() && res.size !in arity) throw IllegalArgumentException(
        "Wrong arity ${res.size} for directive `${command}`, ${arity.joinToString(", ", "[", "]")} expected"
    )
    return res
}

fun Directive.bodyAsCode() = this.directiveBody.code
    ?: throw IllegalArgumentException("Expected code as a body for `${command}` directive")

fun Directive.bodyAsIdents(): List<String> = this.directiveBody.identList
    ?.ifEmpty { null }
    ?: throw IllegalArgumentException("Expected list of identifiers as body of `${command}` directive")

internal fun GrammarData(file: List<FileElement>): GrammarData {
//    println(file.format().pretty(400))
    val tokensIndexed = file
        .filterIsInstance<LexRule>()
        .withIndex()

    val tokenList = tokensIndexed.map { it.value.Ident() }

    val tokenTable = tokensIndexed.associate { it.value.Ident() to it.index }.toMutableMap()

    val patterns = tokensIndexed.mapNotNull {
        it.value.Regex()?.run { regex(this) to it.index }
    }.toMap().toMutableMap()

    val literals = tokensIndexed.mapNotNull {
        it.value.Literal()?.run { literal(this) to it.index }
    }.toMap().toMutableMap()

    val tokensWithTypes = tokensIndexed.map {
        it.value.Regex()?.run {
            TokenTypeInfo(true, it.value.Ident(), regex(this))
        } ?: it.value.Literal()!!.run {
            TokenTypeInfo(false, it.value.Ident(), literal(this))
        }
    }.toMutableList()

    val insertionsLexer: MutableList<String> = mutableListOf()
    val insertionsParser = InsertionsParser()

    val tokensToSkip = mutableSetOf<Token>()

    val attributes = mutableMapOf<String, MutableList<Attribute>>()

    var grammarName: String? = null

    for (directive in file.filterIsInstance<Directive>()) when (directive.command) {
        "insert" -> {
            val place = directive.arguments(1)[0]
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
                    tokenTable[it] ?: throw IllegalArgumentException("${it} token is not present in grammar")
                }
            )
        }
        "setattr" -> {
            val attrs = directive.arguments().map(Attribute::of)
            for (applicant in directive.bodyAsIdents()) {
                attributes.computeIfAbsent(applicant) { mutableListOf() }.addAll(attrs)
            }
        }
        "grammar" -> {
            grammarName = directive.arguments(0, 1).getOrNull(0)
                ?: directive.directiveBody.identList?.getOrNull(0)
                        ?: directive.directiveBody.code?.stripCode()
                        ?: throw IllegalArgumentException("Grammar name should be specified in `grammar` directive")
        }
    }

    var nextSurrogateToken = tokensIndexed.maxOf { it.index } + 1

    val rules: Map<String, Rule> = file
        .filterIsInstance<ParseRule>()
        .map {
            Rule(
                name = it.Ident(),
                variants = it.variants.map { vari ->
                    Variant(
                        vari.elements.map { el ->
                            when (el) {
                                is Regular -> if (el.ident in tokenList) Terminal(
                                    el.ident
                                ) else NonTerminal(el.ident, el.args?.map {
                                    it.ident ?: it.code!!.stripCode()
                                })
                                is CodeInsert -> {
                                    GrammarElement.Code(el.code.stripCode())
                                }
                                is SurrogateRegex -> {
                                    val regex = regex(el.regex)
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
                                is SurrogateLiteral -> {
                                    val literal = literal(el.literal)
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
                            }
                        }
                    )
                },
                returns = it.outAttr,
                args = it.inAttrs.map { it.ident to it.type }
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

