package org.ldemetrios.astra.api

import kotlin.math.max
import kotlin.math.min

class ParsingException(
    message: String, val input: String, val start: Int, val end: Int
) : Exception(
    message + "\nError at range $start:$end\n" + highlightRange(input, start, end)
)

private fun highlightRange(input: String, start: Int, end: Int): String {
    require(start <= end) { "Start position must be <= end position." }
    require(!(start < 0 || end > input.length)) { "Start and end positions must be within the input string." }

    val lines = input.replace("\r\n", " \n").split("[\r\n]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    val result = StringBuilder()

    var currentPos = 0
    for (line in lines) {
        val lineStart = currentPos
        val lineEnd = lineStart + line.length

        if (end <= lineStart) break

        if (start < lineEnd) {
            result.append(line).append("\n")

            val caretStart = max(start, lineStart) - lineStart
            val caretEnd = min(end, lineEnd) - lineStart

            // Generate carets for this line
            result.append(" ".repeat(caretStart))
                .append("^".repeat(caretEnd - caretStart))
                .append("\n")
        }

        currentPos = lineEnd + 1
    }

    return result.toString()
}

