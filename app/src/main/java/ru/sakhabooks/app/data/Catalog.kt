package ru.sakhabooks.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Формат файла каталога. Позже тот же формат будет отдавать сервер. */
@Serializable
data class CatalogFile(
    val version: Int = 2,
    val books: List<Book> = emptyList(),
)

@Serializable
data class Book(
    val id: String,
    /** Название по-якутски — показывается всегда. */
    @SerialName("title_sah") val titleSah: String,
    /** Название на других языках: {"ru": "…", "en": "…"}. */
    val title: Localized = emptyMap(),
    val author: Localized = emptyMap(),
    val description: Localized = emptyMap(),
    @SerialName("is_free") val isFree: Boolean = true,
    @SerialName("is_demo") val isDemo: Boolean = false,
    val cover: String? = null,
    val chapters: List<Chapter> = emptyList(),
)

@Serializable
data class Chapter(
    val id: String,
    val number: Int,
    val title: Localized = emptyMap(),
    /** Имя аудиофайла внутри папки demo/ (позже — ссылка на сервер). null — аудио ещё не записано. */
    val audio: String? = null,
    @SerialName("duration_sec") val durationSec: Int = 0,
    @SerialName("text_sah") val textSah: String = "",
    /** Переводы текста: {"ru": "…", "en": "…"}. */
    val translations: Localized = emptyMap(),
) {
    /** Перевод на языке телефона (русский — для якутского и русского, иначе английский). */
    fun translation(): String = translations[ContentLanguage.translationLanguage()].orEmpty()
}
