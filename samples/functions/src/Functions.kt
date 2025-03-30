package org.ldemetrios.astra.samples.functions

data class Argument(val name: String, val type: Type) {
    override fun toString(): String = "$name : $type"
}

data class Header(val params : List<Param>, val receiver: Type?, val name: String, val arguments: List<Argument>, val returns: Type?) {
    override fun toString(): String = buildString {
        append("fun ")
        if (params.isNotEmpty()) append(params.joinToString(", ", "<", "> "))
        if (receiver != null) append("($receiver).")
        append("$name(${arguments.joinToString()})")
        if (returns != null) append(" : $returns")
    }
}

data class Param(val name: String, val bound : Type?) {
    override fun toString(): String = name + (bound?.let { " : $it" } ?: "")
}

sealed interface Type
data class Concrete(val name: String, val parameters: List<Type>) : Type {
    override fun toString(): String = name + (parameters.ifEmpty { null }?.joinToString(", ", "<", ">") ?: "")
}

data class Function(val from: List<Type>, val into: Type) : Type {
    override fun toString(): String = "(${from.joinToString()}) -> $into"
}

data class Nullable(val type: Type) : Type {
    override fun toString(): String = "($type)?"
}

data object StarProjection : Type {
    override fun toString(): String = "*"
}

