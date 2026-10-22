package ru.sakhabooks.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Полоса орнамента: ряд ромбов между двумя линиями.
 * Это место под настоящий якутский орнамент — когда художник пришлёт SVG,
 * заменим отрисовку здесь, и он появится на всех экранах сразу.
 */
@Composable
fun OrnamentBand(
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    color: Color = MaterialTheme.colorScheme.secondary,
) {
    Canvas(modifier.fillMaxWidth().height(height)) {
        val h = size.height
        val stroke = h / 8f
        drawLine(color, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
        drawLine(color, Offset(0f, h - stroke / 2), Offset(size.width, h - stroke / 2), stroke)

        val step = h * 1.6f
        val half = h * 0.32f
        val cy = h / 2
        var x = step / 2
        while (x < size.width) {
            val diamond = Path().apply {
                moveTo(x, cy - half)
                lineTo(x + half, cy)
                lineTo(x, cy + half)
                lineTo(x - half, cy)
                close()
            }
            drawPath(diamond, color)
            x += step
        }
    }
}

@Preview
@Composable
private fun OrnamentBandPreview() = OrnamentBand()
