package com.gentlemanjoel.jot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** A note pinned to the home panel, with its chosen icon and color. */
data class Pin(
    val path: String,
    val icon: String = "note",
    val color: String = "blue",
) {
    val displayName: String
        get() = path.substringAfterLast('/').removeSuffix(".md")
}

/**
 * Width of the pinned panel. The smallest option is a quarter of what used to
 * be the narrowest setting, which only fits the icon - so anything under
 * [LABEL_FLOOR] drops the note name and shows the icon alone.
 */
enum class PanelWidth(val label: String, val widthDp: Int) {
    ICON_TINY("Tiny - icon only", 48),
    ICON_SMALL("Small - icon only", 58),
    ICON_MEDIUM("Medium - icon only", 70),
    ICON_LARGE("Large - icon only", 86),
    ICON_HUGE("Huge - icon only", 104),
    LABEL_SLIM("Slim - with names", 140),
    LABEL_NARROW("Narrow - with names", 170);

    /** Too narrow for a readable name, so the row shows just the icon. */
    val iconsOnly: Boolean get() = widthDp < LABEL_FLOOR

    companion object {
        const val LABEL_FLOOR = 130
        val DEFAULT = LABEL_NARROW
    }
}

/** Size of the new-note button that floats over the bottom-right corner. */
enum class FabSize(val label: String, val sizeDp: Int, val iconDp: Int) {
    SMALL("Small", 44, 21),
    MEDIUM("Medium", 56, 26),
    LARGE("Large", 68, 32),
    XLARGE("Extra large", 82, 39),
    HUGE("Huge", 96, 46);

    companion object {
        val DEFAULT = LARGE
    }
}

/** A labelled set of icon keys, so the picker can be browsed by category. */
data class IconGroup(val label: String, val keys: List<String>)

object Prefs {

    private const val FILE = "jot_prefs"
    private const val KEY_PINS = "pins_json"
    private const val KEY_PANEL = "panel_width"
    private const val KEY_FAB = "fab_size"
    private const val KEY_TOKEN = "github_token"
    private const val KEY_RESUME_NEW = "resume_new_note"
    private const val KEY_RESUME_MINS = "resume_after_minutes"
    private const val KEY_LOCKED = "locked_paths"
    private const val KEY_DEFAULT_DIR = "default_new_note_dir"

    /** How long a trashed note is kept before it is purged for good. */
    const val TRASH_KEEP_DAYS = 30

    /** Minute thresholds offered for "start a new note when I come back". */
    val RESUME_CHOICES = listOf(1, 2, 5, 15, 60)
    const val RESUME_DEFAULT_MINS = 2

    /**
     * Icons offered in the picker, grouped by what a note is likely to be about.
     * Every key is resolved to a vector by iconFor() in the UI layer.
     */
    val ICON_GROUPS = listOf(
        IconGroup(
            "General", listOf(
                "note", "check", "star", "flag", "bolt", "heart",
                "pin", "bookmark", "tag", "fire", "clock", "alarm",
            )
        ),
        IconGroup(
            "Productivity", listOf(
                "task", "list", "idea", "target", "trending", "timer",
                "calendar", "event", "inbox", "send", "priority", "edit",
            )
        ),
        IconGroup(
            "Office", listOf(
                "work", "folder", "archive", "print", "mail", "phone",
                "badge", "chart", "table", "receipt", "assignment", "briefcase",
            )
        ),
        IconGroup(
            "Money", listOf(
                "money", "wallet", "card", "bank", "savings",
                "cart", "shop", "gift", "percent",
            )
        ),
        IconGroup(
            "Home", listOf(
                "home", "bed", "chair", "cleaning", "laundry", "tools",
                "build", "yard", "pets", "plant", "key",
            )
        ),
        IconGroup(
            "Cooking", listOf(
                "restaurant", "cafe", "pizza", "cake", "icecream", "wine",
                "beer", "egg", "bakery", "grocery", "kitchen", "recipe",
            )
        ),
        IconGroup(
            "Sports", listOf(
                "run", "bike", "gym", "soccer", "basketball", "football",
                "baseball", "tennis", "golf", "hike", "swim", "ski",
            )
        ),
        IconGroup(
            "Entertainment", listOf(
                "movie", "tv", "music", "headphones", "game", "camera",
                "photo", "mic", "book", "palette", "theater", "radio",
            )
        ),
        IconGroup(
            "Travel", listOf(
                "car", "flight", "train", "map", "place",
                "beach", "luggage", "hotel", "fuel", "explore",
            )
        ),
        IconGroup(
            "Health", listOf(
                "health", "spa", "water", "sleep", "medical", "pill",
            )
        ),
        IconGroup(
            "Tech", listOf(
                "code", "laptop", "mobile", "cloud", "wifi",
                "bug", "terminal", "database",
            )
        ),
    )

