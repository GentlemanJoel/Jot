package com.gentlemanjoel.jot.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Wraps the current selection in [token] (or inserts an empty pair at the cursor
 * and places the caret between the markers, ready to type).
 */
fun wrapSelection(value: TextFieldValue, token: String): TextFieldValue {
    val text = value.text
    val start = minOf(value.selection.start, value.selection.end).coerceIn(0, text.length)
    val end = maxOf(value.selection.start, value.selection.end).coerceIn(0, text.length)
    val selected = text.substring(start, end)

    val newText = text.substring(0, start) + token + selected + token + text.substring(end)
    val caret = if (selected.isEmpty()) start + token.length else end + token.length * 2

    return value.copy(
        text = newText,
        selection = TextRange(caret.coerceIn(0, newText.length)),
    )
}

/**
 * Toggles [prefix] at the start of the line the caret sits on. Switching between
 * list types (bullet to checkbox, say) replaces the old prefix rather than stacking.
 */
fun prefixLine(value: TextFieldValue, prefix: String): TextFieldValue {
    val text = value.text
    val caret = value.selection.start.coerceIn(0, text.length)

    val lineStart = text.lastIndexOf('\n', (caret - 1).coerceAtLeast(0))
        .let { if (it < 0) 0 else it + 1 }
    val lineEnd = text.indexOf('\n', caret).let { if (it < 0) text.length else it }
    val line = text.substring(lineStart, lineEnd)

    val knownPrefixes = listOf("- [ ] ", "- [x] ", "- [X] ", "- ", "* ", "> ", "### ", "## ", "# ")
    val currentPrefix = knownPrefixes.firstOrNull { line.startsWith(it) } ?: ""
    val bare = line.removePrefix(currentPrefix)

    // Tapping the same control again clears it.
    val newLine = if (currentPrefix == prefix) bare else prefix + bare

    val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
    val delta = newLine.length - line.length

    return value.copy(
        text = newText,
        selection = TextRange((caret + delta).coerceIn(lineStart, newText.length)),
    )
}

/**
 * Continues list formatting onto the next line. Pressing Enter inside a checklist
 * should produce another checkbox, not a bare line.
 */
fun continueListOnNewline(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    // Only react to a single newline having just been typed at the caret.
    if (new.text.length != old.text.length + 1) return new
    val caret = new.selection.start
    if (caret < 1 || new.text[caret - 1] != '\n') return new

    val prevLineEnd = caret - 1
    val prevLineStart = new.text.lastIndexOf('\n', (prevLineEnd - 1).coerceAtLeast(0))
        .let { if (it < 0) 0 else it + 1 }
    if (prevLineStart >= prevLineEnd) return new
    val prevLine = new.text.substring(prevLineStart, prevLineEnd)

    val carry = when {
        prevLine.startsWith("- [ ] ") || prevLine.startsWith("- [x] ") ||
            prevLine.startsWith("- [X] ") -> "- [ ] "
        prevLine.startsWith("- ") -> "- "
        prevLine.startsWith("* ") -> "* "
        else -> return new
    }

    // An empty list item means the person is done with the list: clear it instead.
    val prevContent = prevLine.removePrefix(carry).trim()
    if (prevContent.isEmpty() &&
        (prevLine.startsWith("- ") || prevLine.startsWith("* "))
    ) {
        val cleaned = new.text.substring(0, prevLineStart) + new.text.substring(prevLineEnd)
        return TextFieldValue(
            text = cleaned,
            selection = TextRange(prevLineStart.coerceIn(0, cleaned.length)),
        )
    }

    val inserted = new.text.substring(0, caret) + carry + new.text.substring(caret)
    return TextFieldValue(
        text = inserted,
        selection = TextRange((caret + carry.length).coerceIn(0, inserted.length)),
    )
}
