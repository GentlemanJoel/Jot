package com.gentlemanjoel.jot

import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A folder or a .md file in the notes tree. */
data class NoteNode(
    val file: File,
    val isFolder: Boolean,
    val children: List<NoteNode> = emptyList(),
) {
    val displayName: String
        get() = if (isFolder) file.name else file.name.removeSuffix(".md")
}

/** An open note, persisted or not. */
data class Draft(
    val file: File?,
    val dir: File,
    val title: String = "",
    val body: String = "",
) {
    val isBlank: Boolean get() = title.isBlank() && body.isBlank()
    val isNew: Boolean get() = file == null
}

object NoteStore {

    /** Syncthing-friendly: a plain folder in shared storage. */
    val root: File
        get() = File(Environment.getExternalStorageDirectory(), "Jot")

    private val skipNames = setOf(".stfolder", ".stversions", ".trash", ".obsidian", ".git")

    fun ensureRoot(): File {
        if (!root.exists()) root.mkdirs()
        return root
    }

    fun loadTree(dir: File = ensureRoot()): List<NoteNode> {
        val entries = dir.listFiles() ?: return emptyList()
        val folders = entries
            .filter { it.isDirectory && !it.isHidden && it.name !in skipNames }
            .sortedBy { it.name.lowercase(Locale.US) }
            .map { NoteNode(it, true, loadTree(it)) }
        val notes = entries
            .filter { it.isFile && it.name.endsWith(".md", ignoreCase = true) && !it.isHidden }
            .sortedByDescending { it.lastModified() }
            .map { NoteNode(it, false) }
        return folders + notes
    }

    fun flatten(nodes: List<NoteNode>): List<NoteNode> {
        val out = mutableListOf<NoteNode>()
        fun walk(list: List<NoteNode>) {
            list.forEach {
                out += it
                if (it.isFolder) walk(it.children)
            }
        }
        walk(nodes)
        return out
    }

    fun search(nodes: List<NoteNode>, query: String): List<NoteNode> {
        val q = query.trim().lowercase(Locale.US)
        if (q.isEmpty()) return emptyList()
        return flatten(nodes)
            .filter { !it.isFolder }
            .filter { node ->
                node.displayName.lowercase(Locale.US).contains(q) ||
                    runCatching { node.file.readText() }.getOrDefault("")
                        .lowercase(Locale.US).contains(q)
            }
            .take(60)
    }

    fun read(file: File): Draft {
        val body = runCatching { file.readText() }.getOrDefault("")
        return Draft(file = file, dir = file.parentFile ?: ensureRoot(), title = file.name.removeSuffix(".md"), body = body)
    }

    /**
     * Writes the draft. Returns the saved file, or null if the draft was blank
     * (in which case any existing file is removed — the "click off to discard" path).
     */
    fun save(draft: Draft): File? {
        if (draft.isBlank) {
            draft.file?.delete()
            return null
        }
        if (!draft.dir.exists()) draft.dir.mkdirs()

        val desired = fileNameFor(draft)
        val existing = draft.file

        val target: File = when {
            existing == null -> uniqueFile(draft.dir, desired)
            existing.name.removeSuffix(".md") == desired -> existing
            else -> {
                val renamed = uniqueFile(draft.dir, desired)
                if (existing.exists()) existing.renameTo(renamed)
                renamed
            }
        }

        runCatching { target.writeText(draft.body) }
        return target
    }

    fun delete(file: File): Boolean = runCatching { file.deleteRecursively() }.getOrDefault(false)

    fun createFolder(parent: File, name: String): File? {
        val clean = sanitize(name).ifBlank { return null }
        val f = File(parent, clean)
        return if (f.mkdirs() || f.isDirectory) f else null
    }

    fun rename(node: NoteNode, newName: String): File? {
        val clean = sanitize(newName).ifBlank { return null }
        val target = File(node.file.parentFile, if (node.isFolder) clean else "$clean.md")
        if (target.exists()) return null
        return if (node.file.renameTo(target)) target else null
    }

    fun move(node: NoteNode, destDir: File): File? {
        val target = File(destDir, node.file.name)
        if (target.exists()) return null
        return if (node.file.renameTo(target)) target else null
    }

    // ---- helpers ----

    private fun fileNameFor(draft: Draft): String {
        val fromTitle = sanitize(draft.title)
        if (fromTitle.isNotBlank()) return fromTitle

        val firstLine = draft.body.lineSequence()
            .map { it.trim().trimStart('#', '-', '*', '>', ' ') }
            .firstOrNull { it.isNotBlank() }
            ?.take(48)
            ?: ""
        val fromBody = sanitize(firstLine)
        if (fromBody.isNotBlank()) return fromBody

        val stamp = SimpleDateFormat("yyyy-MM-dd HHmm", Locale.US).format(Date())
        return "Jot $stamp"
    }

    private fun sanitize(raw: String): String =
        raw.replace(Regex("[\\\\/:*?\"<>|\\n\\r\\t]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.')
            .take(80)

    private fun uniqueFile(dir: File, baseName: String): File {
        var candidate = File(dir, "$baseName.md")
        var n = 2
        while (candidate.exists()) {
            candidate = File(dir, "$baseName ($n).md")
            n++
        }
        return candidate
    }
}
