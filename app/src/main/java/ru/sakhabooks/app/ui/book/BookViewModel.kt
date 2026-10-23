package ru.sakhabooks.app.ui.book

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.sakhabooks.app.data.Book
import ru.sakhabooks.app.data.CatalogRepository
import ru.sakhabooks.app.data.Chapter
import ru.sakhabooks.app.data.FavoritesStore
import ru.sakhabooks.app.data.ProgressStore
import ru.sakhabooks.app.data.ReadingStore
import ru.sakhabooks.app.player.NowPlaying
import ru.sakhabooks.app.player.PlayerConnection
import ru.sakhabooks.app.ui.navigation.BookRoute

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
)

/** Экран книги — «пульт» общего плеера: сам звук играет в службе и не прерывается при уходе с экрана. */
@HiltViewModel
class BookViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: CatalogRepository,
    private val progress: ProgressStore,
    private val player: PlayerConnection,
    private val favorites: FavoritesStore,
    private val reading: ReadingStore,
) : ViewModel() {

    val book: Book? = repository.book(savedStateHandle.toRoute<BookRoute>().bookId)
    val chapter: Chapter? = book?.chapters?.firstOrNull()
    val coverPath: String? = book?.let(repository::coverPath)
    val hasAudio: Boolean = chapter?.let(repository::audioUri) != null

    val state: StateFlow<PlayerUiState> = player.state
        .map(::toUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), toUiState(player.state.value))

    val isFavorite: StateFlow<Boolean> = favorites.ids
        .map { book?.id in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), book?.id in favorites.ids.value)

    init {
        if (hasAudio) player.connect()
    }

    /** Закладка: место в тексте, с которого открыть книгу. */
    val bookmarkOffset: Int? = chapter?.let { reading.offset(it.id) }

    fun saveBookmark(offset: Int?) {
        chapter?.let { reading.saveOffset(it.id, offset) }
    }

    /** Режим текста (якутский / перевод / оба), выбранный для этой книги в прошлый раз. */
    val savedTextMode: String? = chapter?.let { reading.textMode(it.id) }

    fun saveTextMode(mode: String) {
        chapter?.let { reading.saveTextMode(it.id, mode) }
    }

    fun toggleFavorite() {
        book?.let { favorites.toggle(it.id) }
    }

    /** Если играет эта глава — показываем живое состояние плеера, иначе — сохранённое место. */
    private fun toUiState(now: NowPlaying): PlayerUiState {
        val ch = chapter ?: return PlayerUiState(speed = now.speed)
        val fallbackDuration = ch.durationSec * 1000L
        return if (now.chapterId == ch.id) {
            PlayerUiState(
                isPlaying = now.isPlaying,
                positionMs = now.positionMs,
                durationMs = now.durationMs.takeIf { it > 0 } ?: fallbackDuration,
                speed = now.speed,
            )
        } else {
            PlayerUiState(positionMs = progress.position(ch.id), durationMs = fallbackDuration, speed = now.speed)
        }
    }

    fun togglePlay() {
        val ch = chapter ?: return
        if (state.value.isPlaying) player.pause() else player.play(ch.id)
    }

    fun seekBy(deltaMs: Long) {
        val ch = chapter ?: return
        player.seekBy(ch.id, state.value.positionMs, deltaMs)
    }

    fun seekTo(positionMs: Long) {
        val ch = chapter ?: return
        player.seekTo(ch.id, positionMs)
    }

    fun cycleSpeed() {
        val next = SPEEDS[(SPEEDS.indexOf(state.value.speed) + 1) % SPEEDS.size]
        player.setSpeed(next)
    }

    private companion object {
        val SPEEDS = listOf(1f, 1.25f, 1.5f, 2f, 0.75f)
    }
}
