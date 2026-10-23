package ru.sakhabooks.app.ui.book

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.sakhabooks.app.R

/**
 * Чистая читалка: книга, которую листают. Сначала «передние» страницы (обложка, аннотация),
 * затем текст, нарезанный на страницы точно по размеру экрана.
 * Касание правого края — дальше, левого — назад, середины — показать или спрятать меню.
 */
@Composable
fun PagedReader(
    text: AnnotatedString,
    style: TextStyle,
    frontPages: List<@Composable () -> Unit>,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier,
    /** Закладка: место в тексте, с которого открыть книгу; null — открыть на обложке. */
    initialOffset: Int? = null,
    /** Сообщает место в тексте открытой страницы (null — открыта обложка или аннотация). */
    onPositionChanged: (Int?) -> Unit = {},
    controls: @Composable RowScope.() -> Unit = {},
) {
    val toggleControls by rememberUpdatedState(onToggleControls)
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val widthPx = with(density) { (maxWidth - PAGE_PADDING_H * 2).roundToPx() }
        val heightPx = with(density) { (maxHeight - PAGE_PADDING_V * 2).roundToPx() }
        val measurer = rememberTextMeasurer(cacheSize = 0)
        val pages = remember(text, style, widthPx, heightPx) {
            if (widthPx <= 0 || heightPx <= 0) {
                listOf(Page(0, text))
            } else {
                paginate(measurer.measure(text, style, constraints = Constraints(maxWidth = widthPx)), text, heightPx)
            }
        }
        val front = frontPages.size
        // Текущее место в тексте переживает пересчёт страниц (поворот экрана, смена режима текста)
        var position by rememberSaveable(text.text) { mutableStateOf(initialOffset) }
        val pagerState = rememberPagerState(initialPage = pageFor(pages, position, front)) { front + pages.size }
        val scope = rememberCoroutineScope()

        LaunchedEffect(pages) {
            val target = pageFor(pages, position, front)
            if (target != pagerState.currentPage) pagerState.scrollToPage(target)
            snapshotFlow { pagerState.settledPage }.collect { page ->
                position = if (page < front) null else pages.getOrNull(page - front)?.start
                onPositionChanged(position)
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(pagerState) {
                        detectTapGestures { offset ->
                            when {
                                offset.x > size.width * 2 / 3 -> scope.turn(pagerState, +1)
                                offset.x < size.width / 3 -> scope.turn(pagerState, -1)
                                else -> toggleControls()
                            }
                        }
                    },
            ) {
                if (page < front) {
                    frontPages[page]()
                } else {
                    Text(
                        pages[page - front].text,
                        style = style,
                        modifier = Modifier.fillMaxSize().padding(horizontal = PAGE_PADDING_H, vertical = PAGE_PADDING_V),
                    )
                }
            }
        }

        // Нижняя часть меню: страницы и режим текста. Поверх страницы — чтобы текст не перескакивал
        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Surface(tonalElevation = 3.dp, shadowElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().height(56.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { scope.turn(pagerState, -1) }, enabled = pagerState.currentPage > 0) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.page_previous))
                    }
                    Box(Modifier.widthIn(min = 64.dp), contentAlignment = Alignment.Center) {
                        if (pagerState.currentPage >= front) {
                            Text(
                                stringResource(R.string.page_number, pagerState.currentPage - front + 1, pages.size),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    IconButton(
                        onClick = { scope.turn(pagerState, +1) },
                        enabled = pagerState.currentPage < pagerState.pageCount - 1,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.page_next))
                    }
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                        controls()
                    }
                }
            }
        }
    }
}

private fun CoroutineScope.turn(state: PagerState, delta: Int) {
    val target = (state.currentPage + delta).coerceIn(0, state.pageCount - 1)
    if (target != state.currentPage) launch { state.animateScrollToPage(target) }
}

/** Страница текста и номер её первого символа в тексте. */
private class Page(val start: Int, val text: AnnotatedString)

/** Страница пейджера, на которой находится место [offset]; null — обложка. */
private fun pageFor(pages: List<Page>, offset: Int?, front: Int): Int {
    if (offset == null) return 0
    val index = pages.indexOfLast { it.start <= offset }
    return front + index.coerceAtLeast(0)
}

/** Режет свёрстанный текст на страницы по строкам: на страницу — столько строк, сколько влезает. */
private fun paginate(layout: TextLayoutResult, text: AnnotatedString, pageHeight: Int): List<Page> {
    val pages = mutableListOf<Page>()
    var line = 0
    while (line < layout.lineCount) {
        // Пустые строки между абзацами в начале страницы не нужны
        while (line < layout.lineCount && text.substring(layout.getLineStart(line), layout.getLineEnd(line)).isBlank()) line++
        if (line >= layout.lineCount) break
        val top = layout.getLineTop(line)
        var last = line
        while (last + 1 < layout.lineCount && layout.getLineBottom(last + 1) - top <= pageHeight) last++
        val start = layout.getLineStart(line)
        var end = layout.getLineEnd(last)
        while (end > start && text[end - 1].isWhitespace()) end--
        pages += Page(start, text.subSequence(start, end))
        line = last + 1
    }
    return pages.ifEmpty { listOf(Page(0, AnnotatedString(""))) }
}

private val PAGE_PADDING_H = 24.dp
private val PAGE_PADDING_V = 28.dp
