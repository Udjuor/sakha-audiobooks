package ru.sakhabooks.app.ui.book

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.sakhabooks.app.data.Book
import ru.sakhabooks.app.data.CatalogRepository
import ru.sakhabooks.app.data.Chapter
import ru.sakhabooks.app.data.FavoritesStore
import ru.sakhabooks.app.data.ReadingStore
import ru.sakhabooks.app.data.pick
import ru.sakhabooks.app.player.PlayerConnection
import ru.sakhabooks.app.ui.navigation.BookRoute
import javax.inject.Inject

/** Экран книги — чтение. Звук играет в общем плеере (вкладка «Плеер»); отсюда его можно только включить. */
@HiltViewModel
class BookViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: CatalogRepository,
    private val player: PlayerConnection,
    private val favorites: FavoritesStore,
    private val reading: ReadingStore,
) : ViewModel() {

    val book: Book? = repository.book(savedStateHandle.toRoute<BookRoute>().bookId)
    val chapter: Chapter? = book?.chapters?.firstOrNull()
    val coverPath: String? = book?.let(repository::coverPath)
    val hasAudio: Boolean = chapter?.let(repository::audioUri) != null

    /** Для страницы «аннотация»: раздел каталога и длительность записи. */
    val sectionTitle: String? = book?.section?.let { repository.section(it)?.title?.pick() }
    val audioDurationSec: Int = if (hasAudio) chapter?.durationSec ?: 0 else 0

    val isFavorite: StateFlow<Boolean> = favorites.ids
        .map { book?.id in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), book?.id in favorites.ids.value)

    /** Закладка: место в тексте, с которого открыть книгу. */
    val bookmarkOffset: Int? = chapter?.let { reading.offset(it.id) }

    /** Режим текста (якутский / перевод / оба), выбранный для этой книги в прошлый раз. */
    val savedTextMode: String? = chapter?.let { reading.textMode(it.id) }

    init {
        if (hasAudio) player.connect()
    }

    /** Включить эту сказку (если уже играет — просто продолжить). */
    fun listen() {
        chapter?.let { player.play(it.id) }
    }

    fun saveBookmark(offset: Int?) {
        chapter?.let { reading.saveOffset(it.id, offset) }
    }

    fun saveTextMode(mode: String) {
        chapter?.let { reading.saveTextMode(it.id, mode) }
    }

    fun toggleFavorite() {
        book?.let { favorites.toggle(it.id) }
    }
}
