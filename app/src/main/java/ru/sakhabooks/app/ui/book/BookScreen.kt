package ru.sakhabooks.app.ui.book

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import ru.sakhabooks.app.R
import ru.sakhabooks.app.data.Book
import ru.sakhabooks.app.data.pick
import ru.sakhabooks.app.ui.catalog.BookCover
import ru.sakhabooks.app.ui.components.AssetImage
import ru.sakhabooks.app.ui.components.FavoriteButton
import ru.sakhabooks.app.ui.components.OrnamentBand

private enum class TextMode { Sah, Ru, Both }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookScreen(
    onBack: () -> Unit,
    viewModel: BookViewModel = hiltViewModel(),
) {
    val book = viewModel.book
    val chapter = viewModel.chapter
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    var textMode by rememberSaveable {
        mutableStateOf(TextMode.entries.firstOrNull { it.name == viewModel.savedTextMode } ?: TextMode.Both)
    }
    LaunchedEffect(textMode) { viewModel.saveTextMode(textMode.name) }

    // Android 13+: один раз спрашиваем разрешение на уведомления при первом «Слушать»
    val context = LocalContext.current
    var notificationsAllowed by remember { mutableStateOf(notificationsGranted(context)) }
    var askedForNotifications by rememberSaveable { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted
        viewModel.togglePlay()
    }
    val onPlayPause = {
        if (!state.isPlaying && !notificationsAllowed && !askedForNotifications &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            askedForNotifications = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.togglePlay()
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(book?.titleSah ?: "", style = MaterialTheme.typography.headlineSmall) },
            actions = {
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

        // Плеер — компактно сверху, чтобы книге осталось больше места
        if (viewModel.hasAudio) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatTime(state.positionMs), style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = state.positionMs.toFloat(),
                        onValueChange = { viewModel.seekTo(it.toLong()) },
                        valueRange = 0f..state.durationMs.coerceAtLeast(1L).toFloat(),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    Text(formatTime(state.durationMs), style = MaterialTheme.typography.labelMedium)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { viewModel.seekBy(-SEEK_MS) }) {
                        Icon(Icons.Filled.FastRewind, contentDescription = stringResource(R.string.player_back_15))
                    }
                    Spacer(Modifier.width(12.dp))
                    FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(52.dp)) {
                        Icon(
                            if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(if (state.isPlaying) R.string.player_pause else R.string.player_play),
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    IconButton(onClick = { viewModel.seekBy(SEEK_MS) }) {
                        Icon(Icons.Filled.FastForward, contentDescription = stringResource(R.string.player_forward_15))
                    }
                    Spacer(Modifier.width(12.dp))
                    // Значок скорости и само число; полное название — для экранного диктора
                    val speedLabel = stringResource(R.string.player_speed, formatSpeed(state.speed))
                    TextButton(
                        onClick = viewModel::cycleSpeed,
                        modifier = Modifier.semantics { contentDescription = speedLabel },
                    ) {
                        Icon(Icons.Outlined.Speed, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(formatSpeed(state.speed))
                    }
                }
            }
            HorizontalDivider()
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

private const val SEEK_MS = 15_000L

private fun notificationsGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    return String.format(Locale.ROOT, "%d:%02d", total / 60, total % 60)
}

private fun formatSpeed(speed: Float): String =
    if (speed % 1f == 0f) "${speed.toInt()}×" else "${speed}×".replace('.', ',')
