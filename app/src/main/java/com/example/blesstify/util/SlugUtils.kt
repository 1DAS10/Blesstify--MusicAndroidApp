package com.example.blesstify.util

import java.text.Normalizer

object SlugUtils {
    /**
     * Deterministic id from free-form name.
     * - lower-case
     * - remove accents/diacritics
     * - keep letters/digits, convert others to '-'
     * - collapse multiple '-'
     */
    fun slugify(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        val normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

        val slug = buildString {
            var lastDash = false
            for (ch in normalized.lowercase()) {
                val isOk = ch.isLetterOrDigit()
                if (isOk) {
                    append(ch)
                    lastDash = false
                } else {
                    if (!lastDash) {
                        append('-')
                        lastDash = true
                    }
                }
            }
        }

        return slug.trim('-').replace("-+".toRegex(), "-")
    }
}
