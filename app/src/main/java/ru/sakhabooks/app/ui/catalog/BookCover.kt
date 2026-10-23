package ru.sakhabooks.app.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.sakhabooks.app.data.Book
import ru.sakhabooks.app.ui.components.AssetImage
import ru.sakhabooks.app.ui.components.OrnamentBand

/** Обложка книги. Если иллюстрации нет — временная: первая буква и полоса орнамента. */
@Composable
fun BookCover(book: Book, coverPath: String?, modifier: Modifier = Modifier, sizeDp: Int = 88) {
    val shape = Modifier
        .size(width = sizeDp.dp, height = (sizeDp * 1.3f).dp)
        .clip(RoundedCornerShape(8.dp))
    if (coverPath == null) {
        LetterCover(book, sizeDp, modifier.then(shape))
    } else {
        AssetImage(
            path = coverPath,
            contentDescription = book.titleSah,
            modifier = modifier.then(shape),
            // Иллюстрации горизонтальные — в узкой карточке сдвигаем кадр чуть вправо, к героям
            alignment = BiasAlignment(0.3f, 0f),
            placeholder = { LetterCover(book, sizeDp, modifier.then(shape)) },
        )
    }
}

@Composable
private fun LetterCover(book: Book, sizeDp: Int, modifier: Modifier) {
    Box(
        modifier.background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            book.titleSah.take(1),
            style = if (sizeDp >= 72) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        OrnamentBand(
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 4.dp, vertical = (sizeDp / 11).dp),
            height = (sizeDp / 11).coerceAtLeast(4).dp,
        )
    }
}
