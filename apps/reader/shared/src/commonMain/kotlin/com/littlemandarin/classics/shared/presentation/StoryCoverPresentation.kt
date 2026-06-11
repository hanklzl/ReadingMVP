package com.littlemandarin.classics.shared.presentation

import com.littlemandarin.classics.shared.story.Story

/**
 * A deterministic, child-friendly cover theme derived from the story — used to replace
 * the single-character placeholder with a themed gradient + styled bilingual title until
 * real illustration art exists. The shared layer returns a locale-neutral palette index
 * and a short motif glyph; the platform maps the index to actual design-token colors.
 */
data class StoryCoverTheme(
    val storyId: String,
    val paletteIndex: Int,
    val motif: String,
    val level: Int,
)

class StoryCoverUseCases {

    /** Deterministic theme for a story: stable palette from the id, motif from a curated map. */
    fun coverThemeFor(story: Story, paletteCount: Int = DefaultPaletteCount): StoryCoverTheme {
        val palettes = paletteCount.coerceAtLeast(1)
        val paletteIndex = (stableHash(story.id) % palettes).let { if (it < 0) it + palettes else it }
        return StoryCoverTheme(
            storyId = story.id,
            paletteIndex = paletteIndex,
            motif = MOTIFS[story.id] ?: DEFAULT_MOTIF,
            level = story.level,
        )
    }

    // Small, stable, platform-independent hash (FNV-1a-ish) so the same id always maps to
    // the same palette across runs/platforms (kotlin's String.hashCode is stable for ASCII
    // ids but we keep our own to avoid relying on that contract).
    private fun stableHash(value: String): Int {
        var hash = 2166136261u
        for (char in value) {
            hash = hash xor char.code.toUInt()
            hash *= 16777619u
        }
        return (hash and 0x7FFFFFFFu).toInt()
    }

    private companion object {
        const val DefaultPaletteCount = 6
        const val DEFAULT_MOTIF = "📖"

        // Curated, age-appropriate motif per story id (no weapons/violence imagery).
        val MOTIFS: Map<String, String> = mapOf(
            "peach-garden-oath" to "🌸",
            "three-heroes-vs-lubu" to "🛡️",
            "quench-thirst-plums" to "🌳",
            "green-plum-heroes" to "🫛",
            "thousand-mile-loyalty" to "🧭",
            "three-visits-cottage" to "🏡",
            "zhaoyun-changban" to "🐎",
            "debate-scholars" to "💬",
            "borrow-arrows-boats" to "🛶",
            "borrow-east-wind" to "🍃",
            "red-cliffs" to "🌊",
            "huarong-path" to "🤝",
            "seven-captures" to "🌿",
            "empty-fort" to "🏯",
            "wooden-ox-flowing-horse" to "🐂",
        )
    }
}
