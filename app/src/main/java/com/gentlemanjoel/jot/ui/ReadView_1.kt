package com.gentlemanjoel.jot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Read mode: the note rendered with real tappable checkboxes.
 *
 * This is what an existing note opens into, so ticking something off a list never
 * requires entering edit mode. Tapping any non-checkbox line switches to editing.
 */
@Composable
fun ReadView(
    text: String,
    onToggleLine: (Int) -> Unit,
    onEditRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lines = text.lines()
    var inCode = false

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        lines.forEachIndexed { index, raw ->
            val line = raw.trimEnd()
            when {
                line.trimStart().startsWith("```") -> {
                    inCode = !inCode
                    Spacer(Modifier.height(4.dp))
                }

                inCode -> Text(
                    line,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Slate.Text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate.Secondary)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                        .clickable { onEditRequest() }
                )

                LiveMarkdownTransformation.isCheckbox(line) -> {
                    val checked = line[3] != ' '
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleLine(index) }
                            .padding(vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = if (checked) Icons.Filled.CheckBox
                            else Icons.Filled.CheckBoxOutlineBlank,
                            contentDescription = if (checked) "Checked" else "Unchecked",
                            tint = if (checked) Slate.Accent else Slate.Muted,
                            modifier = Modifier.size(23.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            inlineSpans(line.drop(6)),
                            color = if (checked) Slate.Muted else Slate.Text,
                            fontSize = 16.sp,
                            textDecoration = if (checked) TextDecoration.LineThrough
                            else TextDecoration.None,
                        )
                    }
                }

                line.startsWith("### ") -> HeadingRow(line.removePrefix("### "), 17.sp, FontWeight.SemiBold, onEditRequest)
                line.startsWith("## ") -> HeadingRow(line.removePrefix("## "), 20.sp, FontWeight.SemiBold, onEditRequest)
                line.startsWith("# ") -> HeadingRow(line.removePrefix("# "), 24.sp, FontWeight.Bold, onEditRequest)

                line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditRequest() }
                        .padding(vertical = 2.dp)
                ) {
                    Text("•", color = Slate.Accent, fontSize = 16.sp)
                    Spacer(Modifier.width(9.dp))
                    Text(inlineSpans(line.trimStart().drop(2)), color = Slate.Text, fontSize = 16.sp)
                }

                line.startsWith("> ") -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditRequest() }
                        .padding(vertical = 3.dp)
                ) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(20.dp)
                            .background(Slate.Accent)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        inlineSpans(line.removePrefix("> ")),
                        color = Slate.Muted,
                        fontSize = 16.sp,
                        fontStyle = FontStyle.Italic,
                    )
                }

                line.startsWith("---") -> Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Slate.Divider)
                )

                line.isBlank() -> Box(
                    Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clickable { onEditRequest() }
                )

                else -> Text(
                    inlineSpans(line),
                    color = Slate.Text,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditRequest() }
                        .padding(vertical = 2.dp)
                )
            }
        }

        // Tap target so the empty space below a short note still enters edit mode.
        Box(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clickable { onEditRequest() }
        )
    }
}

@Composable
private fun HeadingRow(
    content: String,
    size: androidx.compose.ui.unit.TextUnit,
    weight: FontWeight,
    onEditRequest: () -> Unit,
) {
    Text(
        inlineSpans(content),
        fontSize = size,
        fontWeight = weight,
        color = Slate.Text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditRequest() }
            .padding(top = 12.dp, bottom = 3.dp)
    )
}

/** Renders `**bold**`, `*italic*`, `` `code` `` and `~~strike~~` with the markers removed. */
fun inlineSpans(raw: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    val tokens = listOf(
        "**" to SpanStyle(fontWeight = FontWeight.Bold),
        "~~" to SpanStyle(textDecoration = TextDecoration.LineThrough),
        "`" to SpanStyle(fontFamily = FontFamily.Monospace, color = Slate.Accent),
        "*" to SpanStyle(fontStyle = FontStyle.Italic),
    )

    outer@ while (i < raw.length) {
        for ((token, style) in tokens) {
            if (raw.startsWith(token, i)) {
                val contentStart = i + token.length
                val close = raw.indexOf(token, contentStart)
                if (close > contentStart) {
                    pushStyle(style)
                    append(raw.substring(contentStart, close))
                    pop()
                    i = close + token.length
                    continue@outer
                }
            }
        }
        append(raw[i])
        i++
    }
}
