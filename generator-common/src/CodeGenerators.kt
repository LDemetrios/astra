package org.ldemetrios.astra

import org.ldemetrios.astra.GrammarElement.Reference.*
import java.util.*

open class StrictMap<K, out V>(open val map: Map<K, V>) : Map<K, V> by map {
    override fun get(key: K): V = map[key] ?: throw AssertionError("Key `$key` is not present in map")
}

fun GrammarData.generateLexer(fileName: String): String {
    fun String.escape() = replace("\\", "\\\\").replace("\"", "\\\"")
    val gn = grammarName
        ?: fileName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    return string {
        +insertionsLexer.joinToString(System.lineSeparator().repeat(2))

        +"""
        import org.ldemetrios.astra.api.Lexer
        import org.ldemetrios.astra.api.TokenConsumer
        import org.ldemetrios.astra.api.literalLex
        import org.ldemetrios.astra.api.regexLex
                
        private val tokensToSkip = setOf(${tokensToSkip.joinToString { "`${gn}Token`." + tokens[it].name }})
        
        enum class `${gn}Token` {
        """
        scope {
            +tokenTable.map { it.key }.joinToString { "`$it`" }
        }
        +"""
        }
        
        private val matchers : List<(String) -> TokenConsumer> = listOf(
        """
        scope {
            for ((index, value) in tokens.withIndex()) {
                val (isRegex, name, repr) = value
                if (isRegex) {
                    +"regexLex(\"${repr.escape()}\"), // $index"
                } else {
                    +"literalLex(\"${repr.escape()}\"), // $index"
                }
            }
        }
        +"""
        )
        
        fun `${gn}Lexer`(input: String) = Lexer(input, `${gn}Token`.entries, matchers, tokensToSkip, `${gn}Token`.EOF)
        """
    }
}

