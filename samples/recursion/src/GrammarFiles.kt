package org.ldemetrios.astra.samples.recursion

sealed interface FileElement

data class Directive(val command: String, val directiveArgs: List<String>, val directiveBody: DirectiveBody) : FileElement

data class DirectiveBody(val identList: List<String>?, val code: String?)

data class Argument(val ident: String?, val code: String?)

sealed interface RuleCtx : FileElement

data class LexRule(val name: String, val regex: String?, val literal: String?) : RuleCtx {
    fun Ident() = name
    fun Regex() = regex
    fun Literal() = literal
}

data class ParseRule(
    val name: String,
    val inAttrs: List<Param>,
    val outAttr: String?,
    val variants: List<VariantCtx>
) : RuleCtx {
    fun Ident() = name
}

sealed interface Element

data class Regular(val ident: String, val args: List<Argument>?) : Element
data class CodeInsert(val code: String) : Element
data class SurrogateRegex(val regex: String) : Element
data class SurrogateLiteral(val literal: String) : Element

data class Param(val ident: String, val type: String)

data class VariantCtx(val elements: List<Element>)