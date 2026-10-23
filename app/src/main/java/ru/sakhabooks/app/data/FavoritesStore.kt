package ru.sakhabooks.app.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Книги, отмеченные сердечком. Пока хранятся на телефоне; с сервером — будут в аккаунте. */
@Singleton
class FavoritesStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    private val _ids = MutableStateFlow(prefs.getStringSet(KEY_IDS, null)?.toSet().orEmpty())
    val ids: StateFlow<Set<String>> = _ids.asStateFlow()

    fun toggle(bookId: String) {
        val updated = if (bookId in _ids.value) _ids.value - bookId else _ids.value + bookId
        prefs.edit { putStringSet(KEY_IDS, updated) }
        _ids.value = updated
    }

    private companion object {
        const val KEY_IDS = "book_ids"
    }
}
