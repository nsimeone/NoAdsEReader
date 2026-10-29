package com.example.noadse_reader

// The four highlight colors. The database stores the NAME (for example "YELLOW"),
// and this list turns that name into a real color when we draw it.
enum class HighlightColor(val label: String, val argb: Int) {
    YELLOW("Yellow", 0xFFFFEB3B.toInt()),
    GREEN("Green", 0xFF66BB6A.toInt()),
    BLUE("Blue", 0xFF42A5F5.toInt()),
    ORANGE("Orange", 0xFFFFA726.toInt());

    companion object {
        // If a saved name is ever unrecognized, fall back to yellow instead of crashing.
        fun fromName(name: String): HighlightColor =
            entries.firstOrNull { it.name == name } ?: YELLOW
    }
}