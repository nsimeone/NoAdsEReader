package com.example.noadse_reader

import android.content.Context

enum class PageTheme(val label: String) {
    LIGHT("Light"),
    SEPIA("Sepia"),
    DARK("Dark")
}

class ReaderSettings(context: Context) {
    private val prefs = context.getSharedPreferences("reader_settings", Context.MODE_PRIVATE)

    var fontSize: Double
        get() = prefs.getFloat("fontSize", 1.0f).toDouble()
        set(value) {
            prefs.edit().putFloat("fontSize", value.toFloat()).apply()
        }

    var theme: PageTheme?
        get() = prefs.getString("theme", null)?.let { name ->
            PageTheme.entries.firstOrNull { it.name == name }
        }
        set(value) {
            prefs.edit().putString("theme", value?.name).apply()
        }
}