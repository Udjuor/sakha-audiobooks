package ru.sakhabooks.app.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import ru.sakhabooks.app.R
import ru.sakhabooks.app.data.Book
import ru.sakhabooks.app.data.CatalogRepository
import ru.sakhabooks.app.data.FavoritesStore
import ru.sakhabooks.app.data.pick
import ru.sakhabooks.app.ui.components.FavoriteButton
import ru.sakhabooks.app.ui.components.OrnamentBand
import ru.sakhabooks.app.ui.navigation.SectionRoute
import javax.inject.Inject

@HiltViewModel
class SectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CatalogRepository,
    private val favorites: FavoritesStore,
) : ViewModel() {
    private val sectionId = savedStateHandle.toRoute<SectionRoute>().sectionId
    val title: String = repository.section(sectionId)?.title?.pick().orEmpty()
    val books: List<Book> = repository.booksIn(sectionId)
    val favoriteIds: StateFlow<Set<String>> = favorites.ids

    fun coverPath(book: Book): String? = repository.coverPath(book)

    fun hasAudio(book: Book): Boolean = book.chapters.any { repository.audioUri(it) != null }

    fun toggleFavorite(bookId: String) = favorites.toggle(bookId)
}

/** Раздел каталога: компактный список книг. Нажали на книгу — вошли в неё. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionScreen(
    onBack: () -> Unit,
    onOpenBook: (String) -> Unit,
    viewModel: SectionViewModel = hiltViewModel(),
) {
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(viewModel.title, style = MaterialTheme.typography.headlineSmall) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
        )
        OrnamentBand(Modifier.padding(horizontal = 16.dp))
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            items(viewModel.books, key = { it.id }) { book ->
                BookRow(
                    book = book,
                    coverPath = viewModel.coverPath(book),
                    hasAudio = viewModel.hasAudio(book),
                    isFavorite = book.id in favoriteIds,
                    onToggleFavorite = { viewModel.toggleFavorite(book.id) },
                    onClick = { onOpenBook(book.id) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun BookRow(
    book: Book,
    coverPath: String?,
    hasAudio: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(book, coverPath, sizeDp = 48)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                book.titleSah,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                book.title.pick(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (hasAudio) {
                    Icon(
                        Icons.Outlined.Headphones,
                        contentDescription = stringResource(R.string.info_has_audio),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (book.isFree) Badge(stringResource(R.string.badge_free))
                if (book.isDemo) Badge(stringResource(R.string.badge_demo))
            }
        }
        FavoriteButton(isFavorite, onToggleFavorite)
    }
}

@Composable
private fun Badge(text: String) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 1.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}
