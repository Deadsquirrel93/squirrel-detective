package com.packagespy.app.core.localization

/**
 * Languages the app ships translations for. Kept as top-level `const val`-backed
 * [LocaleManager.ENGLISH]/[LocaleManager.RUSSIAN] so this file has no dependency on Android
 * runtime and can be exercised by plain JVM unit tests.
 */
val SUPPORTED_LANGUAGES: Set<String> = setOf(LocaleManager.ENGLISH, LocaleManager.RUSSIAN)

/**
 * Normalizes a BCP 47 language tag list (as returned by e.g.
 * `AppCompatDelegate.getApplicationLocales().toLanguageTags()`) to one of
 * [SUPPORTED_LANGUAGES], or `null` if the first tag's language is not supported.
 *
 * Only the first tag (before the first `,`) is considered; the language subtag is the part
 * before the first `-` or `_`, compared case-insensitively.
 */
fun normalizeLanguageTag(tags: String): String? {
    val language = tags
        .substringBefore(',')
        .trim()
        .substringBefore('-')
        .substringBefore('_')
        .lowercase()
    return language.takeIf { it in SUPPORTED_LANGUAGES }
}
