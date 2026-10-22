package ru.sakhabooks.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Заголовки — с засечками (книжный характер), основной текст — без засечек.
// Системные шрифты поддерживают якутские буквы ҥ, ҕ, ө, һ, ү.
private val Headings = FontFamily.Serif

val AppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = Headings, fontWeight = FontWeight.SemiBold),
        headlineLarge = headlineLarge.copy(fontFamily = Headings, fontWeight = FontWeight.SemiBold),
        headlineMedium = headlineMedium.copy(fontFamily = Headings, fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontFamily = Headings, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontFamily = Headings, fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 26.sp),
    )
}
