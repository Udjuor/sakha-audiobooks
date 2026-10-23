package ru.sakhabooks.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.sakhabooks.app.R
import ru.sakhabooks.app.data.Book
import ru.sakhabooks.app.data.CatalogRepository
import ru.sakhabooks.app.data.FavoritesStore
import ru.sakhabooks.app.data.ProgressStore
import ru.sakhabooks.app.player.PlayerConnection
import ru.sakhabooks.app.ui.catalog.BookCover
import ru.sakhabooks.app.ui.components.FavoriteButton
import ru.sakhabooks.app.ui.components.TabScreen
import javax.inject.Inject

/** Книга на полке: прогресс прослушивания и с какой главы продолжать. */
data class ShelfItem(
    val book: Book,
    val coverPath: String?,
    /** Глава, которую продолжать; null — у книги нет аудио. */
    val chapterId: String?,
    val progress: Float,
    val isFinished: Boolean,
    val isPlaying: Boolean,
    val isFavorite: Boolean,
)

data class LibraryUiState(
    val listening: List<ShelfItem> = emptyList(),
    val favorites: List<ShelfItem> = emptyList(),
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: CatalogRepository,
    private val progress: ProgressStore,
    private val favorites: FavoritesStore,
    private val player: PlayerConnection,
) : ViewModel() {

    private val playing = player.state.map { if (it.isPlaying) it.chapterId else null }.distinctUntilChanged()

    val state: StateFlow<LibraryUiState> =
        combine(progress.changes, favorites.ids, playing) { _, favoriteIds, playingChapter ->
            build(favoriteIds, playingChapter)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), build(favorites.ids.value, null))

    init {
        player.connect()
    }

    private fun build(favoriteIds: Set<String>, playingChapter: String?): LibraryUiState {
        val items = repository.books().associateWith { book -> shelfItem(book, favoriteIds, playingChapter) }
        // «Слушаю»: книги, которые уже начинали, — сначала недавние
        val listening = items.entries
            .map { (book, item) -> item to book.chapters.maxOfOrNull { progress.lastPlayedAt(it.id) }.orZero() }
            .filter { (_, lastPlayed) -> lastPlayed > 0 }
            .sortedByDescending { (_, lastPlayed) -> lastPlayed }
            .map { (item, _) -> item }
        val favorites = items.values.filter { it.isFavorite }
        return LibraryUiState(listening, favorites)
    }

    private fun shelfItem(book: Book, favoriteIds: Set<String>, playingChapter: String?): ShelfItem {
        val withAudio = book.chapters.filter { repository.audioUri(it) != null }
        // Продолжаем последнюю слушанную главу, иначе — первую
        val chapter = withAudio.maxByOrNull { progress.lastPlayedAt(it.id) }
            ?.takeIf { progress.lastPlayedAt(it.id) > 0 } ?: withAudio.firstOrNull()
        val finished = chapter != null && progress.isFinished(chapter.id)
        val fraction = chapter?.let {
            val duration = progress.duration(it.id).takeIf { d -> d > 0 } ?: (it.durationSec * 1000L)
            if (duration > 0) (progress.position(it.id).toFloat() / duration).coerceIn(0f, 1f) else 0f
        } ?: 0f
        return ShelfItem(
            book = book,
            coverPath = repository.coverPath(book),
            chapterId = chapter?.id,
            progress = if (finished) 1f else fraction,
            isFinished = finished,
            isPlaying = chapter != null && chapter.id == playingChapter,
            isFavorite = book.id in favoriteIds,
        )
    }

    fun togglePlay(item: ShelfItem) {
        val chapterId = item.chapterId ?: return
        if (item.isPlaying) player.pause() else player.play(chapterId)
    }

    fun toggleFavorite(item: ShelfItem) = favorites.toggle(item.book.id)

    private fun Long?.orZero(): Long = this ?: 0L
}

/** «Мин кинигэлэрим» — личная полка: начатые книги и избранное. */
@Composable
fun LibraryScreen(
    onOpenBook: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TabScreen(title = stringResource(R.string.library_title)) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ShelfHeader(stringResource(R.string.library_listening)) }
            if (state.listening.isEmpty()) {
                item { EmptyHint(stringResource(R.string.library_listening_empty)) }
            } else {
                items(state.listening, key = { "listening-" + it.book.id }) { item ->
                    ShelfRow(item, onOpenBook, viewModel::togglePlay, viewModel::toggleFavorite)
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
            item { ShelfHeader(stringResource(R.string.library_favorites)) }
            if (state.favorites.isEmpty()) {
                item { EmptyHint(stringResource(R.string.library_favorites_empty)) }
            } else {
                items(state.favorites, key = { "favorite-" + it.book.id }) { item ->
                    ShelfRow(item, onOpenBook, viewModel::togglePlay, viewModel::toggleFavorite)
                }
            }
        }
    }
}

@Composable
private fun ShelfHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ShelfRow(
    item: ShelfItem,
    onOpenBook: (String) -> Unit,
    onTogglePlay: (ShelfItem) -> Unit,
    onToggleFavorite: (ShelfItem) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpenBook(item.book.id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(item.book, item.coverPath, sizeDp = 56)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.book.titleSah,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(item.book.author, style = MaterialTheme.typography.bodySmall)
                if (item.chapterId != null) {
                    Spacer(Modifier.height(6.dp))
                    if (item.isFinished) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.library_finished), style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        LinearProgressIndicator(
                            progress = { item.progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FavoriteButton(item.isFavorite, { onToggleFavorite(item) })
                if (item.chapterId != null) {
                    FilledTonalIconButton(onClick = { onTogglePlay(item) }) {
                        Icon(
                            if (item.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(
                                if (item.isPlaying) R.string.player_pause else R.string.library_continue,
                            ),
                        )
                    }
                }
            }
        }
    }
}
