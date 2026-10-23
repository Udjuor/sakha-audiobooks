package ru.sakhabooks.app.ui.book

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.sakhabooks.app.data.Book
import ru.sakhabooks.app.data.CatalogRepository
import ru.sakhabooks.app.data.Chapter
import ru.sakhabooks.app.data.ProgressStore
import ru.sakhabooks.app.ui.navigation.BookRoute
import javax.inject.Inject

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
)

@HiltViewModel
class BookViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: CatalogRepository,
    private val progress: ProgressStore,
    @ApplicationContext context: Context,
) : ViewModel() {

    val book: Book? = repository.book(savedStateHandle.toRoute<BookRoute>().bookId)
    val chapter: Chapter? = book?.chapters?.firstOrNull()

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val player: ExoPlayer = ExoPlayer.Builder(context).build()

    init {
        chapter?.let { ch ->
            player.setMediaItem(MediaItem.fromUri(repository.audioUri(ch)))
            player.prepare()
            val saved = progress.position(ch.id)
            if (saved > 0) player.seekTo(saved)
        }
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (!isPlaying) savePosition()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    // Дослушали до конца — в следующий раз начнём сначала.
                    chapter?.let { progress.save(it.id, 0L) }
                }
                refresh()
            }
        })
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(250)
            }
        }
    }

    private fun refresh() {
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        _state.update { it.copy(positionMs = player.currentPosition.coerceAtLeast(0L), durationMs = duration) }
    }

    fun togglePlay() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            player.play()
        }
    }

    fun seekBy(deltaMs: Long) {
        val target = (player.currentPosition + deltaMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
        refresh()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        refresh()
    }

    fun cycleSpeed() {
        val next = SPEEDS[(SPEEDS.indexOf(_state.value.speed) + 1) % SPEEDS.size]
        player.setPlaybackSpeed(next)
        _state.update { it.copy(speed = next) }
    }

    private fun savePosition() {
        val ch = chapter ?: return
        if (player.playbackState != Player.STATE_ENDED) progress.save(ch.id, player.currentPosition)
    }

    override fun onCleared() {
        savePosition()
        player.release()
    }

    private companion object {
        val SPEEDS = listOf(1f, 1.25f, 1.5f, 2f, 0.75f)
    }
}
