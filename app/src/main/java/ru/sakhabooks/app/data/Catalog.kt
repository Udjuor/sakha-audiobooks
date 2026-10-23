package ru.sakhabooks.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Формат файла каталога. Позже тот же формат будет отдавать сервер. */
@Serializable
data class CatalogFile(
    val version: Int = 1,
    val books: List<Book> = emptyList(),
)

@Serializable
data class Book(
    val id: String,
    @SerialName("title_sah") val titleSah: String,
    @SerialName("title_ru") val titleRu: String,
    val author: String,
    @SerialName("description_ru") val descriptionRu: String = "",
    @SerialName("is_free") val isFree: Boolean = true,
    @SerialName("is_demo") val isDemo: Boolean = false,
    val cover: String? = null,
    val chapters: List<Chapter> = emptyList(),
)

@Serializable
data class Chapter(
    val id: String,
    val number: Int,
    val title: String,
    /** Имя аудиофайла внутри папки demo/ (позже — ссылка на сервер). null — аудио ещё не записано. */
    val audio: String? = null,
    @SerialName("duration_sec") val durationSec: Int = 0,
    @SerialName("text_sah") val textSah: String = "",
    @SerialName("text_ru") val textRu: String = "",
)
