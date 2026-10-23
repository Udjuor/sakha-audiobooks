package ru.sakhabooks.app.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Запоминает по каждой главе: место остановки, когда слушали в последний раз,
 * длительность и дослушана ли она; а также выбранную скорость.
 */
@Singleton
class ProgressStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("listening_progress", Context.MODE_PRIVATE)

    private val _changes = MutableStateFlow(0)

    /** Меняется при каждом сохранении — чтобы полка «Мин кинигэлэрим» обновлялась сама. */
    val changes: StateFlow<Int> = _changes.asStateFlow()

    fun position(chapterId: String): Long = prefs.getLong(chapterId, 0L)

    /** Когда главу слушали в последний раз (0 — ни разу). */
    fun lastPlayedAt(chapterId: String): Long = prefs.getLong(chapterId + SUFFIX_TIME, 0L)

    fun duration(chapterId: String): Long = prefs.getLong(chapterId + SUFFIX_DURATION, 0L)

    fun isFinished(chapterId: String): Boolean = prefs.getBoolean(chapterId + SUFFIX_FINISHED, false)

    fun save(chapterId: String, positionMs: Long, durationMs: Long = 0L) {
        prefs.edit {
            putLong(chapterId, positionMs)
            putLong(chapterId + SUFFIX_TIME, System.currentTimeMillis())
            if (durationMs > 0) putLong(chapterId + SUFFIX_DURATION, durationMs)
            putBoolean(chapterId + SUFFIX_FINISHED, false)
        }
        _changes.update { it + 1 }
    }

    /** Дослушали до конца: в следующий раз начнём сначала, на полке — «Прослушано». */
    fun markFinished(chapterId: String) {
        prefs.edit {
            putLong(chapterId, 0L)
            putLong(chapterId + SUFFIX_TIME, System.currentTimeMillis())
            putBoolean(chapterId + SUFFIX_FINISHED, true)
        }
        _changes.update { it + 1 }
    }

    fun speed(): Float = prefs.getFloat(KEY_SPEED, 1f)

    fun saveSpeed(speed: Float) {
        prefs.edit { putFloat(KEY_SPEED, speed) }
    }

    private companion object {
        const val KEY_SPEED = "playback_speed"
        const val SUFFIX_TIME = ":last_played"
        const val SUFFIX_DURATION = ":duration"
        const val SUFFIX_FINISHED = ":finished"
    }
}
