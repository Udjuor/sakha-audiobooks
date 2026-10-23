package ru.sakhabooks.app.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.sakhabooks.app.R
import ru.sakhabooks.app.data.Book
import ru.sakhabooks.app.data.CatalogRepository
import ru.sakhabooks.app.data.CatalogSection
import ru.sakhabooks.app.data.pick
import ru.sakhabooks.app.ui.components.TabScreen
import javax.inject.Inject

/** Раздел каталога для экрана: сколько в нём книг и несколько обложек для узнаваемости. */
data class SectionItem(
    val section: CatalogSection,
    val bookCount: Int,
    val previews: List<Pair<Book, String?>>,
)

@HiltViewModel
class CatalogViewModel @Inject constructor(
    repository: CatalogRepository,
) : ViewModel() {
    val sections: List<SectionItem> = repository.sections().map { section ->
        val books = repository.booksIn(section.id)
        SectionItem(section, books.size, books.take(3).map { it to repository.coverPath(it) })
    }
}

/** Каталог: сначала разделы. Нажали на раздел — откроется список его книг. */
@Composable
fun CatalogScreen(
    onOpenSection: (String) -> Unit,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    TabScreen(title = stringResource(R.string.catalog_title)) {
        if (viewModel.sections.isEmpty()) {
            Text(stringResource(R.string.catalog_placeholder), style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(viewModel.sections, key = { it.section.id }) { item ->
                    SectionCard(item, onClick = { onOpenSection(item.section.id) })
                }
            }
        }
    }
}

@Composable
private fun SectionCard(item: SectionItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Обложки внахлёст — как книги на полке
            Box {
                item.previews.asReversed().forEachIndexed { reversedIndex, (book, cover) ->
                    val index = item.previews.size - 1 - reversedIndex
                    BookCover(book, cover, Modifier.padding(start = (20 * index).dp), sizeDp = 44)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(item.section.title.pick(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    pluralStringResource(R.plurals.catalog_books_count, item.bookCount, item.bookCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
