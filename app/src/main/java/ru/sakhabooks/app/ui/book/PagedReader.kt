package ru.sakhabooks.app.ui.book

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
 * Книга, которую листают: первая страница — обложка, дальше текст, нарезанный на страницы
 * точно по размеру экрана. Листать — пальцем или касанием левого/правого края страницы.
 */
@Composable
fun PagedReader(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    coverPage: @Composable () -> Unit,
    controls: @Composable RowScope.() -> Unit = {},
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val widthPx = with(density) { (maxWidth - PAGE_PADDING_H * 2).roundToPx() }
        val heightPx = with(density) { (maxHeight - BAR_HEIGHT - PAGE_PADDING_V * 2).roundToPx() }
        val measurer = rememberTextMeasurer(cacheSize = 0)
        val pages = remember(text, style, widthPx, heightPx) {
            if (widthPx <= 0 || heightPx <= 0) {
                listOf(text)
            } else {
                paginate(measurer.measure(text, style, constraints = Constraints(maxWidth = widthPx)), text, heightPx)
            }
        }
        val pagerState = rememberPagerState(pageCount = { pages.size + 1 })
        val scope = rememberCoroutineScope()

        Column(Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                beyondViewportPageCount = 1,
            ) { page ->
                if (page == 0) {
                    coverPage()
                } else {
                    Text(
                        pages[page - 1],
                        style = style,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(pagerState) {
                                // Касание правой трети — дальше, левой — назад
                                detectTapGestures { offset ->
                                    when {
                                        offset.x > size.width * 2 / 3 -> scope.turn(pagerState, +1)
                                        offset.x < size.width / 3 -> scope.turn(pagerState, -1)
                                    }
                                }
                            }
                            .padding(horizontal = PAGE_PADDING_H, vertical = PAGE_PADDING_V),
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().height(BAR_HEIGHT).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { scope.turn(pagerState, -1) }, enabled = pagerState.currentPage > 0) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.page_previous))
                }
                Box(Modifier.widthIn(min = 64.dp), contentAlignment = Alignment.Center) {
                    if (pagerState.currentPage > 0) {
                        Text(
                            stringResource(R.string.page_number, pagerState.currentPage, pages.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                IconButton(
                    onClick = { scope.turn(pagerState, +1) },
                    enabled = pagerState.currentPage < pages.size,
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

private fun CoroutineScope.turn(state: PagerState, delta: Int) {
    val target = (state.currentPage + delta).coerceIn(0, state.pageCount - 1)
    if (target != state.currentPage) launch { state.animateScrollToPage(target) }
}

/** Режет свёрстанный текст на страницы по строкам: на страницу — столько строк, сколько влезает. */
private fun paginate(layout: TextLayoutResult, text: AnnotatedString, pageHeight: Int): List<AnnotatedString> {
    val pages = mutableListOf<AnnotatedString>()
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
        pages += text.subSequence(start, end)
        line = last + 1
    }
    return pages.ifEmpty { listOf(AnnotatedString("")) }
}

private val PAGE_PADDING_H = 20.dp
private val PAGE_PADDING_V = 12.dp
private val BAR_HEIGHT = 48.dp
