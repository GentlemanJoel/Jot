package com.gentlemanjoel.jot.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * Renders markdown styling live in the editor while HIDING the syntax characters.
 *
 * The underlying text keeps real markdown (so the .md files stay portable to a PC
 * and Obsidian), but the person never sees `**`, `##`, or `- [ ]`. Bold text simply
 * looks bold; a checkbox line simply shows a box glyph.
 *
 * Correct offset mapping is the whole game here: every visual character records which
 * source character it came from, and the inverse table is derived from that, so the
 * cursor and selection stay put even though characters are being removed.
 */
class LiveMarkdownTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val src = text.text
        val out = StringBuilder(src.length)

        // visSrc[v] = index in src that visual char v came from (monotonic non-decreasing)
        val visSrc = ArrayList<Int>(src.length + 8)
        // style ranges in VISUAL coordinates
        val styles = ArrayList<Triple<Int, Int, SpanStyle>>()

        fun emit(ch: Char, srcIndex: Int) {
            out.append(ch)
            visSrc.add(srcIndex)
        }

        var i = 0
        while (true) {
            val lineEnd = src.indexOf('\n', i).let { if (it < 0) src.length else it }
            val line = src.substring(i, lineEnd)
            val lineVisStart = out.length

            var p = 0
            var lineStyle: SpanStyle? = null

            // ---------------- line-level prefixes ----------------
            when {
                line.startsWith("### ") -> {
                    p = 4
                    lineStyle = SpanStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                line.startsWith("## ") -> {
                    p = 3
                    lineStyle = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
                line.startsWith("# ") -> {
                    p = 2
                    lineStyle = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                isCheckbox(line) -> {
                    val checked = line[3] != ' '
                    emit(if (checked) '☑' else '☐', i)
                    emit(' ', i + 1)
                    styles.add(
                        Triple(
                            lineVisStart, lineVisStart + 1,
                            SpanStyle(
                                color = if (checked) Slate.Accent else Slate.Muted,
                                fontSize = 19.sp,
                            )
                        )
                    )
                    if (checked) {
                        lineStyle = SpanStyle(
                            color = Slate.Muted,
                            textDecoration = TextDecoration.LineThrough,
                        )
                    }
                    p = 6
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    emit('•', i)
                    emit(' ', i + 1)
                    styles.add(
                        Triple(lineVisStart, lineVisStart + 1, SpanStyle(color = Slate.Accent))
                    )
                    p = 2
                }
                line.startsWith("> ") -> {
                    emit('│', i)
                    emit(' ', i + 1)
                    styles.add(
                        Triple(lineVisStart, lineVisStart + 1, SpanStyle(color = Slate.Accent))
                    )
                    lineStyle = SpanStyle(color = Slate.Muted, fontStyle = FontStyle.Italic)
                    p = 2
                }
            }

            // ---------------- inline markers ----------------
            while (p < line.length) {
                val consumed = tryInline(line, p, i, out, visSrc, styles)
                if (consumed > 0) {
                    p += consumed
                } else {
                    emit(line[p], i + p)
                    p++
                }
            }

            if (lineStyle != null && out.length > lineVisStart) {
                styles.add(Triple(lineVisStart, out.length, lineStyle))
            }

            if (lineEnd >= src.length) break
            emit('\n', lineEnd)
            i = lineEnd + 1
        }

        // ---------------- build annotated result ----------------
        val visual = AnnotatedString.Builder(out.toString()).apply {
            val len = out.length
            styles.forEach { (s, e, style) ->
                val a = s.coerceIn(0, len)
                val b = e.coerceIn(a, len)
                if (b > a) addStyle(style, a, b)
            }
        }.toAnnotatedString()

        // source offset -> visual offset
        val srcToVis = IntArray(src.length + 1)
        var v = 0
        for (s in 0..src.length) {
            while (v < visSrc.size && visSrc[v] < s) v++
            srcToVis[s] = v
        }

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val o = offset.coerceIn(0, src.length)
                return srcToVis[o].coerceIn(0, out.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val o = offset.coerceIn(0, out.length)
                if (o >= visSrc.size) return src.length
                return visSrc[o].coerceIn(0, src.length)
            }
        }

        return TransformedText(visual, mapping)
    }

    /**
     * Attempts to consume an inline marker at [p] within [line]. Emits the inner text
     * (markers hidden) and records its style. Returns how many SOURCE chars of the line
     * were consumed, or 0 if no marker matched.
     */
    private fun tryInline(
        line: String,
        p: Int,
        lineSrcStart: Int,
        out: StringBuilder,
        visSrc: ArrayList<Int>,
        styles: ArrayList<Triple<Int, Int, SpanStyle>>,
    ): Int {
        val candidates = listOf(
            "**" to SpanStyle(fontWeight = FontWeight.Bold),
            "~~" to SpanStyle(textDecoration = TextDecoration.LineThrough),
            "`" to SpanStyle(fontFamily = FontFamily.Monospace, color = Slate.Accent),
            "*" to SpanStyle(fontStyle = FontStyle.Italic),
        )

        for ((token, style) in candidates) {
            if (!line.startsWith(token, p)) continue
            val contentStart = p + token.length
            val close = line.indexOf(token, contentStart)
            // need a closing token with at least one char between
            if (close < 0 || close == contentStart) continue

            val innerVisStart = out.length
            var q = contentStart
            while (q < close) {
                out.append(line[q])
                visSrc.add(lineSrcStart + q)
                q++
            }
            styles.add(Triple(innerVisStart, out.length, style))
            return (close + token.length) - p
        }
        return 0
    }

    companion object {
        fun isCheckbox(line: String): Boolean =
            line.length >= 6 &&
                line.startsWith("- [") &&
                line[4] == ']' &&
                line[5] == ' ' &&
                (line[3] == ' ' || line[3] == 'x' || line[3] == 'X')
    }
}

// ------------------------------------------------------------------
// Text manipulation helpers used by the formatting toolbar
// ------------------------------------------------------------------

/** Toggles the checkbox state of the line at [lineIndex] in [text]. */
fun toggleCheckboxAt(text: String, lineIndex: Int): String {
    val lines = text.lines().toMutableList()
    if (lineIndex !in lines.indices) return text
    val line = lines[lineIndex]
    lines[lineIndex] = when {
        line.startsWith("- [ ] ") -> "- [x] " + line.removePrefix("- [ ] ")
        line.startsWith("- [x] ") -> "- [ ] " + line.removePrefix("- [x] ")
        line.startsWith("- [X] ") -> "- [ ] " + line.removePrefix("- [X] ")
        else -> line
    }
    return lines.joinToString("\n")
}