    /** Flat list of every icon key, in group order. */
    val ICON_KEYS: List<String> = ICON_GROUPS.flatMap { it.keys }

    /** Color keys offered in the picker. Resolved by colorFor() in the UI layer. */
    val COLOR_KEYS = listOf(
        "blue", "sky", "indigo", "violet", "orchid",
        "pink", "rose", "red", "orange", "sun",
        "amber", "gold", "lime", "green", "emerald",
        "teal", "cyan", "steel", "sand", "slate",
    )

    private fun prefs(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // ---------------- panel width ----------------

    fun panelWidth(c: Context): PanelWidth {
        val name = prefs(c).getString(KEY_PANEL, PanelWidth.DEFAULT.name)
        return runCatching { PanelWidth.valueOf(name!!) }.getOrDefault(PanelWidth.DEFAULT)
    }

    fun setPanelWidth(c: Context, w: PanelWidth) {
        prefs(c).edit().putString(KEY_PANEL, w.name).apply()
    }

    // ---------------- new-note button size ----------------

    fun fabSize(c: Context): FabSize {
        val name = prefs(c).getString(KEY_FAB, FabSize.DEFAULT.name)
        return runCatching { FabSize.valueOf(name!!) }.getOrDefault(FabSize.DEFAULT)
    }

    fun setFabSize(c: Context, s: FabSize) {
        prefs(c).edit().putString(KEY_FAB, s.name).apply()
    }

    // ---------------- github token ----------------

    // ---------------- reopen behavior ----------------

    /**
     * Whether coming back to the app after a while should drop you into a
     * fresh note. Off by default: a quick trip to another app to copy
     * something should put you back where you were.
     */
    fun resumeNew(c: Context): Boolean = prefs(c).getBoolean(KEY_RESUME_NEW, false)

    fun setResumeNew(c: Context, on: Boolean) {
        prefs(c).edit().putBoolean(KEY_RESUME_NEW, on).apply()
    }

    /** How long away counts as "a while", in minutes. */
    fun resumeMinutes(c: Context): Int =
        prefs(c).getInt(KEY_RESUME_MINS, RESUME_DEFAULT_MINS)

    fun setResumeMinutes(c: Context, mins: Int) {
        prefs(c).edit().putInt(KEY_RESUME_MINS, mins).apply()
    }

    // ---------------- delete locks ----------------

    /**
     * Paths the user has locked. A locked note cannot be deleted from inside
     * the app, and a folder holding one cannot be deleted either. This is app
     * state, not file state, so deleting the file on the PC still works.
     */
    fun lockedPaths(c: Context): Set<String> =
        prefs(c).getStringSet(KEY_LOCKED, emptySet())?.toSet() ?: emptySet()

    fun isLocked(c: Context, path: String): Boolean = lockedPaths(c).contains(path)

    fun setLocked(c: Context, path: String, locked: Boolean) {
        val next = lockedPaths(c).toMutableSet()
        if (locked) next.add(path) else next.remove(path)
        prefs(c).edit().putStringSet(KEY_LOCKED, next).apply()
    }

    /** Locked paths at or beneath [dir], so a folder delete can be blocked. */
    fun lockedUnder(c: Context, dir: String): List<String> {
        val prefix = dir.trimEnd('/') + "/"
        return lockedPaths(c).filter { it == dir || it.startsWith(prefix) }
    }

    // ---------------- default folder for new notes ----------------

    /** Where a new note is filed when no folder was chosen. Null means the root. */
    fun defaultDir(c: Context): String? =
        prefs(c).getString(KEY_DEFAULT_DIR, "")?.takeIf { it.isNotBlank() }

    fun setDefaultDir(c: Context, path: String?) {
        prefs(c).edit().putString(KEY_DEFAULT_DIR, path ?: "").apply()
    }

    fun token(c: Context): String = prefs(c).getString(KEY_TOKEN, "") ?: ""

    fun setToken(c: Context, t: String) {
        prefs(c).edit().putString(KEY_TOKEN, t.trim()).apply()
    }

    // ---------------- pins ----------------

    fun pins(c: Context): List<Pin> {
        val raw = prefs(c).getString(KEY_PINS, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val o = arr.optJSONObject(idx) ?: return@mapNotNull null
                val p = o.optString("path")
                if (p.isBlank()) null
                else Pin(p, o.optString("icon", "note"), o.optString("color", "blue"))
            }
        }.getOrDefault(emptyList())
    }

