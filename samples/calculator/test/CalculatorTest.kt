package org.ldemetrios.astra.samples.calculator

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.ldemetrios.astra.api.ParsingException
import org.ldemetrios.astra.samples.expr.ExprLexer
import org.ldemetrios.astra.samples.expr.ExprParser
import kotlin.math.sin

class ParserTest : FreeSpec({
    fun eval(text: String): Double {
        val parser = ExprParser(ExprLexer(text))
        return parser.expr()
    }

    "Priorities" {
        eval("1 + 2 * 3 - 7 * 11") shouldBe (1 + 2 * 3 - 7 * 11).toDouble()
    }

    "Left associativity" {
        eval("1 - 2 - 3") shouldBe -4.0
    }

    "Unary minuses recognition" {
        //@formatter:off
        eval("1 - - - - - - - 3") shouldBe (1 - - - - - - -3).toDouble()
        //@formatter:on
    }

    "Grouping" {
        eval("(1 + 2) * (3 + 4)") shouldBe 21.0
    }

    "Functions" {
        eval("sin(10)") shouldBe sin(10.0)
    }

    "Example" {
        eval("(1+2)*-sin(-3*(7-4)+2)") shouldBe ((1 + 2) * -sin(-3 * (7 - 4) + 2.0))
    }

    "Recursion" {
        eval("(((((((2)))))))") shouldBe 2.0
    }

    "Double support" {
        eval("1.5 / .5") shouldBe 3.0
    }

    fun erroneous(text: String, ex: List<Class<out Exception>> = listOf(ParsingException::class.java)) {
        val actualText = text.replace("$", "")
        try {
            eval(actualText)
            throw java.lang.AssertionError("Successfully parsed \"$actualText\" (but shouldn't've)")
        } catch (e: Exception) {
            if (ex.any { it.isInstance(e) }) {
                println(e.message)
            } else {
                throw java.lang.AssertionError("Parsing \"$actualText\" threw ${e.javaClass}(${e.message}), which is not allowed")
            }
        }
    }

    "Failures" - {
        "Multiple signs" {
            erroneous("1+$+2")
            erroneous("1*-$+2")
        }
        "Function without parenthesis" {
            erroneous("sin $2")
        }
        "Missing parenthesis" {
            erroneous("(2 + 3$")
            erroneous("(((((2))))$")
        }
        "Missing operator" {
            erroneous("2 $3")
        }
        "Missing operand" {
            erroneous("$* 3")
            erroneous("3 * $")
        }
        "Unsupported function" {
            erroneous("\$hello(3)", listOf(NoSuchMethodException::class.java))
        }
        "Unexpected tokens" {
            erroneous("1 + 2$(")
            erroneous("1 +$@ 2")
        }
    }
})
