package ru.sakhabooks.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Картинка из папки assets (встроенные обложки демо-каталога).
 * Когда обложки будут приходить с сервера, здесь появится загрузка по ссылке.
 */
@Composable
fun AssetImage(
    path: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    placeholder: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = AssetImageCache[path], path) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    context.assets.open(path).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
                }.getOrNull()
            }?.also { AssetImageCache[path] = it }
        }
    }
    val image = bitmap
    if (image == null) {
        placeholder()
    } else {
        Image(image, contentDescription, modifier, alignment = alignment, contentScale = contentScale)
    }
}

/** Уже открытые картинки, чтобы не читать файл заново при каждом показе. */
private object AssetImageCache {
    private val map = HashMap<String, ImageBitmap>()
    operator fun get(path: String): ImageBitmap? = synchronized(map) { map[path] }
    operator fun set(path: String, image: ImageBitmap) = synchronized(map) { map[path] = image }
}
