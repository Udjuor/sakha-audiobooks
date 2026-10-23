package ru.sakhabooks.app.ui.book

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import java.util.Locale

private enum class TextMode { Sah, Ru, Both }

/**
 * Книга — чистая читалка: на экране только страница. Стр. 1 — обложка и название,
 * стр. 2 — аннотация и сведения о произведении, дальше текст.
 * Касание середины страницы показывает меню (назад, 🎧, ♡, номер страницы).
 */
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
    var showControls by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showControls) { showControls = false }
    ImmersiveMode()

    // 🎧 — включить сказку и перейти во вкладку «Плеер»
    val listen = rememberPlayWithPermission {
        viewModel.listen()
        onOpenPlayer()
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (book == null || chapter == null) {
            Text(stringResource(R.string.book_not_found), Modifier.statusBarsPadding().padding(16.dp))
            return@Box
        }

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
            frontPages = listOf(
                { CoverPage(book, viewModel.coverPath) },
                {
                    AboutPage(
                        book = book,
                        sectionTitle = viewModel.sectionTitle,
                        audioDurationSec = viewModel.audioDurationSec,
                        hasTranslation = hasTranslation,
                    )
                },
            ),
            showControls = showControls,
            onToggleControls = { showControls = !showControls },
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout),
            // Закладка действует для того режима текста, в котором её оставили
            initialOffset = viewModel.bookmarkOffset.takeIf {
                textMode.name == viewModel.savedTextMode || viewModel.savedTextMode == null
            },
            onPositionChanged = viewModel::saveBookmark,
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

        // Верхняя часть меню — только по касанию середины страницы
        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Surface(tonalElevation = 3.dp, shadowElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Spacer(Modifier.weight(1f))
                    if (viewModel.hasAudio) {
                        IconButton(onClick = listen) {
                            Icon(Icons.Outlined.Headphones, contentDescription = stringResource(R.string.player_play))
                        }
                    }
                    FavoriteButton(isFavorite, viewModel::toggleFavorite)
                }
            }
        }
    }
}

/** Пока открыта книга — прячем строку состояния и кнопки телефона; вернутся при выходе. */
@Composable
private fun ImmersiveMode() {
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(window) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

/** Страница 1: обложка и название. */
@Composable
private fun CoverPage(book: Book, coverPath: String?) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (coverPath != null) {
            AssetImage(
                path = coverPath,
                contentDescription = book.titleSah,
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().weight(1f, fill = false).clip(RoundedCornerShape(12.dp)),
                aspectRatioFor = { it },
                contentScale = ContentScale.Fit,
            )
        } else {
            BookCover(book, coverPath = null, sizeDp = 160)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            book.titleSah,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            book.author.pick(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Страница 2: аннотация и сведения о произведении. */
@Composable
private fun AboutPage(book: Book, sectionTitle: String?, audioDurationSec: Int, hasTranslation: Boolean) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Text(stringResource(R.string.about_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        OrnamentBand(Modifier.padding(vertical = 12.dp), height = 10.dp)
        val description = book.description.pick()
        if (description.isNotBlank()) {
            Text(description, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(20.dp))
        }
        InfoRow(R.string.info_title, book.titleSah)
        book.title.pick().takeIf { it.isNotBlank() }?.let { InfoRow(R.string.info_title_translated, it) }
        InfoRow(R.string.info_author, book.author.pick())
        sectionTitle?.let { InfoRow(R.string.info_section, it) }
        InfoRow(
            R.string.info_recording,
            if (audioDurationSec > 0) formatDuration(audioDurationSec) else stringResource(R.string.info_not_yet),
        )
        InfoRow(
            R.string.info_translation,
            stringResource(if (hasTranslation) R.string.info_available else R.string.info_not_yet),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.reader_swipe_hint),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun InfoRow(@StringRes label: Int, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
    HorizontalDivider()
}

@Composable
private fun TextModeButton(selected: Boolean, icon: ImageVector, @StringRes label: Int, onClick: () -> Unit) {
    FilledTonalIconToggleButton(checked = selected, onCheckedChange = { onClick() }) {
        Icon(icon, contentDescription = stringResource(label))
    }
}

private fun formatDuration(seconds: Int): String =
    String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
