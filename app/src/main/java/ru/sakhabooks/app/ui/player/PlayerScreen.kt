package ru.sakhabooks.app.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.sakhabooks.app.R
import ru.sakhabooks.app.data.Book
import ru.sakhabooks.app.data.CatalogRepository
import ru.sakhabooks.app.data.pick
import ru.sakhabooks.app.player.NowPlaying
import ru.sakhabooks.app.player.PlayerConnection
import ru.sakhabooks.app.ui.catalog.BookCover
import ru.sakhabooks.app.ui.components.AssetImage
import ru.sakhabooks.app.ui.components.TabScreen
import java.util.Locale
import javax.inject.Inject

/** Что показывает вкладка «Плеер». book == null — ничего не играет. */
data class PlayerUiState(
    val book: Book? = null,
    val chapterId: String? = null,
    val coverPath: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
)

/** Книга с озвучкой — для списка «что послушать», когда ничего не играет. */
data class AudioBook(val book: Book, val chapterId: String, val coverPath: String?)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: CatalogRepository,
    private val player: PlayerConnection,
) : ViewModel() {

    val state: StateFlow<PlayerUiState> = player.state
        .map(::toUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), toUiState(player.state.value))

    val audioBooks: List<AudioBook> = repository.books().mapNotNull { book ->
        book.chapters.firstOrNull { repository.audioUri(it) != null }
            ?.let { AudioBook(book, it.id, repository.coverPath(book)) }
    }

    init {
        player.connect()
    }

    private fun toUiState(now: NowPlaying): PlayerUiState {
        val chapterId = now.chapterId ?: return PlayerUiState(speed = now.speed)
        val book = repository.bookOfChapter(chapterId) ?: return PlayerUiState(speed = now.speed)
        val fallback = (repository.chapter(chapterId)?.durationSec ?: 0) * 1000L
        return PlayerUiState(
            book = book,
            chapterId = chapterId,
            coverPath = repository.coverPath(book),
            isPlaying = now.isPlaying,
            positionMs = now.positionMs,
            durationMs = now.durationMs.takeIf { it > 0 } ?: fallback,
            speed = now.speed,
        )
    }

    fun play(chapterId: String) = player.play(chapterId)

    fun togglePlay() {
        val s = state.value
        val chapterId = s.chapterId ?: return
        if (s.isPlaying) player.pause() else player.play(chapterId)
    }

    fun seekBy(deltaMs: Long) {
        val s = state.value
        s.chapterId?.let { player.seekBy(it, s.positionMs, deltaMs) }
    }

    fun seekTo(positionMs: Long) {
        state.value.chapterId?.let { player.seekTo(it, positionMs) }
    }

    fun cycleSpeed() {
        val next = SPEEDS[(SPEEDS.indexOf(state.value.speed) + 1) % SPEEDS.size]
        player.setSpeed(next)
    }

    private companion object {
        val SPEEDS = listOf(1f, 1.25f, 1.5f, 2f, 0.75f)
    }
}

/** Вкладка «Плеер»: всё про то, что сейчас звучит. */
@Composable
fun PlayerScreen(
    onOpenBook: (String) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val book = state.book
    TabScreen(title = stringResource(R.string.player_title)) {
        if (book == null) {
            NothingPlaying(viewModel.audioBooks, onPlay = viewModel::play)
        } else {
            NowPlayingPanel(state, book, viewModel, onOpenBook)
        }
    }
}

@Composable
private fun NowPlayingPanel(
    state: PlayerUiState,
    book: Book,
    viewModel: PlayerViewModel,
    onOpenBook: (String) -> Unit,
) {
    val onPlayPause = rememberPlayWithPermission(viewModel::togglePlay)
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val coverModifier = Modifier.fillMaxWidth().widthIn(max = 360.dp).aspectRatio(1f).clip(RoundedCornerShape(16.dp))
        if (state.coverPath != null) {
            AssetImage(state.coverPath, contentDescription = book.titleSah, modifier = coverModifier)
        } else {
            BookCover(book, coverPath = null, sizeDp = 200)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            book.titleSah,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            book.title.pick(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(book.author.pick(), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))

        Slider(
            value = state.positionMs.toFloat(),
            onValueChange = { viewModel.seekTo(it.toLong()) },
            valueRange = 0f..state.durationMs.coerceAtLeast(1L).toFloat(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(state.positionMs), style = MaterialTheme.typography.labelMedium)
            Text(formatTime(state.durationMs), style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.seekBy(-SEEK_MS) }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.FastRewind, stringResource(R.string.player_back_15), Modifier.size(36.dp))
            }
            Spacer(Modifier.width(20.dp))
            FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(80.dp)) {
                Icon(
                    if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(if (state.isPlaying) R.string.player_pause else R.string.player_play),
                    modifier = Modifier.size(44.dp),
                )
            }
            Spacer(Modifier.width(20.dp))
            IconButton(onClick = { viewModel.seekBy(SEEK_MS) }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.FastForward, stringResource(R.string.player_forward_15), Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Значок скорости и само число; полное название — для экранного диктора
            val speedLabel = stringResource(R.string.player_speed, formatSpeed(state.speed))
            TextButton(onClick = viewModel::cycleSpeed, modifier = Modifier.semantics { contentDescription = speedLabel }) {
                Icon(Icons.Outlined.Speed, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(formatSpeed(state.speed))
            }
            Spacer(Modifier.width(16.dp))
            FilledTonalIconButton(onClick = { onOpenBook(book.id) }) {
                Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = stringResource(R.string.player_read_text))
            }
        }
    }
}

/** Ничего не играет — предлагаем сказки с озвучкой. */
@Composable
private fun NothingPlaying(audioBooks: List<AudioBook>, onPlay: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                stringResource(R.string.player_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(audioBooks, key = { it.book.id }) { item ->
            val play = rememberPlayWithPermission { onPlay(item.chapterId) }
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = play),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    BookCover(item.book, item.coverPath, sizeDp = 56)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.book.titleSah, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(item.book.author.pick(), style = MaterialTheme.typography.bodySmall)
                    }
                    FilledTonalIconButton(onClick = play) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.player_play))
                    }
                }
            }
        }
    }
}

private const val SEEK_MS = 15_000L

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    return String.format(Locale.ROOT, "%d:%02d", total / 60, total % 60)
}

private fun formatSpeed(speed: Float): String =
    if (speed % 1f == 0f) "${speed.toInt()}×" else "${speed}×".replace('.', ',')
