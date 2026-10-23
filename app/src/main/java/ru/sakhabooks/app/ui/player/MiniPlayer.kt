package ru.sakhabooks.app.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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

data class MiniPlayerState(
    val book: Book,
    val chapterId: String,
    val coverPath: String?,
    val isPlaying: Boolean,
    val progress: Float,
)

@HiltViewModel
class MiniPlayerViewModel @Inject constructor(
    private val repository: CatalogRepository,
    private val player: PlayerConnection,
) : ViewModel() {

    /** null — в плеере ничего нет, мини-плеер скрыт. */
    val state: StateFlow<MiniPlayerState?> = player.state
        .map(::toState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), toState(player.state.value))

    init {
        // Если звук уже играет (например, приложение открыли из уведомления) — сразу покажем мини-плеер
        player.connect()
    }

    private fun toState(now: NowPlaying): MiniPlayerState? {
        val chapterId = now.chapterId ?: return null
        val book = repository.bookOfChapter(chapterId) ?: return null
        return MiniPlayerState(
            book = book,
            chapterId = chapterId,
            coverPath = repository.coverPath(book),
            isPlaying = now.isPlaying,
            progress = if (now.durationMs > 0) now.positionMs.toFloat() / now.durationMs else 0f,
        )
    }

    fun togglePlay() {
        val s = state.value ?: return
        if (s.isPlaying) player.pause() else player.play(s.chapterId)
    }
}

/** Полоска «сейчас играет» над нижним меню: обложка, название, пауза/играть. Нажатие — вкладка «Плеер». */
@Composable
fun MiniPlayer(
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MiniPlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state ?: return
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onOpenPlayer),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
    ) {
        Column {
            LinearProgressIndicator(
                progress = { s.progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondary,
            )
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BookCover(s.book, s.coverPath, Modifier.clip(RoundedCornerShape(6.dp)), sizeDp = 36)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        s.book.titleSah,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        s.book.author.pick(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = viewModel::togglePlay) {
                    Icon(
                        if (s.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(if (s.isPlaying) R.string.player_pause else R.string.player_play),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}
