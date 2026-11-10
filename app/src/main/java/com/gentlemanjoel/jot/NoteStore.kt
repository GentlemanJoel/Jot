package com.gentlemanjoel.jot

import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One item sitting in the trash, with where it came from. */
data class TrashItem(
    val file: File,
    val originalRelPath: String,
    val deletedAt: Long,
    val isFolder: Boolean,
) {
    val displayName: String
        get() = originalRelPath.substringAfterLast('/').removeSuffix(".md")

    /** The folder it used to live in, "" when it sat at the top level. */
    val originalParent: String
        get() = originalRelPath.substringBeforeLast('/', "")
}

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

    // ---------------- trash ----------------

    /**
     * Deleting moves the file here instead of unlinking it, so an accidental
     * tap is recoverable. Hidden from the tree by [skipNames], and Syncthing
     * carries it to the PC like any other folder.
     *
     * The name encodes when it was binned and where it came from:
     * `<epochMillis>__<escaped relative path>`, where the path has `%` and
     * `/` percent-escaped so it round-trips whatever the note was called.
     */
    val trashDir: File get() = File(root, ".trash")

    private const val TRASH_SEP = "__"

    // `/` is the one character a file name cannot contain, so escaping it
    // (and `%` itself, first) makes the encoding reversible for any name.
    private fun encodeRel(rel: String): String =
        rel.replace("%", "%25").replace("/", "%2F")

    private fun decodeRel(name: String): String =
        name.replace("%2F", "/").replace("%25", "%")

    fun ensureTrash(): File {
        val d = trashDir
        if (!d.exists()) d.mkdirs()
        return d
    }

    private fun relToRoot(file: File): String =
        file.absolutePath.removePrefix(root.absolutePath).trimStart('/')

    /**
     * Moves [file] into the trash. Returns true when it is no longer at its
     * old path. Falls back to a plain delete only if the move itself fails,
     * so a delete never silently does nothing.
     */
    fun moveToTrash(file: File): Boolean {
        if (!file.exists()) return false
        val rel = relToRoot(file)
        if (rel.isBlank()) return false

        val dir = ensureTrash()
        val stamp = System.currentTimeMillis()
        val encoded = encodeRel(rel)
        var target = File(dir, "$stamp$TRASH_SEP$encoded")
        var n = 2
        while (target.exists()) {
            target = File(dir, "$stamp-$n$TRASH_SEP$encoded")
            n++
        }
        if (runCatching { file.renameTo(target) }.getOrDefault(false)) return true
        // Different volume, or a locked handle: copy then remove.
        val copied = runCatching {
            file.copyRecursively(target, overwrite = true)
        }.getOrDefault(false)
        return if (copied) runCatching { file.deleteRecursively() }.getOrDefault(false) else false
    }

    fun trashItems(): List<TrashItem> {
        val entries = trashDir.listFiles() ?: return emptyList()
        return entries.mapNotNull { f ->
            val idx = f.name.indexOf(TRASH_SEP)
            if (idx <= 0) return@mapNotNull null
            val stampPart = f.name.substring(0, idx).substringBefore('-')
            val stamp = stampPart.toLongOrNull() ?: return@mapNotNull null
            val rel = decodeRel(f.name.substring(idx + TRASH_SEP.length))
            if (rel.isBlank()) null
            else TrashItem(f, rel, stamp, f.isDirectory)
        }.sortedByDescending { it.deletedAt }
    }

    /**
     * Puts an item back where it came from. If something now occupies that
     * name, the restored copy is given a numbered suffix rather than
     * overwriting whatever is there.
     */
    fun restoreFromTrash(item: TrashItem): File? {
        val dest = File(root, item.originalRelPath)
        val parent = dest.parentFile ?: return null
        if (!parent.exists() && !parent.mkdirs()) return null

        var target = dest
        if (target.exists()) {
            val base = if (item.isFolder) dest.name else dest.name.removeSuffix(".md")
            val ext = if (item.isFolder) "" else ".md"
            var n = 2
            while (target.exists()) {
                target = File(parent, "$base ($n)$ext")
                n++
            }
        }
        return if (runCatching { item.file.renameTo(target) }.getOrDefault(false)) target else null
    }

    fun deleteFromTrash(item: TrashItem): Boolean =
        runCatching { item.file.deleteRecursively() }.getOrDefault(false)

    fun emptyTrash(): Int {
        val items = trashItems()
        var n = 0
        items.forEach { if (deleteFromTrash(it)) n++ }
        return n
    }

    /** Drops anything binned longer ago than [keepDays]. Called on launch. */
    fun purgeTrash(keepDays: Int): Int {
        val cutoff = System.currentTimeMillis() - keepDays * 24L * 60L * 60L * 1000L
        var n = 0
        trashItems().filter { it.deletedAt < cutoff }.forEach { if (deleteFromTrash(it)) n++ }
        return n
    }

    /** Notes at or beneath [dir], used to warn before deleting a folder. */
    fun countNotesUnder(dir: File): Int {
        if (!dir.isDirectory) return 0
        return dir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".md", ignoreCase = true) && !it.isHidden }
            .count()
    }

    /** Every folder in the tree, for the "move to" picker. */
    fun allFolders(): List<File> {
        val out = mutableListOf(ensureRoot())
        fun walk(dir: File) {
            dir.listFiles()
                ?.filter { it.isDirectory && !it.isHidden && it.name !in skipNames }
                ?.sortedBy { it.name.lowercase(Locale.US) }
                ?.forEach { out += it; walk(it) }
        }
        walk(root)
        return out
    }

    /** Path shown in the move picker, relative to the notes root. */
    fun labelFor(dir: File): String =
        relToRoot(dir).ifBlank { "Jot (top level)" }

    /** Permanently removes a file. Prefer [moveToTrash]. */
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
