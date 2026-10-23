package ru.sakhabooks.app.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.sakhabooks.app.R
import ru.sakhabooks.app.data.Book
import ru.sakhabooks.app.data.CatalogRepository
import ru.sakhabooks.app.ui.components.AssetImage
import ru.sakhabooks.app.ui.components.OrnamentBand
import ru.sakhabooks.app.ui.components.TabScreen
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val repository: CatalogRepository,
) : ViewModel() {
    val books: List<Book> = repository.books()

    fun coverPath(book: Book): String? = repository.coverPath(book)
}

@Composable
fun CatalogScreen(
    onOpenBook: (String) -> Unit,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    TabScreen(title = stringResource(R.string.catalog_title)) {
        Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        if (viewModel.books.isEmpty()) {
            Text(stringResource(R.string.catalog_placeholder), style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(viewModel.books, key = { it.id }) { book ->
                    BookCard(book = book, coverPath = viewModel.coverPath(book), onClick = { onOpenBook(book.id) })
                }
            }
        }
    }
}

@Composable
private fun BookCard(book: Book, coverPath: String?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(book, coverPath)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(book.titleSah, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(book.author, style = MaterialTheme.typography.bodyMedium)
                if (book.descriptionRu.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(book.descriptionRu, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (book.isDemo) Badge(stringResource(R.string.badge_demo))
                    if (book.isFree) Badge(stringResource(R.string.badge_free))
                }
            }
        }
    }
}

/** Обложка книги. Если иллюстрации нет — временная: первая буква и полоса орнамента. */
@Composable
fun BookCover(book: Book, coverPath: String?, modifier: Modifier = Modifier, sizeDp: Int = 88) {
    val shape = Modifier
        .size(width = sizeDp.dp, height = (sizeDp * 1.3f).dp)
        .clip(RoundedCornerShape(8.dp))
    if (coverPath == null) {
        LetterCover(book, modifier.then(shape))
    } else {
        AssetImage(
            path = coverPath,
            contentDescription = book.titleSah,
            modifier = modifier.then(shape),
            // Иллюстрации горизонтальные — в узкой карточке сдвигаем кадр чуть вправо, к героям
            alignment = BiasAlignment(0.3f, 0f),
            placeholder = { LetterCover(book, modifier.then(shape)) },
        )
    }
}

@Composable
private fun LetterCover(book: Book, modifier: Modifier) {
    Box(
        modifier.background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            book.titleSah.take(1),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        OrnamentBand(
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 6.dp, vertical = 8.dp),
            height = 8.dp,
        )
    }
}

@Composable
private fun Badge(text: String) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}
