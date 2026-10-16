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

enum class PanelWidth(val label: String, val widthDp: Int) {
    NARROW("Narrow", 190),
    MEDIUM("Medium", 250),
    WIDE("Wide", 310),
}

object Prefs {

    private const val FILE = "jot_prefs"
    private const val KEY_PINS = "pins_json"
    private const val KEY_PANEL = "panel_width"
    private const val KEY_TOKEN = "github_token"

    /** Icon keys offered in the picker. Rendered by IconFor() in the UI layer. */
    val ICON_KEYS = listOf(
        "note", "check", "cart", "star", "flag", "bolt",
        "heart", "home", "work", "idea", "money", "calendar",
    )

    /** Color keys offered in the picker. Resolved by ColorFor() in the UI layer. */
    val COLOR_KEYS = listOf("blue", "green", "amber", "red", "purple", "teal", "slate")

    private fun prefs(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // ---------------- panel width ----------------

    fun panelWidth(c: Context): PanelWidth {
        val name = prefs(c).getString(KEY_PANEL, PanelWidth.MEDIUM.name)
        return runCatching { PanelWidth.valueOf(name!!) }.getOrDefault(PanelWidth.MEDIUM)
    }

    fun setPanelWidth(c: Context, w: PanelWidth) {
        prefs(c).edit().putString(KEY_PANEL, w.name).apply()
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
