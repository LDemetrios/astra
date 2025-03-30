package org.ldemetrios.astra

import org.ldemetrios.astra.GrammarElement.Reference.*
import org.ldemetrios.astra.GrammarElement.Reference
import org.ldemetrios.astra.api.Token

const val EPSILON = "ε"
const val EOF = "EOF"

data class Rule(
    val name: String,
    var variants: List<Variant>,
    var returns: String? = null,
    var args: List<Pair<String, String>>? = null
) {
    override fun toString(): String = string {
        +"Rule ("
        scope {
            +name
            +"variants : ["
            scope {
                variants.forEach { it.writeTo(this) }
            }
            +"]"
            +"returns ${returns ?: "nothing"}"
            if (args == null) +"args : null"
            else {
                +"args : ["
                scope {
                    args!!.forEach {
                        +"${it.first} = ${it.second}"
                    }
                }
                +"]"
            }
        }
        +")"
    }
}

fun Variant(native: List<GrammarElement>): Variant {
    val regular = native.filterIsInstance<Reference>()
    return if (regular.isEmpty()) {
        Variant(
            listOf(Terminal(EPSILON)),
            listOf(Terminal(EPSILON)) + native,
        )
    } else Variant(regular, native)
}

data class Variant(val references: List<Reference>, val elements: List<GrammarElement>) : List<Reference> by references {
    fun writeTo(builder: StringBuilderDSL) = with(builder) {
        +"{"
        scope {
            +"prods : ["
            scope { references.forEach { it.writeTo(this) } }
            +"], native : ["
            scope { elements.forEach { it.writeTo(this) } }
            +"]"
        }
        +"}"
    }

    override fun toString(): String = string { writeTo(this) }

    fun isEpsilon() = references.size == 1 && references[0].let { it is Terminal && it.name == EPSILON }
}

sealed class GrammarElement {
    abstract fun writeTo(builder: StringBuilderDSL)
    override fun toString(): String = string { writeTo(this) }

    data class Code(val code: String) : GrammarElement() {
        override fun writeTo(builder: StringBuilderDSL): Unit = with(builder) { +"{ $code }" }
    }

    sealed class Reference: GrammarElement()  {
        abstract val name: String

        data class Terminal(override val name: String) : Reference() {
            override fun writeTo(builder: StringBuilderDSL): Unit = with(builder) { +name }
        }

        data class NonTerminal(override val name: String, val inAttrs: List<String>?) : Reference() {
            override fun writeTo(builder: StringBuilderDSL): Unit = with(builder) {
                +(name + (inAttrs?.joinToString(", ", "(", ")") ?: ""))
            }
        }
    }
}

data class TokenTypeInfo(
    val isRegex: Boolean, val name: String, val repr: String,
)

data class InsertionsParser(
    val top : MutableList<String> = mutableListOf(),
    val typeargs : MutableList<String> = mutableListOf(),
    val params : MutableList<String> = mutableListOf(),
    val members : MutableList<String> = mutableListOf(),
    val alongside : MutableList<String> = mutableListOf(),
)

class GrammarData(
    val tokenTable: Map<String, Token>,
    val tokens: List<TokenTypeInfo>,
    val tokensToSkip: Set<Token>,
    val rules: Map<String, Rule>,
    val anyEpsilonVariants: Boolean,
    val insertionsLexer: List<String>,
    val insertionsParser: InsertionsParser,
    val attributes: MutableMap<String, MutableList<Attribute>>,
    val grammarName: String?
) {
    val terms: Set<String> = if (anyEpsilonVariants) tokenTable.keys + EPSILON else tokenTable.keys

    val nonTerms: Set<String> = rules.keys
}

enum class Attribute {
    PUBLIC, // aaaand that's it? Dunno, maybe I'll come up with something
    ;

    companion object {
        fun of(s: String) = when (val it = s.lowercase()) {
            "public" -> PUBLIC
            else -> throw IllegalArgumentException("Incorrect attribute \"$it\"")
        }
    }
}
