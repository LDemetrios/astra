!insert(top) {
    package org.ldemetrios.astra.samples.recursion
}

!setattr(public) file;

!skip WS lineComment;

file : List<FileElement>
    := directive fileContent({ mutableListOf(directive) }) { fileContent }
    | rule fileContent({ mutableListOf(rule) }) { fileContent };

fileContent [ acc : MutableList<FileElement> ] : List<FileElement>
    := directive { acc.add(directive) } fileContent(acc) { acc }
     | rule { acc.add(rule) } fileContent(acc) { acc }
     | { acc }
     ;

directive : Directive := `!` ident maybeDirectiveArgs directiveBody {
    Directive(ident, maybeDirectiveArgs ?: listOf(), directiveBody)
};

maybeDirectiveArgs : List<String>
    := directiveArgs { directiveArgs }
     | { listOf() }
     ;

directiveArgs : List<String> := `(` identList `)` { identList };

identList : List<String> := ident moreIdents({ mutableListOf(ident) }) { moreIdents } ;

moreIdents [ acc : MutableList<String> ] : List<String>
    := maybeComma ident { acc.add(ident) } moreIdents(acc) { acc }
     | { acc }
     ;

maybeComma : Unit := `,` | {Unit} ;

directiveBody : DirectiveBody
    := identList `;` { DirectiveBody(identList, null) }
     | code maybeSemi { DirectiveBody(null, code) }
     ;

maybeSemi : Unit := `;` | {Unit} ;

rule : RuleCtx := ident ruleContent(ident) { ruleContent };

ruleContent [ name : String ] : RuleCtx
    := maybeInAttrs maybeOutAttr `:=` variant moreVariants({ mutableListOf(variant) }) `;` {
         ParseRule(name, maybeInAttrs, maybeOutAttr, moreVariants)
     }
     | `=` lexRuleContent(name) `;` { lexRuleContent }
     ;

maybeOutAttr : String?
    := `:` type { type }
     | { null }
     ;

moreVariants [ acc : MutableList<VariantCtx> ] : List<VariantCtx>
    := `|` variant { acc.add(variant) } moreVariants(acc) { acc }
     | { acc }
     ;

lexRuleContent [ name : String ] : RuleCtx
    := regex { LexRule(name, regex, null) }
     | literal { LexRule(name, null, literal) }
     ;

maybeInAttrs : List<Param>
    := `[` param moreParams({ mutableListOf(param) }) `]` { moreParams }
     | { listOf() }
     ;

moreParams [ acc : MutableList<Param> ] : List<Param>
    := `,` param { acc.add(param) } moreParams(acc) { acc }
     | { acc }
     ;

param : Param := ident `:` type { Param(ident, type) } ;

type : String
    := { val start = lexer.tokenStart } type1 { lexer.input.substring(start, lexer.tokenStart).trim() }
     ;

type1 : Unit
    := ident maybeParameters maybeNullable
     | `(` typeListOrEmpty `)` postTypeParMod
     | `*`
     ;

typeListOrEmpty : Unit := typeList | {Unit} ;
maybeNullable : Unit := `?` | {Unit} ;
postTypeParMod : Unit := `?` | `->` type1 | {Unit} ;
maybeParameters : Unit := `<` typeList `>` | {Unit} ;
typeList : Unit := type1 addTypes ;
addTypes : Unit := `,` type1 addTypes | {Unit} ;

variant : VariantCtx := elements({ mutableListOf() }) { VariantCtx(elements) };

elements [ acc : MutableList<Element> ] : List<Element>
    := element { acc.add(element) } elements(acc) { acc }
     | { acc }
     ;

element : Element
    := ident maybeArgs { Regular(ident, maybeArgs) }
     | code { CodeInsert(code) }
     | regex { SurrogateRegex(regex) }
     | literal { SurrogateLiteral(literal) }
     ;

maybeArgs : List<Argument>?
    := `(` arg moreArgs({ mutableListOf(arg) }) `)` { moreArgs }
     | { listOf() }
     ;

arg : Argument := ident { Argument(ident, null) } | code { Argument(null, code) } ;

moreArgs [ acc : MutableList<Argument> ] : List<Argument>
    := arg { acc.add(arg) } moreArgs(acc) { acc }
     | { acc }
     ;

bydef = `:=`;
colon = `:`;
equal = `=`;

lineComment = /\/\/[^\r\n]*/;

ident = /[A-Za-z_][_a-zA-Z0-9.]*/;

regex = /\/([^\/\\]|\\.)*\//;

literal = /`([^`\\]|\\`|\\\\|\\n|\\r|\\u[A-Fa-f0-9]{4})*`/;

code = /\{(?:[^{}]+|\g<0>)*\}/;

WS = /\s+/;