fun GrammarData.generateParser(fileName: String): String {
    val first: StrictMap<String, Set<String>> = run {
        val first: StrictMap<String, MutableSet<String>> = StrictMap(
            terms.associateWith { mutableSetOf(it) } +
                    rules.entries.associate { it.key to mutableSetOf() }
        )

        for ((name, rule) in rules) {
            if (rule.variants.any(Variant::isEpsilon)) first[name].add(EPSILON)
        }

        var changed = true
        while (changed) {
            changed = false
            for (rule in rules.values) for (variant in rule.variants) for ((i, element) in variant.withIndex()) {
                if (EPSILON in first[element.name]) {
                    changed = first[rule.name].addAll(first[element.name]) || changed
                    if (i == variant.size - 1) changed = first[rule.name].add(EPSILON) || changed
                } else {
                    changed = first[rule.name].addAll(first[element.name]) || changed
                    break
                }
            }
        }
        first
    }

    val follow: StrictMap<String, Set<String>> = run {
        val follow: StrictMap<String, MutableSet<String>> = StrictMap(nonTerms.associateWith { mutableSetOf() })

        for (rule in rules.keys) if (attributes[rule]?.contains(Attribute.PUBLIC) == true) {
            follow[rule].add(EOF)
        }

        var changed = true
        while (changed) {
            changed = false
            for (rule in rules.values) for (variant in rule.variants) {
                // For A -> aBb, add (FIRST(b) \ epsilon) to FOLLOW(B)
                for (i in (0..variant.size - 2)) if (variant[i] is NonTerminal) {
                    changed = changed || follow[variant[i].name].addAll(
                        first[variant[i + 1].name].filter { it != EPSILON }
                    )
                }

                // For A -> aB, add FOLLOW(A) to FOLLOW(B)
                if (variant.last() is NonTerminal) {
                    changed = changed || follow[variant.last().name].addAll(follow[rule.name])
                }

                // For A -> aBb, epsilon in FIRST(b), add FOLLOW(A) to FOLLOW(B)
                for (i in variant.indices.reversed()) {
                    if (i > 0 && EPSILON in first[variant[i].name]) {
                        val toAdd = follow[variant[i].name]
                        if (variant[i - 1].name in follow) changed =
                            follow[variant[i - 1].name].addAll(toAdd) || changed
                    }
                }
            }
        }

        follow
    }

    return string {
        val gn = grammarName ?: fileName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        +insertionsParser.top.joinToString(System.lineSeparator().repeat(2))

        +"""
            
        import org.ldemetrios.astra.api.ParsingException
        import org.ldemetrios.astra.api.Lexer
        import org.ldemetrios.astra.api.Token

        """

        +insertionsParser.alongside.joinToString(System.lineSeparator().repeat(2))

        val typeArgs = insertionsParser.typeargs.dropIfEmpty()?.joinToString(", ", "<", ">") ?: ""
        val params = insertionsParser.params.joinToString(", ")

        +"""
        @Suppress("UNUSED_VARIABLE", "RemoveRedundantBackticks")
        class `${gn}Parser`$typeArgs(private val lexer: Lexer<`${gn}Token`>, $params) {
        """

        scope {
            +insertionsParser.members.joinToString(System.lineSeparator().repeat(2))

            nl

            +"""
                init {
                    lexer.advance()
                }

                private fun skip(token: `${gn}Token`): String {
                    if (lexer.token != token) lexer.error("Expected token ${'$'}token, found ${'$'}{lexer.token}")
                    val res = lexer.tokenValue ?: throw IllegalArgumentException("Cannot skip EOF token")
                    lexer.advance()
                    return res
                }

                private fun unexpected(rule : String, vararg expected : `${gn}Token`) : Nothing {
                    lexer.error("Unexpected token `${'$'}{lexer.token}` at the start of rule `${'$'}rule`, only " + expected.joinToString() + " expected")
                }
            """

            nl

            for ((name, rule) in rules) {
                val modifier = if (attributes[name]?.contains(Attribute.PUBLIC) == true) "public" else "private"
                +"$modifier fun `$name`(${rule.args?.joinToString { (n, t) -> "$n: $t" } ?: ""}) : ${rule.returns ?: "Unit"} = when(lexer.token) {"
                scope {
                    val switcher = rule.let {
                        val result = it.variants.associateWith { variant ->
                            if (variant.isEpsilon()) follow[it.name].toList()
                            else {
                                val res = mutableListOf<String>()
                                for ((ind, element) in variant.withIndex()) {
                                    if (EPSILON in first[element.name]) {
                                        res.addAll(first[element.name])
                                        if (ind == variant.size - 1) res.addAll(follow[element.name])
                                    } else {
                                        res.addAll(first[element.name])
                                        break
                                    }
                                }
                                res.toSet()
                            }
                        }
                        result.values.flatten().run {
                            if (size != toSet().size) throw IllegalArgumentException("Provided grammar isn't LL(1), it has conflicting variants for rule ${it.name}:\n" + result.entries.joinToString { "FOLLOW = ${it.value} leads to ${it.key.references.joinToString { it.name }}" })
                        }
                        result.mapValues { (k, v) -> v - EPSILON }
                    }
                    for ((variant, tokens) in switcher) {
                        // Tokens
                        +"${tokens.joinToString { "`${gn}Token`.`$it`" }} -> {"
                        scope {
                            // Declarations
                            val lst = variant.groupBy { it.name }.filter { it.value.size > 1 }.keys
                            lst.forEach { e ->
                                when (e) {
                                    in terms -> +"val `$e` : MutableList<String> = mutableListOf()"
                                    in nonTerms -> {
                                        val returnType = rules[e]!!.returns
                                        if (returnType != null)
                                            +"val `$e` : MutableList<$returnType> = mutableListOf()"
                                    }
                                }
                            }

                            // Assignments
                            for (elem in variant.elements) when (elem) {
                                is Terminal -> when (elem.name) {
                                    EPSILON -> continue
                                    in lst -> +"`${elem.name}`.add(skip(`${gn}Token`.`${elem.name}`))"
                                    else -> +"val `${elem.name}` = skip(`${gn}Token`.`${elem.name}`)"
                                }
                                is NonTerminal -> {
                                    val callAttrs = elem.inAttrs?.joinToString() ?: ""
                                    if (elem.name in lst) +"${elem.name}.add(${elem.name}($callAttrs))"
                                    else +"val ${elem.name} = ${elem.name}($callAttrs)"
                                }
                                is GrammarElement.Code -> +elem.code
                            }
                        }
                        if (rule.returns == null) +"Unit"
                        +"}"
                    }

                    +"else -> unexpected(\"$name\", ${switcher.values.flatten().joinToString { "`${gn}Token`.`$it`" }})"
                }
                +"}"
                nl
            }
        }
        +"}"
    }
}
