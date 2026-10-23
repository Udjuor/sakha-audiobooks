package ru.sakhabooks.app.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Источник книг. Сейчас читает встроенный демо-каталог из assets/demo,
 * поэтому демо-рассказ работает без интернета. Когда появится сервер,
 * сюда добавится загрузка по сети, а экраны менять не придётся.
 */
@Singleton
class CatalogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val catalog: CatalogFile by lazy {
        runCatching {
            context.assets.open(DEMO_CATALOG).bufferedReader().use { json.decodeFromString<CatalogFile>(it.readText()) }
        }.getOrElse { CatalogFile() }
    }

    fun books(): List<Book> = catalog.books

    fun book(id: String): Book? = catalog.books.firstOrNull { it.id == id }

    /** Книга, которой принадлежит глава (плеер знает только номер главы). */
    fun bookOfChapter(chapterId: String): Book? =
        catalog.books.firstOrNull { book -> book.chapters.any { it.id == chapterId } }

    fun chapter(chapterId: String): Chapter? =
        bookOfChapter(chapterId)?.chapters?.firstOrNull { it.id == chapterId }

    /** Адрес аудио для плеера: встроенный файл из assets. null — аудио ещё нет. */
    fun audioUri(chapter: Chapter): String? = chapter.audio?.let { "asset:///$DEMO_DIR/$it" }

    /** Путь к обложке внутри assets. null — обложки нет, покажем временную. */
    fun coverPath(book: Book): String? = book.cover?.let { "$DEMO_DIR/$it" }

    private companion object {
        const val DEMO_DIR = "demo"
        const val DEMO_CATALOG = "$DEMO_DIR/catalog.json"
    }
}
