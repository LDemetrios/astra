!insert(top) {
    package org.ldemetrios.astra.samples.functions
}

!skip WS;

!setattr(public) header;

header : Header := fun maybeParams maybeReceiver ident `(` arguments `)` maybeReturns {
    Header(maybeParams, maybeReceiver, ident, arguments, maybeReturns)
};

maybeParams : List<Param>
    := `<` param moreParams({ mutableListOf(param) }) `>` { moreParams }
     | { listOf() }
     ;

param : Param := ident maybeSuper { Param(ident, maybeSuper) } ;

moreParams [ acc : MutableList<Param> ] : List<Param>
    := `,` param { acc.add(param) } moreParams(acc) { acc }
     | { acc }
     ;

maybeSuper : Type?
    := `:` type { type }
     | { null }
     ;

maybeReceiver : Type?
    := `(` type `)` `.` { type }
     | { null }
     ;

arguments : List<Argument>
    := argument moreArguments({ mutableListOf(argument) }) { moreArguments }
     | { listOf() }
     ;

moreArguments [acc : MutableList<Argument>] : List<Argument>
    := `,` argument { acc.add(argument) } moreArguments(acc) { acc }
     | { acc }
     ;

argument : Argument := ident `:` type { Argument(ident, type) } ;

maybeReturns : Type?
    := `:` type { type }
     | { null }
     ;


type : Type
    := ident maybeParameters maybeNullable { Concrete(ident, maybeParameters).let { if (maybeNullable) Nullable(it) else it } }
     | `(` typeListOrEmpty `)` postTypeParMod(typeListOrEmpty) { postTypeParMod }
     | `*` { StarProjection }
     ;

typeListOrEmpty : List<Type>
    := typeList { typeList }
     | { listOf() }
     ;

maybeNullable : Boolean
    := `?` { true }
     | { false }
     ;

postTypeParMod [list : List<Type>] : Type
    := `?` { Nullable(list.single()) }
     | `->` type { Function(list, type) }
     | { list.single() }
     ;

maybeParameters : List<Type>
    := `<` typeList `>` { typeList }
     | { listOf() }
     ;

typeList : List<Type> := type addTypes({ mutableListOf(type) }) { addTypes } ;

addTypes [acc : MutableList<Type>] : List<Type>
    := `,` type { acc.add(type) } addTypes(acc) { acc }
     | { acc }
     ;

fun = /\bfun\b/;

ident = /[a-zA-Z_][a-zA-Z_0-9]*/;

WS = /\s+/;
