package com.gentlemanjoel.jot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Inline markdown: **bold**, *italic*, `code`, ~~strike~~. */
fun inlineMarkdown(raw: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < raw.length) {
        val rest = raw.substring(i)
        val match = listOf(
            Triple("**", SpanStyle(fontWeight = FontWeight.Bold), 2),
            Triple("~~", SpanStyle(textDecoration = TextDecoration.LineThrough), 2),
            Triple("`", SpanStyle(fontFamily = FontFamily.Monospace, color = Slate.Accent), 1),
            Triple("*", SpanStyle(fontStyle = FontStyle.Italic), 1),
        ).firstOrNull { (token, _, _) ->
            rest.startsWith(token) && rest.indexOf(token, token.length) > token.length - 1
        }

        if (match != null) {
            val (token, style, len) = match
            val close = rest.indexOf(token, len)
            if (close > len) {
                pushStyle(style)
                append(rest.substring(len, close))
                pop()
                i += close + len
                continue
            }
        }
        append(raw[i])
        i++
    }
}

@Composable
fun MarkdownPreview(text: String, modifier: Modifier = Modifier) {
    val lines = text.lines()
    var inCode = false
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        lines.forEach { line ->
            val t = line.trimEnd()
            when {
                t.trimStart().startsWith("```") -> {
                    inCode = !inCode
                    Spacer(Modifier.height(4.dp))
                }
                inCode -> Text(
                    t,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Slate.Text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate.Secondary)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
                t.startsWith("### ") -> Text(
                    inlineMarkdown(t.removePrefix("### ")),
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Slate.Text,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                )
                t.startsWith("## ") -> Text(
                    inlineMarkdown(t.removePrefix("## ")),
                    fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Slate.Text,
                    modifier = Modifier.padding(top = 12.dp, bottom = 3.dp)
                )
                t.startsWith("# ") -> Text(
                    inlineMarkdown(t.removePrefix("# ")),
                    fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Slate.Text,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                t.trimStart().startsWith("- [x]") || t.trimStart().startsWith("- [X]") -> CheckRow(
                    checked = true, label = t.trimStart().drop(5).trim()
                )
                t.trimStart().startsWith("- [ ]") -> CheckRow(
                    checked = false, label = t.trimStart().drop(5).trim()
                )
                t.trimStart().startsWith("- ") || t.trimStart().startsWith("* ") -> Row(
                    Modifier.padding(vertical = 1.dp)
                ) {
                    Text("•", color = Slate.Accent, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(inlineMarkdown(t.trimStart().drop(2)), color = Slate.Text, fontSize = 16.sp)
                }
                t.startsWith("> ") -> Row(Modifier.padding(vertical = 3.dp)) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(20.dp)
                            .background(Slate.Accent)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(inlineMarkdown(t.removePrefix("> ")), color = Slate.Muted, fontSize = 16.sp)
                }
                t.startsWith("---") -> Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(vertical = 0.dp)
                        .background(Slate.Divider)
                )
                t.isBlank() -> Spacer(Modifier.height(8.dp))
                else -> Text(
                    inlineMarkdown(t),
                    color = Slate.Text,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun CheckRow(checked: Boolean, label: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(
            if (checked) "☑" else "☐",
            color = if (checked) Slate.Accent else Slate.Muted,
            fontSize = 16.sp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            inlineMarkdown(label),
            color = if (checked) Slate.Muted else Slate.Text,
            fontSize = 16.sp,
            textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
        )
    }
}

// ---- toolbar actions ----

/** Wraps the selection (or inserts a placeholder) with [token]. */
fun wrapSelection(value: TextFieldValue, token: String): TextFieldValue {
    val sel = value.selection
    val text = value.text
    val start = minOf(sel.start, sel.end)
    val end = maxOf(sel.start, sel.end)
    val selected = text.substring(start, end)
    val newText = text.substring(0, start) + token + selected + token + text.substring(end)
    val cursor = if (selected.isEmpty()) start + token.length else end + token.length * 2
    return value.copy(
        text = newText,
        selection = androidx.compose.ui.text.TextRange(cursor),
    )
}

/** Puts [prefix] at the start of the line the cursor is on (toggles it off if already there). */
fun prefixLine(value: TextFieldValue, prefix: String): TextFieldValue {
    val text = value.text
    val cursor = value.selection.start.coerceIn(0, text.length)
    val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
    val lineEnd = text.indexOf('\n', cursor).let { if (it < 0) text.length else it }
    val line = text.substring(lineStart, lineEnd)

    val newLine = if (line.startsWith(prefix)) line.removePrefix(prefix) else prefix + line
    val delta = newLine.length - line.length
    val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
    return value.copy(
        text = newText,
        selection = androidx.compose.ui.text.TextRange((cursor + delta).coerceIn(0, newText.length)),
    )
}
