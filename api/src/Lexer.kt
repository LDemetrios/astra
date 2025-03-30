package org.ldemetrios.astra.api

import org.joni.Option
import org.joni.Regex

typealias Token = Int

class Lexer<E : Enum<E>>(
    val input: String,
    private val tokens: List<E>,
    matchers: List<(String) -> TokenConsumer>,
    private val tokensToSkip: Set<E>,
    private val EOF_TOKEN: E
) {
    private val matchers = matchers.map { it(input) }

    var token: E = EOF_TOKEN
        private set

    var tokenValue: String? = null
        private set

    var position: Int = 0
        private set

    val tokenStart get() = position - tokenValue!!.length

    fun advance() {
        outer@ do {
            if (position == input.length) {
                tokenValue = null
                token = EOF_TOKEN
                break
            }
            for ((index, matcher) in matchers.withIndex()) {
                val matched = matcher.consume(position)
                if (matched != -1) {
                    tokenValue = input.substring(position, position + matched)
                    position += matched
                    token = tokens[index]
                    continue@outer
                }
            }
            throw ParsingException("Unexpected symbol, no lexeme recognized", input, position, position + 1)
        } while (token in tokensToSkip)
    }

    fun error(message: String): Nothing =
        throw ParsingException(message, input, position - (tokenValue?.length ?: 1), position)
}

fun regexLex(pattern: String): (String) -> TokenConsumer {
    val regex = org.joni.Regex(pattern)
    return { input ->
        val matcher = regex.matcher(input.toByteArray())
        TokenConsumer { matcher.match(it, input.length, Option.NONE) }
    }
}

fun literalLex(literal: String): (String) -> TokenConsumer {
    return { input ->
        TokenConsumer { if (input.startsWith(literal, it)) literal.length else -1 }
    }
}

fun interface TokenConsumer {
    fun consume(from: Int): Int
}
