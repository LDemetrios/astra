package org.ldemetrios.astra.samples.functions

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.ldemetrios.astra.api.ParsingException

class Test : FreeSpec({
    fun reparse(string: String): String = FunctionsParser(FunctionsLexer(string)).header().toString()
    fun check(string: String) = reparse(string) shouldBe string

    "Basic test" {
        reparse("fun fact(x: Int) : Int") shouldBe "fun fact(x : Int) : Int"
    }

    "Receiver" {
        reparse("fun (Int).plus(other: String) : String")

        shouldThrow<ParsingException> {
            // This is very poorly translates into LL(1)
            reparse("fun Int.plus(other: String) : String")
        }
    }

    "Generics" {
        check("fun join(list : List<String>, sep : String) : String")
    }

    "Parameters" {
        check("fun <T : Comparable<T>> (List<T>).sort() : List<T>")
    }

    "Functions" {
        check("fun <A, B, C> ((A) -> B).then(other : (B) -> C) : (A) -> C")
    }

    "Nullable" {
        reparse("fun <T> (T?).orElse(value : T) : T") shouldBe "fun <T> ((T)?).orElse(value : T) : T"
    }
})