package ru.sakhabooks.app.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Закладка чтения: для каждой главы — место в тексте (номер первого символа открытой страницы)
 * и режим текста. Храним место, а не номер страницы: страницы пересчитываются при повороте
 * телефона, а место в тексте остаётся тем же.
 */
@Singleton
class ReadingStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("reading_bookmarks", Context.MODE_PRIVATE)

    /** Место в тексте; null — книгу ещё не открывали дальше обложки. */
    fun offset(chapterId: String): Int? =
        prefs.getInt(chapterId + SUFFIX_OFFSET, NONE).takeIf { it != NONE }

    fun saveOffset(chapterId: String, offset: Int?) {
        prefs.edit { putInt(chapterId + SUFFIX_OFFSET, offset ?: NONE) }
    }

    fun textMode(chapterId: String): String? = prefs.getString(chapterId + SUFFIX_MODE, null)

    fun saveTextMode(chapterId: String, mode: String) {
        prefs.edit { putString(chapterId + SUFFIX_MODE, mode) }
    }

    private companion object {
        const val NONE = -1
        const val SUFFIX_OFFSET = ":offset"
        const val SUFFIX_MODE = ":text_mode"
    }
}
