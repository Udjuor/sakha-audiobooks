package ru.sakhabooks.app.player

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import ru.sakhabooks.app.MainActivity
import ru.sakhabooks.app.R
import ru.sakhabooks.app.data.Book
import ru.sakhabooks.app.data.CatalogRepository
import ru.sakhabooks.app.data.ProgressStore
import ru.sakhabooks.app.data.pick
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Служба плеера: один плеер на всё приложение.
 * Живёт отдельно от экранов, поэтому звук не прерывается при выключенном экране,
 * на других вкладках и в свёрнутом приложении. Через неё же работают
 * уведомление в шторке, экран блокировки и кнопки гарнитуры.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var repository: CatalogRepository
    @Inject lateinit var progress: ProgressStore

    private var session: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())

    /** Пока играет — запоминаем место раз в несколько секунд (на случай, если телефон выключится). */
    private val saveTicker = object : Runnable {
        override fun run() {
            saveProgress()
            handler.postDelayed(this, SAVE_EVERY_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            // Аудиофокус: пауза при звонке и когда звук берёт другое приложение
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            // Пауза, если отключили наушники
            .setHandleAudioBecomingNoisy(true)
            // Не даём телефону «уснуть» посреди главы при выключенном экране
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setSeekBackIncrementMs(SEEK_MS)
            .setSeekForwardIncrementMs(SEEK_MS)
            .build()
        player.setPlaybackSpeed(progress.speed())
        player.addListener(playerListener)

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelName(R.string.notification_channel_name)
                .build()
                .apply { setSmallIcon(R.drawable.ic_notification) },
        )

        session = MediaSession.Builder(this, player)
            .setCallback(SessionCallback())
            .setSessionActivity(openAppIntent())
            .setMediaButtonPreferences(seekButtons())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        saveProgress()
        val player = session?.player
        // Приложение смахнули из списка недавних: если звук не играет — службу закрываем
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        saveProgress()
        handler.removeCallbacks(saveTicker)
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            handler.removeCallbacks(saveTicker)
            if (isPlaying) handler.postDelayed(saveTicker, SAVE_EVERY_MS) else saveProgress()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                // Дослушали до конца — в следующий раз начнём сначала
                session?.player?.currentMediaItem?.mediaId?.let(progress::markFinished)
            }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            progress.saveSpeed(playbackParameters.speed)
        }
    }

    private fun saveProgress() {
        val player = session?.player ?: return
        val chapterId = player.currentMediaItem?.mediaId ?: return
        if (player.playbackState == Player.STATE_ENDED) return
        val duration = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        progress.save(chapterId, player.currentPosition, duration)
    }

    /** Кнопки «назад/вперёд на 15 секунд» в уведомлении и на экране блокировки. */
    private fun seekButtons(): List<CommandButton> = listOf(
        CommandButton.Builder(CommandButton.ICON_SKIP_BACK_15)
            .setPlayerCommand(Player.COMMAND_SEEK_BACK)
            .setDisplayName(getString(R.string.player_back_15))
            .setSlots(CommandButton.SLOT_BACK)
            .build(),
        CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_15)
            .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
            .setDisplayName(getString(R.string.player_forward_15))
            .setSlots(CommandButton.SLOT_FORWARD)
            .build(),
    )

    /** Нажатие на уведомление открывает приложение на экране играющей книги. */
    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_NOW_PLAYING, true)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private inner class SessionCallback : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            // «Следующий/предыдущий трек» не нужны — вместо них кнопки ±15 секунд
            val commands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .remove(Player.COMMAND_SEEK_TO_NEXT)
                .remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(commands)
                .build()
        }

        // Экран присылает только номер главы — адрес файла, название и обложку подставляет служба
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> =
            Futures.immediateFuture(mediaItems.mapNotNull(::resolve).toMutableList())

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val resolved = mediaItems.mapNotNull(::resolve)
            // Продолжаем с места, где остановились
            val start = if (startPositionMs == C.TIME_UNSET && resolved.isNotEmpty()) {
                progress.position(resolved.first().mediaId)
            } else {
                startPositionMs
            }
            return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(resolved, 0, start))
        }
    }

    private fun resolve(item: MediaItem): MediaItem? {
        val chapter = repository.chapter(item.mediaId) ?: return null
        val book = repository.bookOfChapter(item.mediaId) ?: return null
        val uri = repository.audioUri(chapter) ?: return null
        val metadata = MediaMetadata.Builder()
            .setTitle(book.titleSah)
            .setDisplayTitle(book.titleSah)
            .setArtist(book.author.pick())
            .setAlbumTitle(book.title.pick())
            .setArtworkData(artwork(book), MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .build()
        return MediaItem.Builder()
            .setMediaId(chapter.id)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }

    /** Обложка для уведомления — уменьшенная копия, чтобы не гонять большие картинки. */
    private fun artwork(book: Book): ByteArray? {
        val path = repository.coverPath(book) ?: return null
        return runCatching {
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            val bitmap = assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }
                ?: return null
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                out.toByteArray()
            }
        }.getOrNull()
    }

    private companion object {
        const val SEEK_MS = 15_000L
        const val SAVE_EVERY_MS = 5_000L
    }
}