    fun savePins(c: Context, list: List<Pin>) {
        val arr = JSONArray()
        list.forEach { pin ->
            arr.put(
                JSONObject().apply {
                    put("path", pin.path)
                    put("icon", pin.icon)
                    put("color", pin.color)
                }
            )
        }
        prefs(c).edit().putString(KEY_PINS, arr.toString()).apply()
    }

    fun isPinned(c: Context, path: String): Boolean = pins(c).any { it.path == path }

    fun pin(c: Context, path: String) {
        val list = pins(c).toMutableList()
        if (list.none { it.path == path }) {
            list.add(Pin(path))
            savePins(c, list)
        }
    }

    fun unpin(c: Context, path: String) {
        savePins(c, pins(c).filterNot { it.path == path })
    }

    fun updatePin(c: Context, path: String, icon: String? = null, color: String? = null) {
        savePins(
            c,
            pins(c).map { p ->
                if (p.path == path) p.copy(icon = icon ?: p.icon, color = color ?: p.color) else p
            }
        )
    }

    /** Moves a pin up (-1) or down (+1) in the list. */
    fun movePin(c: Context, path: String, delta: Int) {
        val list = pins(c).toMutableList()
        val from = list.indexOfFirst { it.path == path }
        if (from < 0) return
        val to = (from + delta).coerceIn(0, list.size - 1)
        if (to == from) return
        val item = list.removeAt(from)
        list.add(to, item)
        savePins(c, list)
    }

    /** Drops pins whose file no longer exists, and refreshes renamed paths out of the list. */
    fun prune(c: Context) {
        val alive = pins(c).filter { java.io.File(it.path).exists() }
        if (alive.size != pins(c).size) savePins(c, alive)

        // A note deleted on the PC leaves a lock behind that guards nothing.
        val locks = lockedPaths(c)
        val liveLocks = locks.filter { java.io.File(it).exists() }.toSet()
        if (liveLocks.size != locks.size) {
            prefs(c).edit().putStringSet(KEY_LOCKED, liveLocks).apply()
        }
        defaultDir(c)?.let { if (!java.io.File(it).isDirectory) setDefaultDir(c, null) }
    }

    /**
     * Follows a rename or move everywhere a path is stored: pins, locks, and
     * the default-folder setting. Children are carried too, so moving a folder
     * does not silently unlock the notes inside it.
     */
    fun repath(c: Context, oldPath: String, newPath: String) {
        val oldPrefix = oldPath.trimEnd('/') + "/"
        fun moved(p: String): String = when {
            p == oldPath -> newPath
            p.startsWith(oldPrefix) -> newPath.trimEnd('/') + "/" + p.removePrefix(oldPrefix)
            else -> p
        }
        savePins(c, pins(c).map { it.copy(path = moved(it.path)) })
        prefs(c).edit()
            .putStringSet(KEY_LOCKED, lockedPaths(c).map { moved(it) }.toSet())
            .apply()
        defaultDir(c)?.let { d -> if (moved(d) != d) setDefaultDir(c, moved(d)) }
    }

    /** Forgets every stored reference to a path and its children. */
    fun forgetPath(c: Context, path: String) {
        val prefix = path.trimEnd('/') + "/"
        fun hit(p: String) = p == path || p.startsWith(prefix)
        savePins(c, pins(c).filterNot { hit(it.path) })
        prefs(c).edit()
            .putStringSet(KEY_LOCKED, lockedPaths(c).filterNot { hit(it) }.toSet())
            .apply()
        defaultDir(c)?.let { if (hit(it)) setDefaultDir(c, null) }
    }
}
