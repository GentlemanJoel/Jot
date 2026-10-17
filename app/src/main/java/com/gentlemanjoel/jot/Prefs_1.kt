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
    LABEL_NARROW("Narrow - with names", 170),
    LABEL_MEDIUM("Medium - with names", 210),
    LABEL_WIDE("Wide - with names", 260),
    LABEL_XWIDE("Extra wide - with names", 310);

    /** Too narrow for a readable name, so the row shows just the icon. */
    val iconsOnly: Boolean get() = widthDp < LABEL_FLOOR

    companion object {
        const val LABEL_FLOOR = 130
        val DEFAULT = LABEL_MEDIUM
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
    }

    /** Renames a pin's path in place so pinning survives a note rename. */
    fun repath(c: Context, oldPath: String, newPath: String) {
        savePins(c, pins(c).map { if (it.path == oldPath) it.copy(path = newPath) else it })
    }
}
