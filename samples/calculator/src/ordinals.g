!insert(top) {
    package org.ldemetrios.astra.samples.expr
};

!skip WS;

!setattr(public) expr;

!insert(parser.top) {
    import java.math.BigInteger
}

expr : Ordinal := term addTerms(term) { addTerms } ;

addTerms [acc: Ordinal] : Ordinal
    := `+` term { val next = acc + term } addTerms(next) { addTerms }
     | { acc } ;

term : Ordinal := power addPowers(power) { addPowers } ;

addPowers [acc: Ordinal] : Ordinal
    := `*` power addPowers({ acc * power }) { addPowers }
     | { acc } ;

power : Ordinal := atomic maybePower(atomic) { maybePower };

maybePower [left : Ordinal] : Ordinal
    := `^` power { left pow power }
     | { left }
     ;

atomic : Ordinal
    := `(` expr `)` { expr }
     | num { Ordinal.Finite(num.toBigInteger()) }
     | `w` { Ordinal.Omega }
     ;

num = /[0-9]+/;

WS = /\s+/;



!insert(parser.alongside) {


val Int.B get() = BigInteger.valueOf(this.toLong())

data class Term(val beta: Ordinal, val a: BigInteger) : Comparable<Term> {
    override fun toString(): String {
        return if (beta.isFinite()) {
            when (beta.natural) {
                0.B -> a.toString()
                1.B -> when (a) {
                    0.B -> "0"
                    1.B -> "w"
                    else -> "w * $a"
                }

                else -> when (a) {
                    0.B -> "0"
                    1.B -> "w ^ (${beta.natural})"
                    else -> "w ^ (${beta.natural}) * $a"
                }
            }
        } else {
            when (a) {
                0.B -> "0"
                1.B -> "w ^ ($beta)"
                else -> "w ^ ($beta) * $a"
            }
        }
    }

    override fun compareTo(other: Term): Int {
        val order = beta.compareTo(other.beta)
        if (order != 0) return order
        return a.compareTo(other.a)
    }

    fun isFinite() = beta == Ordinal.Zero
}

@Suppress("FunctionName")
fun FiniteTerm(n: BigInteger) = Term(Ordinal.Zero, n)

@Suppress("DataClassPrivateConstructor")
data class Ordinal private constructor(val terms: List<Term>) :  Comparable<Ordinal> {
    operator fun plus(other: Ordinal): Ordinal = of(terms + other.terms)

    operator fun times(other: Ordinal): Ordinal {
        val terms = (other.terms).flatMap { this * it }
        return of(terms)
    }

    private operator fun times(it: Term): List<Term> {
        if (this == Zero) return listOf()
        if (it.isFinite()) {
            val x = it.a
            if (x == 0.B) return listOf()
            val terms = (this.terms).toMutableList()
            terms[0] = Term(terms[0].beta, terms[0].a * x)
            return terms
        } else {
            val xi = it.beta
            return listOf(Term((this.terms)[0].beta + xi, it.a))
        }
    }

    infix fun pow(other: Ordinal): Ordinal = other.terms.map { pow(it) }.reduceOrNull(Ordinal::times) ?: One

    private fun pow(other: Term): Ordinal {
        return if (other.isFinite()) {
            if (isFinite()) {
                // (finite) ^ (finite)
                val power = other.a
                val base = this.natural
                when {
                    power == 0.B -> One
                    base == 0.B -> Zero
                    base == 1.B -> One
                    else -> Finite(base.pow(power.intValueExact()))
                }
            } else {
                // (something) ^ (finite)
                var power = other.a.intValueExact()
                var result = One
                var base = this
                while (power > 0) {
                    if (power % 2 == 1) {
                        result *= base
                    }
                    base *= base
                    power /= 2
                }
                result
            }
        } else {
            // w^x * a
            fun wTo(x: Ordinal, a: BigInteger = 1.B): Ordinal = of(Term(x, a))
            fun wTo(x: Term, a: BigInteger = 1.B): Ordinal = wTo(of(x), a)
            fun wTo(x: BigInteger, a: BigInteger = 1.B): Ordinal = wTo(Finite(x), a)

            if (isFinite()) {
                if (this.natural == 0.B) return Zero
                if (this.natural == 1.B) return One
                // (finite) ^ (limit)
                // m^w^n = w^w^(n-1), n -- finite
                // m^w^x = w^w^x, x -- limit
                if (other.beta.isFinite()) { // this is finite
                    // m (w^n * a) = w^(w^(n-1) * a)
                    val n = other.beta.natural
                    wTo(wTo(n - 1.B, other.a))
                } else {
                    // x = other.beta
                    /* w^w^x */ wTo(/* w^x */ other)
                }
            } else {
                // (something) ^ (limit)
                wTo(of(/*beta1*/ terms[0].beta * /*gamma*/ other))
            }
        }
    }

    override fun toString(): String = if (terms.isEmpty()) "0" else terms.joinToString(" + ")

    override operator fun compareTo(other: Ordinal): Int {
        val limit = minOf(terms.size, other.terms.size)
        for (i in 0 until limit) {
            val result = terms[i].compareTo(other.terms[i])
            if (result != 0) return result
        }
        return terms.size.compareTo(other.terms.size)
    }

//    override fun equals(other: Any?): Boolean {
//        return other is Ordinal && compareTo(other) == 0
//    }
    // With our representation works as is

    val natural: BigInteger
        get() = when {
            terms.isEmpty() -> 0.B
            terms.last().beta == Zero -> terms.last().a
            else -> 0.B
        }

    fun isFinite() = terms.isEmpty() || terms.size == 1 && terms.first().beta == Zero

    companion object {
        val Zero = Ordinal(listOf())
        val Omega = Ordinal(listOf(Term(Finite(1.B), 1.B)))
        @Suppress("FunctionName")
        fun Finite(x: BigInteger) = of(FiniteTerm(x))
        val One = Finite(1.B)

        fun of(vararg terms: Term) = of(terms.toList())

        fun of(terms: List<Term>): Ordinal {
            val reducedTerms = mutableListOf<Term>()
            for (term in terms) {
                while (reducedTerms.isNotEmpty() && reducedTerms.last().beta < term.beta) {
                    reducedTerms.removeLast()
                }
                if (reducedTerms.isNotEmpty() && reducedTerms.last().beta == term.beta) {
                    val was = reducedTerms.removeLast()
                    val new = Term(
                        term.beta,
                        term.a + was.a
                    )
                    reducedTerms.add(new)
                } else {
                    reducedTerms.add(term)
                }
            }
            return Ordinal(
                reducedTerms.filter { it.a != 0.B }
            )
        }
    }
}

}