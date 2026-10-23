package ru.sakhabooks.app.data

import java.util.Locale

/**
 * Текст на нескольких языках: {"ru": "…", "en": "…", "sah": "…"}.
 * Приложение показывает язык телефона, а если такого перевода нет — ближайший запасной.
 */
typealias Localized = Map<String, String>

object ContentLanguage {
    const val SAKHA = "sah"
    const val RUSSIAN = "ru"
    const val ENGLISH = "en"

    /** Язык интерфейса: берётся из настроек телефона (или языка приложения на Android 13+). */
    fun current(): String = Locale.getDefault().language

    /** Порядок поиска перевода: язык телефона, потом запасные. Якутским читателям ближе русский. */
    fun preferred(language: String = current()): List<String> = when (language) {
        SAKHA, RUSSIAN -> listOf(language, RUSSIAN, ENGLISH, SAKHA)
        else -> listOf(language, ENGLISH, RUSSIAN, SAKHA)
    }.distinct()

    /** Язык перевода текста книги: якутский оригинал показываем всегда, перевод — на языке телефона. */
    fun translationLanguage(language: String = current()): String = when (language) {
        SAKHA, RUSSIAN -> RUSSIAN
        else -> ENGLISH
    }
}

/** Текст на языке телефона или ближайший запасной; пустая строка, если текста нет вообще. */
fun Localized.pick(): String =
    ContentLanguage.preferred().firstNotNullOfOrNull { this[it]?.takeIf(String::isNotBlank) } ?: ""
