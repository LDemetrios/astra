package org.ldemetrios.astra


class StringBuilderDSL {
    private val sb = StringBuilder()

    private var indent = 0

    operator fun String.unaryPlus(): StringBuilderDSL {
        this.trimIndent()
            .split(Regex("\r\n?|\n"))
            .map {
                " ".repeat(4 * indent) + it
            }.forEach {
                sb.append(it).append(System.lineSeparator())
            }
        return this@StringBuilderDSL
    }

    fun scope(func: StringBuilderDSL.() -> Unit) {
        indent++
        this.func()
        indent--
    }

    override fun toString(): String = sb.toString()
}

internal inline fun string(func: StringBuilderDSL.() -> Unit): String {
    val sb = StringBuilderDSL()
    sb.func()
    return sb.toString()
}

internal val StringBuilderDSL.nl get() = +""

fun String.escape() =
    replace("\u0000", "\\0")
        .replace("\u0001", "\\SOH")
        .replace("\u0002", "\\STX")
        .replace("\u0003", "\\ETX")
        .replace("\u0004", "\\EOT")
        .replace("\u0005", "\\ENQ")
        .replace("\u0006", "\\ACK")
        .replace("\u0007", "\\BEL")
        .replace("\u0008", "\\BS")
        .replace("\u0009", "\\t")
        .replace("\u000A", "\\n")
        .replace("\u000B", "\\VT")
        .replace("\u000C", "\\f")
        .replace("\u000D", "\\r")
        .replace("\u000E", "\\SO")
        .replace("\u000F", "\\SI")
        .replace("\u0010", "\\DLE")
        .replace("\u0011", "\\DC1")
        .replace("\u0012", "\\DC2")
        .replace("\u0013", "\\DC3")
        .replace("\u0014", "\\DC4")
        .replace("\u0015", "\\NAK")
        .replace("\u0016", "\\SYN")
        .replace("\u0017", "\\ETB")
        .replace("\u0018", "\\CAN")
        .replace("\u0019", "\\EM")
        .replace("\u001A", "\\SUB")
        .replace("\u001B", "\\ESC")
        .replace("\u001C", "\\FS")
        .replace("\u001D", "\\GS")
        .replace("\u001E", "\\RS")
        .replace("\u001F", "\\US")

fun <T> List<T>.dropIfEmpty() = ifEmpty { null }

fun regex(ctx: String): String {
    val text = ctx.run { substring(1, length - 1) } // Remove '/'s
    // Any escape remains the same
    // Redundantly escaped '/'s are not a problem
    return text.split(Regex("\\\\\\\\"))
        .joinToString("\\\\") {
            it.split(Regex("\\\\ ")).joinToString(" ") {
                it.replace(Regex("[ \t\n\r]"), "")
            }
        }
}

fun main() {
    println(regex("""
            /    [0-9] ([0-9_]* [0-9])?\\ \.\ ([0-9] ([0-9_]* [0-9])?)? ([eE] [+-]? [0-9] ([0-9_]* [0-9])?)? ([fFdD])?
                | \. [0-9] ([0-9_]* [0-9])? ([eE] [+-]? [0-9] ([0-9_]* [0-9])?)? ([fFdD])?
                | [0-9] ([0-9_]* [0-9])? [eE] [+-]? [0-9] ([0-9_]* [0-9])? ([fFdD])?
                | [0-9] ([0-9_]* [0-9])? [fFdD] /
    """.trim()))
}


fun literal(ctx: String): String {
    val text = ctx.run { substring(1, length - 1) } // Remove '`'s
    return text
        .replace(Regex("\\\\([\\\\nr`]|u[A-Fa-f0-9]{4})")) {
            if (it.value.length == 2) when (it.value[1]) {
                'n' -> "\n"
                'r' -> "\r"
                '`' -> "`"
                '\\' -> "\\"
                else -> throw AssertionError("Incorrect escape ${it.value}")
            } else it.value.substring(2, 6).lowercase().toInt(16).toChar().toString()
        }

}

fun String.stripCode() = trim().removePrefix("{").removeSuffix("}")