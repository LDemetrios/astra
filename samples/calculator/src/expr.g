!insert(top) {
    package org.ldemetrios.astra.samples.expr
};

!skip WS;

!setattr(public) expr;

!insert(parser.top) {
    import kotlin.math.sin
    import kotlin.math.cos
}

!insert(parser.alongside) {
    typealias T = Double
    fun String.toT() = toDouble()
}

expr : T := term addTerms(term) { addTerms } ;

addTerms [acc: T] : T
    := `+` term { val next = acc + term } addTerms(next) { addTerms }
     | `-` term { val next = acc - term } addTerms(next) { addTerms }
     | { acc } ;

term : T := atomic addAtomics(atomic) { addAtomics } ;

addAtomics [acc: T] : T
    := `*` atomic addAtomics({ acc * atomic }) { addAtomics }
     | `/` atomic addAtomics({ acc / atomic }) { addAtomics }
     | { acc } ;

atomic : T
    := `(` expr `)` { expr }
     | `-` atomic { -atomic }
     | ident `(` expr `)` { java.lang.Math::class.java.getMethod(ident, Double::class.java).invoke(null, expr) as Double }
     | num { num.toT() }
     ;

ident = /[a-zA-Z_][a-zA-Z_0-9]*/;

num = /
    (
        [0-9] ([0-9_]* [0-9])? \. ([0-9] ([0-9_]* [0-9])?)?
      |                        \.  [0-9] ([0-9_]* [0-9])?
      | [0-9] ([0-9_]* [0-9])?
    )
    ([eE] [+-]? [0-9] ([0-9_]* [0-9])?)?
/;

WS = /\s+/;
