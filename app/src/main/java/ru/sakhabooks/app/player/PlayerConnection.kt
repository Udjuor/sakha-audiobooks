package ru.sakhabooks.app.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.sakhabooks.app.data.ProgressStore
import javax.inject.Inject
import javax.inject.Singleton

/** Что сейчас загружено в плеер. chapterId == null — ничего не играет. */
data class NowPlaying(
    val chapterId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
)

/**
 * «Пульт» общего плеера для экранов. Сам звук играет в [PlaybackService],
 * а экраны только отправляют команды и показывают состояние.
 */
@Singleton
class PlayerConnection @Inject constructor(
    @ApplicationContext private val context: Context,
    progress: ProgressStore,
) {
    private val _state = MutableStateFlow(NowPlaying(speed = progress.speed()))
    val state: StateFlow<NowPlaying> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private var connecting = false
    private val pending = mutableListOf<(MediaController) -> Unit>()
    private var ticker: Job? = null

    /** Подключиться к службе: если звук уже играет, мини-плеер сразу это покажет. */
    fun connect() = withController { }

    fun play(chapterId: String) = withController { c ->
        when {
            c.currentMediaItem?.mediaId != chapterId -> load(c, chapterId)
            c.playbackState == Player.STATE_ENDED -> c.seekTo(0)
            c.playbackState == Player.STATE_IDLE -> c.prepare()
        }
        c.play()
    }

    fun pause() = withController { it.pause() }

    fun seekTo(chapterId: String, positionMs: Long) = withController { c ->
        if (c.currentMediaItem?.mediaId != chapterId) load(c, chapterId)
        c.seekTo(positionMs.coerceAtLeast(0L))
        update()
    }

    fun seekBy(chapterId: String, currentPositionMs: Long, deltaMs: Long) = withController { c ->
        if (c.currentMediaItem?.mediaId != chapterId) load(c, chapterId)
        val duration = c.duration.takeIf { it != C.TIME_UNSET } ?: Long.MAX_VALUE
        c.seekTo((currentPositionMs + deltaMs).coerceIn(0L, duration))
        update()
    }

    fun setSpeed(speed: Float) {
        _state.value = _state.value.copy(speed = speed)
        withController { it.setPlaybackSpeed(speed) }
    }

    private fun load(c: MediaController, chapterId: String) {
        // Служба сама подставит файл, название, обложку и место остановки
        c.setMediaItem(MediaItem.Builder().setMediaId(chapterId).build())
        c.prepare()
    }

    private fun withController(action: (MediaController) -> Unit) {
        controller?.let { action(it); return }
        pending += action
        if (connecting) return
        connecting = true
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            connecting = false
            val c = runCatching { future.get() }.getOrNull()
            if (c == null) {
                pending.clear()
                return@addListener
            }
            controller = c
            c.addListener(listener)
            update()
            val actions = pending.toList()
            pending.clear()
            actions.forEach { it(c) }
        }, ContextCompat.getMainExecutor(context))
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            update()
            if (player.isPlaying) startTicker() else stopTicker()
        }
    }

    private fun update() {
        val c = controller ?: return
        _state.value = NowPlaying(
            chapterId = c.currentMediaItem?.mediaId,
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L,
            speed = c.playbackParameters.speed,
        )
    }

    /** Пока играет — обновляем ползунок несколько раз в секунду. */
    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                update()
                delay(TICK_MS)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private companion object {
        const val TICK_MS = 250L
    }
}
