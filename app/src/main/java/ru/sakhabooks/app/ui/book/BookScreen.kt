package ru.sakhabooks.app.ui.book

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.sakhabooks.app.R
import ru.sakhabooks.app.data.Book
import ru.sakhabooks.app.data.pick
import ru.sakhabooks.app.ui.catalog.BookCover
import ru.sakhabooks.app.ui.components.AssetImage
import ru.sakhabooks.app.ui.components.FavoriteButton
import ru.sakhabooks.app.ui.components.OrnamentBand
import ru.sakhabooks.app.ui.player.rememberPlayWithPermission

private enum class TextMode { Sah, Ru, Both }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookScreen(
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    viewModel: BookViewModel = hiltViewModel(),
) {
    val book = viewModel.book
    val chapter = viewModel.chapter
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    var textMode by rememberSaveable {
        mutableStateOf(TextMode.entries.firstOrNull { it.name == viewModel.savedTextMode } ?: TextMode.Both)
    }
    LaunchedEffect(textMode) { viewModel.saveTextMode(textMode.name) }

    // 🎧 — включить сказку и перейти во вкладку «Плеер»
    val listen = rememberPlayWithPermission {
        viewModel.listen()
        onOpenPlayer()
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(book?.titleSah ?: "", style = MaterialTheme.typography.headlineSmall) },
            actions = {
                if (book != null && viewModel.hasAudio) {
                    IconButton(onClick = listen) {
                        Icon(Icons.Outlined.Headphones, contentDescription = stringResource(R.string.player_play))
                    }
                }
                if (book != null) FavoriteButton(isFavorite, viewModel::toggleFavorite)
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
        )
        OrnamentBand(Modifier.padding(horizontal = 16.dp))

        if (book == null || chapter == null) {
            Text(stringResource(R.string.book_not_found), Modifier.padding(16.dp))
            return@Column
        }

        // Книга, которую листают: обложка, затем страницы текста
        val translation = chapter.translation()
        val hasTranslation = translation.isNotBlank()
        val translationColor = MaterialTheme.colorScheme.onSurfaceVariant
        val readerText = remember(chapter.textSah, translation, textMode, hasTranslation, translationColor) {
            buildAnnotatedString {
                val mode = if (hasTranslation) textMode else TextMode.Sah
                if (mode != TextMode.Ru) append(chapter.textSah)
                if (mode == TextMode.Both) append("\n\n")
                if (mode == TextMode.Ru) append(translation)
                if (mode == TextMode.Both) withStyle(SpanStyle(color = translationColor)) { append(translation) }
            }
        }
        PagedReader(
            text = readerText,
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            // Закладка действует для того режима текста, в котором её оставили
            initialOffset = viewModel.bookmarkOffset.takeIf { textMode.name == viewModel.savedTextMode || viewModel.savedTextMode == null },
            onPositionChanged = viewModel::saveBookmark,
            coverPage = {
                CoverPage(
                    book = book,
                    coverPath = viewModel.coverPath,
                    hasAudio = viewModel.hasAudio,
                    hasTranslation = hasTranslation,
                )
            },
            controls = {
                if (hasTranslation) {
                    // Режим текста — значками: оригинал, перевод, оба
                    TextModeButton(textMode == TextMode.Sah, Icons.Outlined.Article, R.string.text_sah) {
                        textMode = TextMode.Sah
                    }
                    TextModeButton(textMode == TextMode.Ru, Icons.Outlined.Translate, R.string.text_translation) {
                        textMode = TextMode.Ru
                    }
                    TextModeButton(textMode == TextMode.Both, Icons.Outlined.ViewAgenda, R.string.text_both) {
                        textMode = TextMode.Both
                    }
                }
            },
        )
    }
}

/** Первая страница книги: иллюстрация, название, описание. */
@Composable
private fun CoverPage(book: Book, coverPath: String?, hasAudio: Boolean, hasTranslation: Boolean) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (coverPath != null) {
            AssetImage(
                path = coverPath,
                contentDescription = book.titleSah,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                // Горизонтальная иллюстрация — целиком; вертикальная — квадратом
                aspectRatioFor = { it.coerceAtLeast(1f) },
            )
        } else {
            BookCover(book, coverPath = null, sizeDp = 96)
        }
        Spacer(Modifier.height(12.dp))
        Text(book.titleSah, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(book.title.pick(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(book.author.pick(), style = MaterialTheme.typography.bodyMedium)
        val description = book.description.pick()
        if (description.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
        val notes = listOfNotNull(
            R.string.audio_coming_soon.takeIf { !hasAudio },
            R.string.translation_coming_soon.takeIf { !hasTranslation },
        )
        notes.forEach {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.reader_swipe_hint),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun TextModeButton(selected: Boolean, icon: ImageVector, @StringRes label: Int, onClick: () -> Unit) {
    FilledTonalIconToggleButton(checked = selected, onCheckedChange = { onClick() }) {
        Icon(icon, contentDescription = stringResource(label))
    }
}
